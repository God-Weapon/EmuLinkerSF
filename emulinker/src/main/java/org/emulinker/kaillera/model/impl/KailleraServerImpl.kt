package org.emulinker.kaillera.model.impl

import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.flogger.FluentLogger
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.access.AccessManager.Companion.ACCESS_NAMES
import org.emulinker.kaillera.lookingforgame.LookingForGameEvent
import org.emulinker.kaillera.lookingforgame.TwitterBroadcaster
import org.emulinker.kaillera.master.StatsCollector
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.KailleraUser
import org.emulinker.kaillera.model.KailleraUser.Companion.CONNECTION_TYPE_NAMES
import org.emulinker.kaillera.model.KailleraUser.Companion.STATUS_NAMES
import org.emulinker.kaillera.model.event.*
import org.emulinker.kaillera.model.exception.*
import org.emulinker.kaillera.release.ReleaseInfo
import org.emulinker.util.EmuLang.getString
import org.emulinker.util.EmuLang.hasString
import org.emulinker.util.EmuUtil.formatSocketAddress
import org.emulinker.util.Executable

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class KailleraServerImpl
    @Inject
    internal constructor(
        private val threadPool: ThreadPoolExecutor,
        override val accessManager: AccessManager,
        private val flags: RuntimeFlags,
        statsCollector: StatsCollector?,
        override val releaseInfo: ReleaseInfo,
        private val autoFireDetectorFactory: AutoFireDetectorFactory,
        private val lookingForGameReporter: TwitterBroadcaster,
        metrics: MetricRegistry
    ) : KailleraServer, Executable {

  protected var allowedConnectionTypes = BooleanArray(7)
  protected val loginMessages: ImmutableList<String>
  protected var stopFlag = false
  override var running = false
    protected set
  protected var connectionCounter = 1
  protected var gameCounter = 1

  var statsCollector: StatsCollector? = null

  private val usersMap: MutableMap<Int, KailleraUserImpl> = ConcurrentHashMap(flags.maxUsers)
  override val users = usersMap.values

  var gamesMap: MutableMap<Int, KailleraGameImpl> = ConcurrentHashMap(flags.maxGames)
  override val games = gamesMap.values

  override var trivia: Trivia? = null

  var triviaThread: Thread? = null

  override var switchTrivia = false

  override fun getUser(userID: Int): KailleraUser? {
    return usersMap[userID]
  }

  override fun getGame(gameID: Int): KailleraGame? {
    return gamesMap[gameID]
  }

  fun getNumGamesPlaying(): Int {
    var count = 0
    for (game in games) {
      if (game.status != KailleraGame.STATUS_WAITING.toInt()) count++
    }
    return count
  }

  override val maxPing = flags.maxPing
  override val maxUsers = flags.maxUsers
  override val maxGames = flags.maxGames

  val chatFloodTime = flags.chatFloodTime
  val createGameFloodTime = flags.createGameFloodTime
  val allowSinglePlayer = flags.allowSinglePlayer
  val maxUserNameLength = flags.maxUserNameLength
  val maxChatLength = flags.maxChatLength
  val maxGameChatLength = flags.maxGameChatLength
  val maxGameNameLength = flags.maxGameNameLength
  val quitMessageLength = flags.maxQuitMessageLength
  val maxClientNameLength = flags.maxClientNameLength
  val allowMultipleConnections = flags.allowMultipleConnections

  override fun toString(): String {
    return String.format(
        "KailleraServerImpl[numUsers=%d numGames=%d isRunning=%b]", users.size, games.size, running)
  }

  @Synchronized
  override fun start() {
    logger.atFine().log("KailleraServer thread received start request!")
    logger
        .atFine()
        .log(
            "KailleraServer thread starting (ThreadPool:%d/%d)",
            threadPool.activeCount,
            threadPool.poolSize)
    stopFlag = false
    threadPool.execute(this)
    Thread.yield()
  }

  @Synchronized
  override fun stop() {
    logger.atFine().log("KailleraServer thread received stop request!")
    if (!running) {
      logger.atFine().log("KailleraServer thread stop request ignored: not running!")
      return
    }
    stopFlag = true
    for (user in usersMap.values) user.stop()
    usersMap.clear()
    gamesMap.clear()
  }

  // not synchronized because I know the caller will be thread safe
  protected fun getNextUserID(): Int {
    if (connectionCounter > 0xFFFF) connectionCounter = 1
    return connectionCounter++
  }

  // not synchronized because I know the caller will be thread safe
  protected fun getNextGameID(): Int {
    if (gameCounter > 0xFFFF) gameCounter = 1
    return gameCounter++
  }

  fun getAutoFireDetector(game: KailleraGame?): AutoFireDetector {
    return autoFireDetectorFactory.getInstance(game!!, flags.gameAutoFireSensitivity)
  }

  @Synchronized
  @Throws(ServerFullException::class, NewConnectionException::class)
  override fun newConnection(
      clientSocketAddress: InetSocketAddress?, protocol: String?, listener: KailleraEventListener?
  ): KailleraUser {
    // we'll assume at this point that ConnectController has already asked AccessManager if this IP
    // is banned, so no need to do it again here
    logger
        .atFine()
        .log("Processing connection request from " + formatSocketAddress(clientSocketAddress!!))
    val access = accessManager.getAccess(clientSocketAddress.address)

    // admins will be allowed in even if the server is full
    if (flags.maxUsers > 0 && usersMap.size >= maxUsers && access <= AccessManager.ACCESS_NORMAL) {
      logger
          .atWarning()
          .log(
              "Connection from " +
                  formatSocketAddress(clientSocketAddress) +
                  " denied: Server is full!")
      throw ServerFullException(getString("KailleraServerImpl.LoginDeniedServerFull"))
    }
    val userID = getNextUserID()
    val user = KailleraUserImpl(userID, protocol!!, clientSocketAddress, listener!!, this)
    user.status = KailleraUser.STATUS_CONNECTING.toInt()
    logger
        .atInfo()
        .log(
            user.toString() +
                " attempting new connection using protocol " +
                protocol +
                " from " +
                formatSocketAddress(clientSocketAddress))
    logger
        .atFine()
        .log(
            user.toString() +
                " Thread starting (ThreadPool:" +
                threadPool.activeCount +
                "/" +
                threadPool.poolSize +
                ")")
    threadPool.execute(user)
    Thread.yield()
    logger
        .atFine()
        .log(
            user.toString() +
                " Thread started (ThreadPool:" +
                threadPool.activeCount +
                "/" +
                threadPool.poolSize +
                ")")
    usersMap[userID] = user
    return user
  }

  @Synchronized
  @Throws(
      PingTimeException::class,
      ClientAddressException::class,
      ConnectionTypeException::class,
      UserNameException::class,
      LoginException::class)
  override fun login(user: KailleraUser?) {
    val userImpl = user as KailleraUserImpl?
    val loginDelay = System.currentTimeMillis() - user!!.connectTime
    logger
        .atInfo()
        .log(
            user.toString() +
                ": login request: delay=" +
                loginDelay +
                "ms, clientAddress=" +
                formatSocketAddress(user.socketAddress!!) +
                ", name=" +
                user.name +
                ", ping=" +
                user.ping +
                ", client=" +
                user.clientType +
                ", connection=" +
                CONNECTION_TYPE_NAMES[user.connectionType.toInt()])
    if (user.loggedIn) {
      logger.atWarning().log("$user login denied: Already logged in!")
      throw LoginException(getString("KailleraServerImpl.LoginDeniedAlreadyLoggedIn"))
    }
    val userListKey = user.id
    val u: KailleraUser? = usersMap[userListKey]
    if (u == null) {
      logger.atWarning().log("$user login denied: Connection timed out!")
      throw LoginException(getString("KailleraServerImpl.LoginDeniedConnectionTimedOut"))
    }
    val access = accessManager.getAccess(user.socketAddress!!.address)
    if (access < AccessManager.ACCESS_NORMAL) {
      logger.atInfo().log("$user login denied: Access denied")
      usersMap.remove(userListKey)
      throw LoginException(getString("KailleraServerImpl.LoginDeniedAccessDenied"))
    }
    if (access == AccessManager.ACCESS_NORMAL && maxPing > 0 && user.ping > maxPing) {
      logger.atInfo().log(user.toString() + " login denied: Ping " + user.ping + " > " + maxPing)
      usersMap.remove(userListKey)
      throw PingTimeException(
          getString(
              "KailleraServerImpl.LoginDeniedPingTooHigh", user.ping.toString() + " > " + maxPing))
    }
    if (access == AccessManager.ACCESS_NORMAL &&
        !allowedConnectionTypes[user.connectionType.toInt()]) {
      logger
          .atInfo()
          .log(
              user.toString() +
                  " login denied: Connection " +
                  CONNECTION_TYPE_NAMES[user.connectionType.toInt()] +
                  " Not Allowed")
      usersMap.remove(userListKey)
      throw LoginException(
          getString(
              "KailleraServerImpl.LoginDeniedConnectionTypeDenied",
              CONNECTION_TYPE_NAMES[user.connectionType.toInt()]))
    }
    if (user.ping < 0) {
      logger.atWarning().log(user.toString() + " login denied: Invalid ping: " + user.ping)
      usersMap.remove(userListKey)
      throw PingTimeException(getString("KailleraServerImpl.LoginErrorInvalidPing", user.ping))
    }
    if (access == AccessManager.ACCESS_NORMAL && Strings.isNullOrEmpty(user.name) ||
        user.name!!.isBlank()) {
      logger.atInfo().log("$user login denied: Empty UserName")
      usersMap.remove(userListKey)
      throw UserNameException(getString("KailleraServerImpl.LoginDeniedUserNameEmpty"))
    }

    // new SF MOD - Username filter
    val nameLower = user.name!!.lowercase(Locale.getDefault())
    if (user.name == "Server" ||
        nameLower.contains("|") ||
        (access == AccessManager.ACCESS_NORMAL &&
            (nameLower.contains("www.") ||
                nameLower.contains("http://") ||
                nameLower.contains("https://") ||
                nameLower.contains("\\") ||
                nameLower.contains("�") ||
                nameLower.contains("�")))) {
      logger.atInfo().log("$user login denied: Illegal characters in UserName")
      usersMap.remove(userListKey)
      throw UserNameException(
          getString("KailleraServerImpl.LoginDeniedIllegalCharactersInUserName"))
    }

    // access == AccessManager.ACCESS_NORMAL &&
    if (flags.maxUserNameLength > 0 && user.name!!.length > maxUserNameLength) {
      logger.atInfo().log(user.toString() + " login denied: UserName Length > " + maxUserNameLength)
      usersMap.remove(userListKey)
      throw UserNameException(getString("KailleraServerImpl.LoginDeniedUserNameTooLong"))
    }
    if (access == AccessManager.ACCESS_NORMAL &&
        flags.maxClientNameLength > 0 &&
        user.clientType!!.length > maxClientNameLength) {
      logger.atInfo().log("$user login denied: Client Name Length > $maxClientNameLength")
      usersMap.remove(userListKey)
      throw UserNameException(getString("KailleraServerImpl.LoginDeniedEmulatorNameTooLong"))
    }
    if (user.clientType!!.lowercase(Locale.getDefault()).contains("|")) {
      logger.atWarning().log("$user login denied: Illegal characters in EmulatorName")
      usersMap.remove(userListKey)
      throw UserNameException("Illegal characters in Emulator Name")
    }
    if (access == AccessManager.ACCESS_NORMAL) {
      val chars = user.name!!.toCharArray()
      for (i in chars.indices) {
        if (chars[i].code < 32) {
          logger.atInfo().log("$user login denied: Illegal characters in UserName")
          usersMap.remove(userListKey)
          throw UserNameException(
              getString("KailleraServerImpl.LoginDeniedIllegalCharactersInUserName"))
        }
      }
    }
    if (u.status != KailleraUser.STATUS_CONNECTING.toInt()) {
      usersMap.remove(userListKey)
      logger
          .atWarning()
          .log(user.toString() + " login denied: Invalid status=" + STATUS_NAMES[u.status])
      throw LoginException(getString("KailleraServerImpl.LoginErrorInvalidStatus", u.status))
    }
    if (u.connectSocketAddress.address != user.socketAddress!!.address) {
      usersMap.remove(userListKey)
      logger
          .atWarning()
          .log(
              user.toString() +
                  " login denied: Connect address does not match login address: " +
                  u.connectSocketAddress.address.hostAddress +
                  " != " +
                  user.socketAddress!!.address.hostAddress)
      throw ClientAddressException(getString("KailleraServerImpl.LoginDeniedAddressMatchError"))
    }
    if (access == AccessManager.ACCESS_NORMAL &&
        !accessManager.isEmulatorAllowed(user.clientType)) {
      logger
          .atInfo()
          .log(user.toString() + " login denied: AccessManager denied emulator: " + user.clientType)
      usersMap.remove(userListKey)
      throw LoginException(
          getString("KailleraServerImpl.LoginDeniedEmulatorRestricted", user.clientType))
    }
    for (u2 in users) {
      if (u2.loggedIn) {
        if (!u2.equals(u) &&
            (u.connectSocketAddress.address == u2.connectSocketAddress.address) &&
            u.name == u2.name) {
          // user is attempting to login more than once with the same name and address
          // logoff the old user and login the new one
          try {
            quit(u2, getString("KailleraServerImpl.ForcedQuitReconnected"))
          } catch (e: Exception) {
            logger.atSevere().withCause(e).log("Error forcing $u2 quit for reconnect!")
          }
        } else if (!u2.equals(u) &&
            u2.name!!.lowercase(Locale.getDefault()).trim { it <= ' ' } ==
                u.name!!.lowercase(Locale.getDefault()).trim { it <= ' ' }) {
          usersMap.remove(userListKey)
          logger
              .atWarning()
              .log(user.toString() + " login denied: Duplicating Names is not allowed! " + u2.name)
          throw ClientAddressException("Duplicating names is not allowed: " + u2.name)
        }
        if (access == AccessManager.ACCESS_NORMAL &&
            !u2.equals(u) &&
            (u.connectSocketAddress.address == u2.connectSocketAddress.address) &&
            u.name != u2.name &&
            !flags.allowMultipleConnections) {
          usersMap.remove(userListKey)
          logger
              .atWarning()
              .log(user.toString() + " login denied: Address already logged in as " + u2.name)
          throw ClientAddressException(
              getString("KailleraServerImpl.LoginDeniedAlreadyLoggedInAs", u2.name))
        }
      }
    }

    // passed all checks
    userImpl!!.access = access
    userImpl.status = KailleraUser.STATUS_IDLE.toInt()
    userImpl.loggedIn = true
    usersMap[userListKey] = userImpl
    userImpl.addEvent(ConnectedEvent(this, user))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    for (loginMessage in loginMessages) {
      userImpl.addEvent(InfoMessageEvent(user, loginMessage!!))
      try {
        Thread.sleep(20)
      } catch (e: Exception) {}
    }
    if (access > AccessManager.ACCESS_NORMAL)
        logger
            .atInfo()
            .log(
                user.toString() +
                    " logged in successfully with " +
                    ACCESS_NAMES[access] +
                    " access!")
    else logger.atInfo().log("$user logged in successfully")

    // this is fairly ugly
    if (user.isEmuLinkerClient) {
      userImpl.addEvent(InfoMessageEvent(user, ":ACCESS=" + userImpl.accessStr))
      if (access >= AccessManager.ACCESS_SUPERADMIN) {
        var sb = StringBuilder()
        sb.append(":USERINFO=")
        var sbCount = 0
        for (u3 in users) {
          if (!u3.loggedIn) continue
          sb.append(u3.id)
          sb.append(0x02.toChar())
          sb.append(u3.connectSocketAddress.address.hostAddress)
          sb.append(0x02.toChar())
          sb.append(u3.accessStr)
          sb.append(0x02.toChar())
          // str = u3.getName().replace(',','.');
          // str = str.replace(';','.');
          sb.append(u3.name)
          sb.append(0x02.toChar())
          sb.append(u3.ping)
          sb.append(0x02.toChar())
          sb.append(u3.status)
          sb.append(0x02.toChar())
          sb.append(u3.connectionType.toInt())
          sb.append(0x03.toChar())
          sbCount++
          if (sb.length > 300) {
            (user as KailleraUserImpl?)!!.addEvent(InfoMessageEvent(user, sb.toString()))
            sb = StringBuilder()
            sb.append(":USERINFO=")
            sbCount = 0
            try {
              Thread.sleep(100)
            } catch (e: Exception) {}
          }
        }
        if (sbCount > 0)
            (user as KailleraUserImpl?)!!.addEvent(InfoMessageEvent(user, sb.toString()))
        try {
          Thread.sleep(100)
        } catch (e: Exception) {}
      }
    }
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    if (access >= AccessManager.ACCESS_ADMIN)
        userImpl.addEvent(
            InfoMessageEvent(user, getString("KailleraServerImpl.AdminWelcomeMessage")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    // TODO(nue): Localize this welcome message?
    // userImpl.addEvent(
    //     new InfoMessageEvent(
    //         user,
    //         getReleaseInfo().getProductName()
    //             + " v"
    //             + getReleaseInfo().getVersionString()
    //             + ": "
    //             + getReleaseInfo().getReleaseDate()
    //             + " - Visit: www.EmuLinker.org"));
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    addEvent(UserJoinedEvent(this, user))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    val announcement = accessManager.getAnnouncement(user.socketAddress!!.address)
    if (announcement != null) announce(announcement, false, null)
  }

  @Synchronized
  @Throws(
      QuitException::class,
      DropGameException::class,
      QuitGameException::class,
      CloseGameException::class)
  override fun quit(user: KailleraUser?, message: String?) {
    lookingForGameReporter.cancelActionsForUser(user!!.id)
    if (!user.loggedIn) {
      usersMap.remove(user.id)
      logger.atSevere().log("$user quit failed: Not logged in")
      throw QuitException(getString("KailleraServerImpl.NotLoggedIn"))
    }
    if (usersMap.remove(user.id) == null)
        logger.atSevere().log("$user quit failed: not in user list")
    val userGame = (user as KailleraUserImpl?)!!.game
    if (userGame != null) user.quitGame()
    var quitMsg = message!!.trim { it <= ' ' }
    if (Strings.isNullOrEmpty(quitMsg) ||
        (flags.maxQuitMessageLength > 0 && quitMsg.length > flags.maxQuitMessageLength))
        quitMsg = getString("KailleraServerImpl.StandardQuitMessage")
    val access = user.server.accessManager.getAccess(user.socketAddress!!.address)
    if (access < AccessManager.ACCESS_SUPERADMIN &&
        user.server.accessManager.isSilenced(user.socketAddress!!.address)) {
      quitMsg = "www.EmuLinker.org"
    }
    logger.atInfo().log("$user quit: $quitMsg")
    val quitEvent = UserQuitEvent(this, user, quitMsg)
    addEvent(quitEvent)
    (user as KailleraUserImpl?)!!.addEvent(quitEvent)
  }

  @Synchronized
  @Throws(ChatException::class, FloodException::class)
  override fun chat(user: KailleraUser?, message: String?) {
    var message = message
    if (!user!!.loggedIn) {
      logger.atSevere().log("$user chat failed: Not logged in")
      throw ChatException(getString("KailleraServerImpl.NotLoggedIn"))
    }
    val access = accessManager.getAccess(user.socketAddress!!.address)
    if (access < AccessManager.ACCESS_SUPERADMIN &&
        accessManager.isSilenced(user.socketAddress!!.address)) {
      logger.atWarning().log("$user chat denied: Silenced: $message")
      throw ChatException(getString("KailleraServerImpl.ChatDeniedSilenced"))
    }
    if (access == AccessManager.ACCESS_NORMAL &&
        flags.chatFloodTime > 0 &&
        (System.currentTimeMillis() - (user as KailleraUserImpl?)!!.lastChatTime <
            flags.chatFloodTime * 1000)) {
      logger.atWarning().log("$user chat denied: Flood: $message")
      throw FloodException(getString("KailleraServerImpl.ChatDeniedFloodControl"))
    }
    if (message == ":USER_COMMAND") {
      return
    }
    message = message!!.trim { it <= ' ' }
    if (Strings.isNullOrEmpty(message) || message.startsWith("�") || message.startsWith("�")) return
    if (access == AccessManager.ACCESS_NORMAL) {
      val chars = message.toCharArray()
      for (i in chars.indices) {
        if (chars[i].code < 32) {
          logger.atWarning().log("$user chat denied: Illegal characters in message")
          throw ChatException(getString("KailleraServerImpl.ChatDeniedIllegalCharacters"))
        }
      }
      if (flags.maxChatLength > 0 && message.length > flags.maxChatLength) {
        logger
            .atWarning()
            .log(user.toString() + " chat denied: Message Length > " + flags.maxChatLength)
        throw ChatException(getString("KailleraServerImpl.ChatDeniedMessageTooLong"))
      }
    }
    logger.atInfo().log("$user chat: $message")
    addEvent(ChatEvent(this, user, message))
    if (switchTrivia) {
      if (!trivia!!.isAnswered && trivia!!.isCorrect(message)) {
        trivia!!.addScore(user.name!!, user.socketAddress!!.address.hostAddress, message)
      }
    }
  }

  @Synchronized
  @Throws(CreateGameException::class, FloodException::class)
  override fun createGame(user: KailleraUser?, romName: String?): KailleraGame? {
    if (!user!!.loggedIn) {
      logger.atSevere().log("$user create game failed: Not logged in")
      throw CreateGameException(getString("KailleraServerImpl.NotLoggedIn"))
    }
    if ((user as KailleraUserImpl?)!!.game != null) {
      logger
          .atSevere()
          .log(
              user.toString() +
                  " create game failed: already in game: " +
                  (user as KailleraUserImpl?)!!.game)
      throw CreateGameException(getString("KailleraServerImpl.CreateGameErrorAlreadyInGame"))
    }
    if (flags.maxGameNameLength > 0 &&
        romName!!.trim { it <= ' ' }.length > flags.maxGameNameLength) {
      logger
          .atWarning()
          .log(
              user.toString() + " create game denied: Rom Name Length > " + flags.maxGameNameLength)
      throw CreateGameException(getString("KailleraServerImpl.CreateGameDeniedNameTooLong"))
    }
    if (romName!!.lowercase(Locale.getDefault()).contains("|")) {
      logger.atWarning().log("$user create game denied: Illegal characters in ROM name")
      throw CreateGameException(getString("KailleraServerImpl.CreateGameDeniedIllegalCharacters"))
    }
    val access = accessManager.getAccess(user.socketAddress!!.address)
    if (access == AccessManager.ACCESS_NORMAL) {
      if (flags.createGameFloodTime > 0 &&
          System.currentTimeMillis() - (user as KailleraUserImpl?)!!.lastCreateGameTime <
              flags.createGameFloodTime * 1000) {
        logger.atWarning().log("$user create game denied: Flood: $romName")
        throw FloodException(getString("KailleraServerImpl.CreateGameDeniedFloodControl"))
      }
      if (flags.maxGames > 0 && games.size >= flags.maxGames) {
        logger
            .atWarning()
            .log(
                user.toString() +
                    " create game denied: Over maximum of " +
                    flags.maxGames +
                    " current games!")
        throw CreateGameException(
            getString("KailleraServerImpl.CreateGameDeniedMaxGames", flags.maxGames))
      }
      val chars = romName.toCharArray()
      for (i in chars.indices) {
        if (chars[i].code < 32) {
          logger.atWarning().log("$user create game denied: Illegal characters in ROM name")
          throw CreateGameException(
              getString("KailleraServerImpl.CreateGameDeniedIllegalCharacters"))
        }
      }
      if (romName.trim { it <= ' ' }.isEmpty()) {
        logger.atWarning().log("$user create game denied: Rom Name Empty")
        throw CreateGameException(getString("KailleraServerImpl.CreateGameErrorEmptyName"))
      }
      if (!accessManager.isGameAllowed(romName)) {
        logger.atWarning().log("$user create game denied: AccessManager denied game: $romName")
        throw CreateGameException(getString("KailleraServerImpl.CreateGameDeniedGameBanned"))
      }
    }
    var game: KailleraGameImpl? = null
    val gameID = getNextGameID()
    game =
        KailleraGameImpl(gameID, romName, (user as KailleraUserImpl?)!!, this, flags.gameBufferSize)
    gamesMap[gameID] = game
    addEvent(GameCreatedEvent(this, game))
    logger.atInfo().log(user.toString() + " created: " + game + ": " + game.romName)
    try {
      user.joinGame(game.id)
    } catch (e: Exception) {
      // this shouldn't happen
      logger
          .atSevere()
          .withCause(e)
          .log("Caught exception while making owner join game! This shouldn't happen!")
    }
    announce(
        getString("KailleraServerImpl.UserCreatedGameAnnouncement", user.name, game.romName),
        false,
        null)
    if (lookingForGameReporter.reportAndStartTimer(
        LookingForGameEvent(
            /* gameId= */ game.id, /* gameTitle= */ game.romName, /* user= */ user))) {
      user.game!!.announce(
          getString(
              "KailleraServerImpl.TweetPendingAnnouncement", flags.twitterBroadcastDelay.seconds),
          user)
    }
    return game
  }

  @Synchronized
  @Throws(CloseGameException::class)
  fun closeGame(game: KailleraGame, user: KailleraUser) {
    if (!user.loggedIn) {
      logger.atSevere().log("$user close $game failed: Not logged in")
      throw CloseGameException(getString("KailleraServerImpl.NotLoggedIn"))
    }
    if (!gamesMap.containsKey(game.id)) {
      logger.atSevere().log("$user close $game failed: not in list: $game")
      return
    }
    (game as KailleraGameImpl).close(user)
    gamesMap.remove(game.id)
    logger.atInfo().log("$user closed: $game")
    addEvent(GameClosedEvent(this, game))
  }

  override fun checkMe(user: KailleraUser, message: String): Boolean {
    // >>>>>>>>>>>>>>>>>>>>
    var message = message
    if (!user.loggedIn) {
      logger.atSevere().log("$user chat failed: Not logged in")
      return false
    }
    val access = accessManager.getAccess(user.socketAddress!!.address)
    if (access < AccessManager.ACCESS_SUPERADMIN &&
        accessManager.isSilenced(user.socketAddress!!.address)) {
      logger.atWarning().log("$user /me: Silenced: $message")
      return false
    }

    // if (access == AccessManager.ACCESS_NORMAL && flags.getChatFloodTime() > 0 &&
    // (System.currentTimeMillis()
    // - ((KailleraUserImpl) user).getLastChatTime()) < (flags.getChatFloodTime() * 1000))
    // {
    //	logger.atWarning().log(user + " /me denied: Flood: " + message);
    //	return false;
    // }
    if (message == ":USER_COMMAND") {
      return false
    }
    message = message.trim { it <= ' ' }
    if (Strings.isNullOrEmpty(message)) return false
    if (access == AccessManager.ACCESS_NORMAL) {
      val chars = message.toCharArray()
      for (i in chars.indices) {
        if (chars[i].code < 32) {
          logger.atWarning().log("$user /me: Illegal characters in message")
          return false
        }
      }
      if (flags.maxChatLength > 0 && message.length > flags.maxChatLength) {
        logger
            .atWarning()
            .log(user.toString() + " /me denied: Message Length > " + flags.maxChatLength)
        return false
      }
    }
    return true
  }

  fun announceInGame(announcement: String?, user: KailleraUserImpl) {
    user.game!!.announce(announcement!!, user)
  }

  override fun announce(announcement: String, gamesAlso: Boolean, user: KailleraUserImpl?) {
    if (user != null) {
      if (gamesAlso) { //   /msg and /me commands
        for (kailleraUser in users) {
          if (kailleraUser.loggedIn) {
            val access = accessManager.getAccess(user.connectSocketAddress.address)
            if (access < AccessManager.ACCESS_ADMIN) {
              if (!kailleraUser.searchIgnoredUsers(user.connectSocketAddress.address.hostAddress))
                  kailleraUser.addEvent(InfoMessageEvent(kailleraUser, announcement))
            } else {
              kailleraUser.addEvent(InfoMessageEvent(kailleraUser, announcement))
            }

            /*//SF MOD
            if(gamesAlso){
            	if(kailleraUser.getGame() != null){
            		kailleraUser.getGame().announce(announcement, kailleraUser);
            		Thread.yield();
            	}
            }
            */
          }
        }
      } else {
        user.addEvent(InfoMessageEvent(user, announcement))
      }
    } else {
      for (kailleraUser in users) {
        if (kailleraUser.loggedIn) {
          kailleraUser.addEvent(InfoMessageEvent(kailleraUser, announcement))

          // SF MOD
          if (gamesAlso) {
            if (kailleraUser.game != null) {
              kailleraUser.game!!.announce(announcement, kailleraUser)
              Thread.yield()
            }
          }
        }
      }
    }
  }

  fun addEvent(event: ServerEvent) {
    for (user in usersMap.values) {
      if (user.loggedIn) {
        if (user.status != KailleraUser.STATUS_IDLE.toInt()) {
          if (user.p2P) {
            if (event.toString() == "GameDataEvent") user.addEvent(event)
            else if (event.toString() == "ChatEvent") continue
            else if (event.toString() == "UserJoinedEvent") continue
            else if (event.toString() == "UserQuitEvent") continue
            else if (event.toString() == "GameStatusChangedEvent") continue
            else if (event.toString() == "GameClosedEvent") continue
            else if (event.toString() == "GameCreatedEvent") continue else user.addEvent(event)
          } else {
            user.addEvent(event)
          }
        } else {
          user.addEvent(event)
        }
      } else {
        logger.atFine().log("$user: not adding event, not logged in: $event")
      }
    }
  }

  override fun run() {
    running = true
    logger.atFine().log("KailleraServer thread running...")
    try {
      while (!stopFlag) {
        try {
          Thread.sleep((flags.maxPing * 3).toLong())
        } catch (e: InterruptedException) {
          logger.atSevere().withCause(e).log("Sleep Interrupted!")
        }

        //				logger.atFine().log(this + " running maintenance...");
        if (stopFlag) break
        if (usersMap.isEmpty()) continue
        for (user in users) {
          synchronized(user) {
            val access = accessManager.getAccess(user.connectSocketAddress.address)
            (user as KailleraUserImpl?)!!.access = access

            // LagStat
            if (user.loggedIn) {
              if (user.game != null &&
                  user.game!!.status == KailleraGame.STATUS_PLAYING.toInt() &&
                  !user.game!!.startTimeout) {
                if (System.currentTimeMillis() - user.game!!.startTimeoutTime > 15000) {
                  user.game!!.startTimeout = true
                }
              }
            }
            if (!user.loggedIn &&
                System.currentTimeMillis() - user.connectTime > flags.maxPing * 15) {
              logger.atInfo().log("$user connection timeout!")
              user.stop()
              usersMap.remove(user.id)
            } else if (user.loggedIn &&
                System.currentTimeMillis() - user.lastKeepAlive > flags.keepAliveTimeout * 1000) {
              logger.atInfo().log("$user keepalive timeout!")
              try {
                quit(user, getString("KailleraServerImpl.ForcedQuitPingTimeout"))
              } catch (e: Exception) {
                logger
                    .atSevere()
                    .withCause(e)
                    .log("Error forcing $user quit for keepalive timeout!")
              }
            } else if (flags.idleTimeout > 0 &&
                access == AccessManager.ACCESS_NORMAL &&
                user.loggedIn &&
                (System.currentTimeMillis() - user.lastActivity > flags.idleTimeout * 1000)) {
              logger.atInfo().log("$user inactivity timeout!")
              try {
                quit(user, getString("KailleraServerImpl.ForcedQuitInactivityTimeout"))
              } catch (e: Exception) {
                logger
                    .atSevere()
                    .withCause(e)
                    .log("Error forcing $user quit for inactivity timeout!")
              }
            } else if (user.loggedIn && access < AccessManager.ACCESS_NORMAL) {
              logger.atInfo().log("$user banned!")
              try {
                quit(user, getString("KailleraServerImpl.ForcedQuitBanned"))
              } catch (e: Exception) {
                logger.atSevere().withCause(e).log("Error forcing $user quit because banned!")
              }
            } else if (user.loggedIn &&
                access == AccessManager.ACCESS_NORMAL &&
                !accessManager.isEmulatorAllowed(user.clientType)) {
              logger.atInfo().log("$user: emulator restricted!")
              try {
                quit(user, getString("KailleraServerImpl.ForcedQuitEmulatorRestricted"))
              } catch (e: Exception) {
                logger
                    .atSevere()
                    .withCause(e)
                    .log("Error forcing $user quit because emulator restricted!")
              }
            } else {}
          }
        }
      }
    } catch (e: Throwable) {
      if (!stopFlag) {
        logger.atSevere().withCause(e).log("KailleraServer thread caught unexpected exception: $e")
      }
    } finally {
      running = false
      logger.atFine().log("KailleraServer thread exiting...")
    }
  }

  init {
    val loginMessagesBuilder = ImmutableList.builder<String>()
    var i = 1
    while (hasString("KailleraServerImpl.LoginMessage.$i")) {
      loginMessagesBuilder.add(getString("KailleraServerImpl.LoginMessage.$i"))
      i++
    }
    loginMessages = loginMessagesBuilder.build()
    flags.connectionTypes.forEach(
        Consumer { type: String ->
          val ct = type.toInt()
          allowedConnectionTypes[ct] = true
        })
    if (flags.touchKaillera) {
      this.statsCollector = statsCollector
    }
    metrics.register(
        MetricRegistry.name(this.javaClass, "users", "idle"),
        Gauge {
          usersMap
              .values
              .stream()
              .filter { user: KailleraUserImpl? ->
                user!!.status == KailleraUser.STATUS_IDLE.toInt()
              }
              .count()
              .toInt()
        })
    metrics.register(
        MetricRegistry.name(this.javaClass, "users", "playing"),
        Gauge {
          usersMap
              .values
              .stream()
              .filter { user: KailleraUserImpl? ->
                user!!.status == KailleraUser.STATUS_PLAYING.toInt()
              }
              .count()
              .toInt()
        })
    metrics.register(
        MetricRegistry.name(this.javaClass, "games", "waiting"),
        Gauge {
          gamesMap
              .values
              .stream()
              .filter { game: KailleraGameImpl ->
                game.status == KailleraGame.STATUS_WAITING.toInt()
              }
              .count()
              .toInt()
        })
    metrics.register(
        MetricRegistry.name(this.javaClass, "games", "playing"),
        Gauge {
          gamesMap
              .values
              .stream()
              .filter { game: KailleraGameImpl ->
                game.status == KailleraGame.STATUS_PLAYING.toInt()
              }
              .count()
              .toInt()
        })
  }
}

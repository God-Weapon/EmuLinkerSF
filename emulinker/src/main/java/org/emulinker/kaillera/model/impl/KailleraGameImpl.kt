package org.emulinker.kaillera.model.impl

import com.google.common.flogger.FluentLogger
import java.lang.Exception
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.Throws
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.master.StatsCollector
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraGame.Companion.STATUS_NAMES
import org.emulinker.kaillera.model.KailleraUser
import org.emulinker.kaillera.model.KailleraUser.Companion.CONNECTION_TYPE_NAMES
import org.emulinker.kaillera.model.event.AllReadyEvent
import org.emulinker.kaillera.model.event.GameChatEvent
import org.emulinker.kaillera.model.event.GameDataEvent
import org.emulinker.kaillera.model.event.GameEvent
import org.emulinker.kaillera.model.event.GameInfoEvent
import org.emulinker.kaillera.model.event.GameStartedEvent
import org.emulinker.kaillera.model.event.GameStatusChangedEvent
import org.emulinker.kaillera.model.event.GameTimeoutEvent
import org.emulinker.kaillera.model.event.PlayerDesynchEvent
import org.emulinker.kaillera.model.event.UserDroppedGameEvent
import org.emulinker.kaillera.model.event.UserJoinedGameEvent
import org.emulinker.kaillera.model.event.UserQuitGameEvent
import org.emulinker.kaillera.model.exception.CloseGameException
import org.emulinker.kaillera.model.exception.DropGameException
import org.emulinker.kaillera.model.exception.GameChatException
import org.emulinker.kaillera.model.exception.GameDataException
import org.emulinker.kaillera.model.exception.GameKickException
import org.emulinker.kaillera.model.exception.JoinGameException
import org.emulinker.kaillera.model.exception.QuitGameException
import org.emulinker.kaillera.model.exception.StartGameException
import org.emulinker.kaillera.model.exception.UserReadyException
import org.emulinker.util.EmuLang

class KailleraGameImpl(
    override val id: Int,
    override val romName: String,
    owner: KailleraUserImpl,
    override val server: KailleraServerImpl,
    bufferSize: Int,
    timeoutMillis: Int,
    desynchTimeouts: Int
) : KailleraGame {

  override val owner: KailleraUserImpl = owner

  private val toString: String
  val startDate: Date
  private var lastAddress = "null"
  private var lastAddressCount = 0
  override var maxUsers = 8
    set(maxUsers) {
      field = maxUsers
      server.addEvent(GameStatusChangedEvent(server, this))
    }
  override var delay = 0
  var aEmulator = "any"
  var aConnection = "any"
  override var maxPing = 1000
  override var startN = -1
  override var p2P = false
  override var sameDelay = false
  @JvmField var swap = false
  override var highestPing = 0
    private set
  private val bufferSize: Int
  override var startTimeout = false
  override var startTimeoutTime: Long = 0
    private set
  private val timeoutMillis: Int
  private val desynchTimeouts: Int

  override val players: MutableList<KailleraUser> = CopyOnWriteArrayList()

  private val statsCollector: StatsCollector?
  private val kickedUsers: MutableList<String> = ArrayList()
  val mutedUsers: MutableList<String> = mutableListOf()

  override var status = KailleraGame.STATUS_WAITING.toInt()
    private set(status) {
      field = status
      server.addEvent(GameStatusChangedEvent(server, this))
    }

  var isSynched = false
    private set

  private val actionsPerMessage: Int

  override var playerActionQueue: Array<PlayerActionQueue?>? = null
    private set

  val autoFireDetector: AutoFireDetector?

  override fun getPlayerNumber(user: KailleraUser?): Int {
    return players.indexOf(user) + 1
  }

  override fun getPlayer(playerNumber: Int): KailleraUser? {
    if (playerNumber > players.size) {
      logger
          .atSevere()
          .log(
              this.toString() +
                  ": getPlayer(" +
                  playerNumber +
                  ") failed! (size = " +
                  players.size +
                  ")")
      return null
    }
    return players[playerNumber - 1]
  }

  override val numPlayers: Int
    get() = players.size

  override val clientType: String?
    get() = owner.clientType

  override fun toString(): String {
    return toString
  }

  fun toDetailedString(): String {
    return "KailleraGame[id=$id romName=$romName owner=$owner numPlayers=$numPlayers status=${STATUS_NAMES[status]}]"
  }

  val playingCount: Int
    get() {
      var count = 0
      for (player in players) {
        if (player!!.status == KailleraUser.STATUS_PLAYING.toInt()) {
          count++
        }
      }
      return count
    }

  // return dataQueues.size();
  //		return readyCount;
  val synchedCount: Int
    get() {
      if (playerActionQueue == null) return 0
      var count = 0
      for (i in playerActionQueue!!.indices) {
        if (playerActionQueue!![i]!!.isSynched) count++
      }
      return count

      // return dataQueues.size();
      //		return readyCount;
    }

  fun addEvent(event: GameEvent?) {
    for (player in players) (player!! as KailleraUserImpl).addEvent(event)
  }

  @Synchronized
  @Throws(GameChatException::class)
  override fun chat(user: KailleraUser?, message: String?) {
    if (!players.contains(user)) {
      logger.atWarning().log(user.toString() + " game chat denied: not in " + this)
      throw GameChatException(EmuLang.getString("KailleraGameImpl.GameChatErrorNotInGame"))
    }
    if (user!!.access == AccessManager.ACCESS_NORMAL) {
      if (server.maxGameChatLength > 0 && message!!.length > server.maxGameChatLength) {
        logger
            .atWarning()
            .log(user.toString() + " gamechat denied: Message Length > " + server.maxGameChatLength)
        addEvent(
            GameInfoEvent(
                this, EmuLang.getString("KailleraGameImpl.GameChatDeniedMessageTooLong"), user))
        throw GameChatException(EmuLang.getString("KailleraGameImpl.GameChatDeniedMessageTooLong"))
      }
    }
    logger.atInfo().log("$user, $this gamechat: $message")
    addEvent(GameChatEvent(this, user, message!!))
  }

  @Synchronized
  fun announce(announcement: String, user: KailleraUser?) {
    addEvent(GameInfoEvent(this, announcement, user))
  }

  @Synchronized
  @Throws(GameKickException::class)
  override fun kick(user: KailleraUser?, userID: Int) {
    if (user!!.access < AccessManager.ACCESS_ADMIN) {
      if (user != owner) {
        logger.atWarning().log("$user kick denied: not the owner of $this")
        throw GameKickException(EmuLang.getString("KailleraGameImpl.GameKickDeniedNotGameOwner"))
      }
    }
    if (user.id == userID) {
      logger.atWarning().log("$user kick denied: attempt to kick self")
      throw GameKickException(EmuLang.getString("KailleraGameImpl.GameKickDeniedCannotKickSelf"))
    }
    for (player in players) {
      if (player!!.id == userID) {
        try {
          if (user.access != AccessManager.ACCESS_SUPERADMIN) {
            if (player.access >= AccessManager.ACCESS_ADMIN) {
              return
            }
          }
          logger.atInfo().log("$user kicked: $userID from $this")
          // SF MOD - Changed to IP rather than ID
          kickedUsers.add(player.connectSocketAddress.address.hostAddress)
          player.quitGame()
          return
        } catch (e: Exception) {
          // this shouldn't happen
          logger
              .atSevere()
              .withCause(e)
              .log("Caught exception while making user quit game! This shouldn't happen!")
        }
      }
    }
    logger.atWarning().log("$user kick failed: user $userID not found in: $this")
    throw GameKickException(EmuLang.getString("KailleraGameImpl.GameKickErrorUserNotFound"))
  }

  @Synchronized
  @Throws(JoinGameException::class)
  override fun join(user: KailleraUser?): Int {
    val access = server.accessManager.getAccess(user!!.socketAddress!!.address)

    // SF MOD - Join room spam protection
    if (lastAddress == user.connectSocketAddress.address.hostAddress) {
      lastAddressCount++
      if (lastAddressCount >= 4) {
        logger.atInfo().log(user.toString() + " join spam protection: " + user.id + " from " + this)
        // SF MOD - Changed to IP rather than ID
        if (access < AccessManager.ACCESS_ADMIN) {
          kickedUsers.add(user.connectSocketAddress.address.hostAddress)
          try {
            user.quitGame()
          } catch (e: Exception) {}
          throw JoinGameException("Spam Protection")
        }
      }
    } else {
      lastAddressCount = 0
      lastAddress = user.connectSocketAddress.address.hostAddress
    }
    if (players.contains(user)) {
      logger.atWarning().log("$user join game denied: already in $this")
      throw JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameErrorAlreadyInGame"))
    }
    if (access < AccessManager.ACCESS_ELEVATED && numPlayers >= maxUsers) {
      logger.atWarning().log("$user join game denied: max users reached $this")
      throw JoinGameException("This room's user capacity has been reached.")
    }
    if (access < AccessManager.ACCESS_ELEVATED && user.ping > maxPing) {
      logger.atWarning().log("$user join game denied: max ping reached $this")
      throw JoinGameException("Your ping is too high for this room.")
    }
    if (access < AccessManager.ACCESS_ELEVATED && aEmulator != "any") {
      if (aEmulator != user.clientType) {
        logger
            .atWarning()
            .log(
                user.toString() +
                    " join game denied: owner doesn't allow that emulator: " +
                    user.clientType)
        throw JoinGameException("Owner only allows emulator version: $aEmulator")
      }
    }
    if (access < AccessManager.ACCESS_ELEVATED && aConnection != "any") {
      if (user.connectionType != owner.connectionType) {
        logger
            .atWarning()
            .log(
                user.toString() +
                    "join game denied: owner doesn't allow that connection type: " +
                    CONNECTION_TYPE_NAMES[user.connectionType.toInt()])
        throw JoinGameException(
            "Owner only allows connection type: " +
                CONNECTION_TYPE_NAMES[owner.connectionType.toInt()])
      }
    }
    if (access < AccessManager.ACCESS_ADMIN &&
        kickedUsers.contains(user.connectSocketAddress.address.hostAddress)) {
      logger.atWarning().log("$user join game denied: previously kicked: $this")
      throw JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameDeniedPreviouslyKicked"))
    }
    if (access == AccessManager.ACCESS_NORMAL && status != KailleraGame.STATUS_WAITING.toInt()) {
      logger.atWarning().log("$user join game denied: attempt to join game in progress: $this")
      throw JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameDeniedGameIsInProgress"))
    }
    if (mutedUsers.contains(user.connectSocketAddress.address.hostAddress)) {
      user.mute = true
    }
    players.add(user as KailleraUserImpl)
    user.playerNumber = players.size
    server.addEvent(GameStatusChangedEvent(server, this))
    logger.atInfo().log("$user joined: $this")
    addEvent(UserJoinedGameEvent(this, user))

    // SF MOD - /startn
    if (startN != -1) {
      if (players.size >= startN) {
        try {
          Thread.sleep(1000)
        } catch (e: Exception) {}
        try {
          start(owner)
        } catch (e: Exception) {}
      }
    }

    // if(user.equals(owner))
    // {

    // TODO(nue): Localize this welcome message?
    // announce(
    //     "Help: "
    //         + getServer().getReleaseInfo().getProductName()
    //         + " v"
    //         + getServer().getReleaseInfo().getVersionString()
    //         + ": "
    //         + getServer().getReleaseInfo().getReleaseDate()
    //         + " - Visit: www.EmuLinker.org",
    //     user);
    // announce("************************", user);
    // announce("Type /p2pon to ignore ALL server activity during gameplay.", user);
    // announce("This will reduce lag that you contribute due to a busy server.", user);
    // announce("If server is greater than 60 users, option is auto set.", user);
    // announce("************************", user);

    /*
    if(autoFireDetector != null)
    {
    	if(autoFireDetector.getSensitivity() > 0)
    	{
    		announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOn"));
    		announce(EmuLang.getString("KailleraGameImpl.AutofireCurrentSensitivity", autoFireDetector.getSensitivity()));
    	}
    	else
    	{
    		announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOff"));
    	}
    	announce(EmuLang.getString("KailleraGameImpl.GameHelp"));
    }
    */
    // }

    // new SF MOD - different emulator versions notifications
    if (access < AccessManager.ACCESS_ADMIN &&
        user.clientType != owner.clientType &&
        !owner.game!!.romName.startsWith("*"))
        addEvent(
            GameInfoEvent(
                this, user.name + " using different emulator version: " + user.clientType, null))
    return players.indexOf(user) + 1
  }

  @Synchronized
  @Throws(StartGameException::class)
  override fun start(user: KailleraUser?) {
    val access = server.accessManager.getAccess(user!!.socketAddress!!.address)
    if (user != owner && access < AccessManager.ACCESS_ADMIN) {
      logger.atWarning().log("$user start game denied: not the owner of $this")
      throw StartGameException(
          EmuLang.getString("KailleraGameImpl.StartGameDeniedOnlyOwnerMayStart"))
    }
    if (status == KailleraGame.STATUS_SYNCHRONIZING.toInt()) {
      logger
          .atWarning()
          .log(
              user.toString() +
                  " start game failed: " +
                  this +
                  " status is " +
                  STATUS_NAMES[status])
      throw StartGameException(EmuLang.getString("KailleraGameImpl.StartGameErrorSynchronizing"))
    } else if (status == KailleraGame.STATUS_PLAYING.toInt()) {
      logger
          .atWarning()
          .log(
              user.toString() +
                  " start game failed: " +
                  this +
                  " status is " +
                  STATUS_NAMES[status])
      throw StartGameException(EmuLang.getString("KailleraGameImpl.StartGameErrorStatusIsPlaying"))
    }
    if (access == AccessManager.ACCESS_NORMAL && numPlayers < 2 && !server.allowSinglePlayer) {
      logger.atWarning().log("$user start game denied: $this needs at least 2 players")
      throw StartGameException(
          EmuLang.getString("KailleraGameImpl.StartGameDeniedSinglePlayerNotAllowed"))
    }

    // do not start if not game
    if (owner.game!!.romName.startsWith("*")) return
    for (player in players) {
      if (player!!.stealth == false) {
        if (player.connectionType != owner.connectionType) {
          logger
              .atWarning()
              .log(
                  user.toString() +
                      " start game denied: " +
                      this +
                      ": All players must use the same connection type")
          addEvent(
              GameInfoEvent(
                  this,
                  EmuLang.getString(
                      "KailleraGameImpl.StartGameConnectionTypeMismatchInfo",
                      CONNECTION_TYPE_NAMES[owner.connectionType.toInt()]),
                  null))
          throw StartGameException(
              EmuLang.getString("KailleraGameImpl.StartGameDeniedConnectionTypeMismatch"))
        }
        if (player.clientType != clientType) {
          logger
              .atWarning()
              .log("$user start game denied: $this: All players must use the same emulator!")
          addEvent(
              GameInfoEvent(
                  this,
                  EmuLang.getString("KailleraGameImpl.StartGameEmulatorMismatchInfo", clientType),
                  null))
          throw StartGameException(
              EmuLang.getString("KailleraGameImpl.StartGameDeniedEmulatorMismatch"))
        }
      }
    }
    logger.atInfo().log("$user started: $this")
    status = KailleraGame.STATUS_SYNCHRONIZING.toInt()
    autoFireDetector?.start(players.size)
    playerActionQueue = arrayOfNulls(players.size)
    startTimeout = false
    delay = 1
    if (server.users.size > 60) {
      p2P = true
    }
    var i = 0
    while (i < playerActionQueue!!.size && i < players.size) {
      val player = players[i]
      val playerNumber = i + 1
      if (!swap) player.playerNumber = playerNumber
      player.timeouts = 0
      player.frameCount = 0
      playerActionQueue!![i] =
          PlayerActionQueue(
              playerNumber, player as KailleraUserImpl, numPlayers, bufferSize, timeoutMillis, true)
      // playerActionQueues[i] = new PlayerActionQueue(playerNumber, player, getNumPlayers(),
      // GAME_BUFFER_SIZE, (player.getPing()*3));
      // SF MOD - player.setPlayerNumber(playerNumber);
      // SF MOD - Delay Value = [(60/connectionType) * (ping/1000)] + 1
      val delayVal = 60 / player.connectionType * (player.ping.toDouble() / 1000) + 1
      player.delay = delayVal.toInt()
      if (delayVal.toInt() > delay) {
        delay = delayVal.toInt()
      }
      if (player.ping > highestPing) {
        highestPing = user.ping
      }
      if (p2P) {
        player.p2P = true
        announce("This game is ignoring ALL server activity during gameplay!", player)
      }
      /*else{
      	player.setP2P(false);
      }*/ logger.atInfo().log("$this: $player is player number $playerNumber")
      autoFireDetector?.addPlayer(player, playerNumber)
      i++
    }
    statsCollector?.markGameAsStarted(server, this)

    /*if(user.getConnectionType() > KailleraUser.CONNECTION_TYPE_GOOD || user.getConnectionType() < KailleraUser.CONNECTION_TYPE_GOOD){
    	//sameDelay = true;
    }*/

    // timeoutMillis = highestPing;
    addEvent(GameStartedEvent(this))
  }

  @Synchronized
  @Throws(UserReadyException::class)
  override fun ready(user: KailleraUser?, playerNumber: Int) {
    if (!players.contains(user)) {
      logger.atWarning().log(user.toString() + " ready game failed: not in " + this)
      throw UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorNotInGame"))
    }
    if (status != KailleraGame.STATUS_SYNCHRONIZING.toInt()) {
      logger
          .atWarning()
          .log(user.toString() + " ready failed: " + this + " status is " + STATUS_NAMES[status])
      throw UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorIncorrectState"))
    }
    if (playerActionQueue == null) {
      logger
          .atSevere()
          .log(user.toString() + " ready failed: " + this + " playerActionQueues == null!")
      throw UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorInternalError"))
    }
    logger
        .atInfo()
        .log(user.toString() + " (player " + playerNumber + ") is ready to play: " + this)
    playerActionQueue!![playerNumber - 1]!!.isSynched = true
    if (synchedCount == numPlayers) {
      logger.atInfo().log("$this all players are ready: starting...")
      status = KailleraGame.STATUS_PLAYING.toInt()
      isSynched = true
      startTimeoutTime = System.currentTimeMillis()
      addEvent(AllReadyEvent(this))
      var frameDelay = (delay + 1) * owner.connectionType - 1
      if (sameDelay) {
        announce("This game's delay is: $delay ($frameDelay frame delay)", null)
      } else {
        var i = 0
        while (i < playerActionQueue!!.size && i < players.size) {
          val player = players[i]
          // do not show delay if stealth mode
          if (player != null && !player.stealth) {
            frameDelay = (player.delay + 1) * player.connectionType - 1
            announce(
                "P" + (i + 1) + " Delay = " + player.delay + " (" + frameDelay + " frame delay)",
                null)
          }
          i++
        }
      }
    }
  }

  @Synchronized
  @Throws(DropGameException::class)
  override fun drop(user: KailleraUser, playerNumber: Int) {
    if (!players.contains(user)) {
      logger.atWarning().log(user.toString() + " drop game failed: not in " + this)
      throw DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorNotInGame"))
    }
    if (playerActionQueue == null) {
      logger
          .atSevere()
          .log(user.toString() + " drop failed: " + this + " playerActionQueues == null!")
      throw DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorInternalError"))
    }
    logger.atInfo().log(user.toString() + " dropped: " + this)
    if (playerNumber - 1 < playerActionQueue!!.size)
        playerActionQueue!![playerNumber - 1]!!.isSynched = false
    if (synchedCount < 2 && isSynched) {
      isSynched = false
      for (q in playerActionQueue!!) q!!.isSynched = false
      logger.atInfo().log("$this: game desynched: less than 2 players playing!")
    }
    autoFireDetector?.stop(playerNumber)
    if (playingCount == 0) {
      if (startN != -1) {
        startN = -1
        announce("StartN is now off.", null)
      }
      status = KailleraGame.STATUS_WAITING.toInt()
    }
    addEvent(UserDroppedGameEvent(this, user, playerNumber))
    if (user!!.p2P) {
      // KailleraUserImpl u = (KailleraUserImpl) user;
      // u.addEvent(ServerACK.create(.getNextMessageNumber());
      // u.addEvent(new ConnectedEvent(server, user));
      // u.addEvent(new UserQuitEvent(server, user, "Rejoining..."));
      // try{user.quit("Rejoining...");}catch(Exception e){}
      announce("Rejoin server to update client of ignored server activity!", user)
    }
  }

  @Throws(DropGameException::class, QuitGameException::class, CloseGameException::class)
  override fun quit(user: KailleraUser, playerNumber: Int) {
    synchronized(this) {
      if (!players.remove(user)) {
        logger.atWarning().log("$user quit game failed: not in $this")
        throw QuitGameException(EmuLang.getString("KailleraGameImpl.QuitGameErrorNotInGame"))
      }
      logger.atInfo().log("$user quit: $this")
      addEvent(UserQuitGameEvent(this, user))
      user.p2P = false
      swap = false
      if (status == KailleraGame.STATUS_WAITING.toInt()) {
        for (i in 0 until numPlayers) {
          getPlayer(i + 1)!!.playerNumber = i + 1
          logger.atFine().log(getPlayer(i + 1)!!.name + ":::" + getPlayer(i + 1)!!.playerNumber)
        }
      }
    }
    if (user == owner) server.closeGame(this, user)
    else server.addEvent(GameStatusChangedEvent(server, this))
  }

  @Synchronized
  @Throws(CloseGameException::class)
  fun close(user: KailleraUser) {
    if (user != owner) {
      logger.atWarning().log("$user close game denied: not the owner of $this")
      throw CloseGameException(EmuLang.getString("KailleraGameImpl.CloseGameErrorNotGameOwner"))
    }
    if (isSynched) {
      isSynched = false
      for (q in playerActionQueue!!) q!!.isSynched = false
      logger.atInfo().log("$this: game desynched: game closed!")
    }
    for (player in players) {
      (player!! as KailleraUserImpl).apply {
        status = KailleraUser.STATUS_IDLE.toInt()
        mute = false
        p2P = false
        p2P = false
        game = null
      }
    }
    autoFireDetector?.stop()
    players.clear()
  }

  @Synchronized
  override fun droppedPacket(user: KailleraUser?) {
    if (!isSynched) return
    val playerNumber = user!!.playerNumber
    if (user.playerNumber > playerActionQueue!!.size) {
      logger
          .atInfo()
          .log(
              this.toString() +
                  ": " +
                  user +
                  ": player desynched: dropped a packet! Also left the game already: KailleraGameImpl -> DroppedPacket")
    }
    if (playerActionQueue != null && playerActionQueue!![playerNumber - 1]!!.isSynched) {
      playerActionQueue!![playerNumber - 1]!!.isSynched = false
      logger.atInfo().log("$this: $user: player desynched: dropped a packet!")
      addEvent(
          PlayerDesynchEvent(
              this,
              user,
              EmuLang.getString("KailleraGameImpl.DesynchDetectedDroppedPacket", user.name)))
      if (synchedCount < 2 && isSynched) {
        isSynched = false
        for (q in playerActionQueue!!) q!!.isSynched = false
        logger.atInfo().log("$this: game desynched: less than 2 players synched!")
      }
    }
  }

  @Throws(GameDataException::class)
  override fun addData(user: KailleraUser?, playerNumber: Int, data: ByteArray?) {
    if (playerActionQueue == null) return

    // int bytesPerAction = (data.length / actionsPerMessage);
    var timeoutCounter = 0
    // int arraySize = (playerActionQueues.length * actionsPerMessage * user.getBytesPerAction());
    if (!isSynched) {
      throw GameDataException(
          EmuLang.getString("KailleraGameImpl.DesynchedWarning"),
          data!!,
          actionsPerMessage,
          playerNumber,
          playerActionQueue!!.size)
    }
    playerActionQueue!![playerNumber - 1]!!.addActions(data)
    autoFireDetector?.addData(playerNumber, data, user!!.bytesPerAction)
    val response = ByteArray(user!!.arraySize)
    for (actionCounter in 0 until actionsPerMessage) {
      for (playerCounter in playerActionQueue!!.indices) {
        while (isSynched) {
          try {
            playerActionQueue!![playerCounter]!!.getAction(
                playerNumber,
                response,
                actionCounter * (playerActionQueue!!.size * user.bytesPerAction) +
                    playerCounter * user.bytesPerAction,
                user.bytesPerAction)
            break
          } catch (e: PlayerTimeoutException) {
            e.timeoutNumber = ++timeoutCounter
            handleTimeout(e)
          }
        }
      }
    }
    if (!isSynched)
        throw GameDataException(
            EmuLang.getString("KailleraGameImpl.DesynchedWarning"),
            data!!,
            user.bytesPerAction,
            playerNumber,
            playerActionQueue!!.size)
    (user as KailleraUserImpl?)!!.addEvent(GameDataEvent(this, response))
  }

  // it's very important this method is synchronized
  @Synchronized
  private fun handleTimeout(e: PlayerTimeoutException) {
    if (!isSynched) return
    val playerNumber = e.playerNumber
    val timeoutNumber = e.timeoutNumber
    val playerActionQueue = playerActionQueue!![playerNumber - 1]
    if (!playerActionQueue!!.isSynched || e == playerActionQueue.lastTimeout) return
    playerActionQueue.lastTimeout = e
    val player: KailleraUser = e.player!!
    if (timeoutNumber < desynchTimeouts) {
      if (startTimeout) player.timeouts = player.timeouts + 1
      if (timeoutNumber % 12 == 0) {
        logger.atInfo().log(this.toString() + ": " + player + ": Timeout #" + timeoutNumber / 12)
        addEvent(GameTimeoutEvent(this, player, timeoutNumber / 12))
      }
    } else {
      logger.atInfo().log(this.toString() + ": " + player + ": Timeout #" + timeoutNumber / 12)
      playerActionQueue.isSynched = false
      logger.atInfo().log("$this: $player: player desynched: Lagged!")
      addEvent(
          PlayerDesynchEvent(
              this,
              player,
              EmuLang.getString("KailleraGameImpl.DesynchDetectedPlayerLagged", player.name)))
      if (synchedCount < 2) {
        isSynched = false
        for (q in this.playerActionQueue!!) q!!.isSynched = false
        logger.atInfo().log("$this: game desynched: less than 2 players synched!")
      }
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val chatFloodTime = 3
  }

  init {
    actionsPerMessage = owner.connectionType.toInt()
    this.bufferSize = bufferSize
    this.timeoutMillis = 100 // timeoutMillis;
    this.desynchTimeouts = 120 // desynchTimeouts;
    toString =
        String.format(
            "Game%d(%s)",
            id,
            if (romName.length > 15) romName.substring(0, 15) + "..." else romName)
    startDate = Date()
    statsCollector = server.getStatsCollector()
    autoFireDetector = server.getAutoFireDetector(this)
  }
}

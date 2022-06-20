package org.emulinker.kaillera.model.impl

import com.google.common.flogger.FluentLogger
import java.lang.Exception
import java.lang.InterruptedException
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.Throws
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.model.ConnectionType
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser
import org.emulinker.kaillera.model.UserStatus
import org.emulinker.kaillera.model.event.*
import org.emulinker.kaillera.model.exception.*
import org.emulinker.util.EmuLang
import org.emulinker.util.EmuUtil
import org.emulinker.util.Executable

private val logger = FluentLogger.forEnclosingClass()

private const val EMULINKER_CLIENT_NAME = "EmulinkerSF Admin Client"

class KailleraUserImpl(
    override val id: Int,
    override val protocol: String,
    override val connectSocketAddress: InetSocketAddress,
    override val listener: KailleraEventListener,
    override val server: KailleraServerImpl
) : KailleraUser, Executable {

  override var inStealthMode = false

  /** Example: "Project 64k 0.13 (01 Aug 2003)" */
  override var clientType: String? = null
    set(clientType) {
      field = clientType
      if (clientType != null && clientType.startsWith(EMULINKER_CLIENT_NAME))
          isEmuLinkerClient = true
    }

  private val initTime = System.currentTimeMillis()

  override var connectionType: ConnectionType =
      ConnectionType.DISABLED // TODO(nue): This probably shouldn't have a default.
  override var ping = 0
  override var socketAddress: InetSocketAddress? = null
  override var status =
      UserStatus.PLAYING // TODO(nue): This probably shouldn't have a default value..
  override var accessLevel = 0
  override var isEmuLinkerClient = false
    private set
  override val connectTime: Long = initTime
  override var timeouts = 0
  override var lastActivity: Long = initTime
    private set

  override fun updateLastActivity() {
    lastKeepAlive = System.currentTimeMillis()
    lastActivity = lastKeepAlive
  }

  override var lastKeepAlive: Long = initTime
    private set
  var lastChatTime: Long = initTime
    private set
  var lastCreateGameTime: Long = 0
    private set
  override var frameCount = 0
  override var frameDelay = 0

  private var totalDelay = 0
  override var bytesPerAction = 0
    private set

  override var arraySize = 0
    private set

  override var p2P = false

  override var playerNumber = -1
  override var ignoreAll = false
  override var isAcceptingDirectMessages = true
  override var lastMsgID = -1
  override var isMuted = false

  private val lostInput: MutableList<ByteArray> = ArrayList()
  /** Note that this is a different type from lostInput. */
  override fun getLostInput(): ByteArray {
    return lostInput[0]
  }

  private val ignoredUsers: MutableList<String> = ArrayList()
  private var gameDataErrorTime: Long = -1

  override var threadIsActive = false
    private set

  private var stopFlag = false
  private val eventQueue: BlockingQueue<KailleraEvent> = LinkedBlockingQueue()

  override var tempDelay = 0

  override val users: Collection<KailleraUserImpl>
    get() = server.users

  override fun addIgnoredUser(address: String) {
    ignoredUsers.add(address)
  }

  override fun findIgnoredUser(address: String): Boolean {
    for (i in ignoredUsers.indices) {
      if (ignoredUsers[i] == address) {
        return true
      }
    }
    return false
  }

  override fun removeIgnoredUser(address: String, removeAll: Boolean): Boolean {
    var i = 1
    var here = false
    if (removeAll) {
      ignoredUsers.clear()
      return true
    }
    i = 0
    while (i < ignoredUsers.size) {
      if (ignoredUsers[i] == address) {
        ignoredUsers.removeAt(i)
        here = true
      }
      i++
    }
    return here
  }

  override fun searchIgnoredUsers(address: String): Boolean {
    var i = 1
    i = 0
    while (i < ignoredUsers.size) {
      if (ignoredUsers[i] == address) {
        return true
      }
      i++
    }
    return false
  }

  override var loggedIn = false

  override fun toString(): String {
    val n = name
    return if (n == null) {
      "User$id(${connectSocketAddress.address.hostAddress})"
    } else {
      "User$id(${if (n.length > 15) n.take(15) + "..." else n}/${connectSocketAddress.address.hostAddress})"
    }
  }

  override var name: String? = null

  override fun updateLastKeepAlive() {
    lastKeepAlive = System.currentTimeMillis()
  }

  override var game: KailleraGameImpl? = null
    set(value) {
      if (value == null) {
        playerNumber = -1
      }
      field = value
    }

  val accessStr: String
    get() = AccessManager.ACCESS_NAMES[accessLevel]

  override fun equals(other: Any?): Boolean {
    return other is KailleraUserImpl && other.id == id
  }

  fun toDetailedString(): String {
    return ("KailleraUserImpl[id=$id protocol=$protocol status=$status name=$name clientType=$clientType ping=$ping connectionType=$connectionType remoteAddress=" +
        (if (socketAddress == null) EmuUtil.formatSocketAddress(connectSocketAddress)
        else EmuUtil.formatSocketAddress(socketAddress!!)) +
        "]")
  }

  override fun stop() {
    synchronized(this) {
      if (!threadIsActive) {
        logger.atFine().log("$this  thread stop request ignored: not running!")
        return
      }
      if (stopFlag) {
        logger.atFine().log("$this  thread stop request ignored: already stopping!")
        return
      }
      stopFlag = true
      try {
        Thread.sleep(500)
      } catch (e: Exception) {}
      addEvent(StopFlagEvent())
    }
    listener.stop()
  }

  @Synchronized
  override fun droppedPacket() {
    if (game != null) {
      // if(game.getStatus() == KailleraGame.STATUS_PLAYING){
      game!!.droppedPacket(this)
      // }
    }
  }

  // server actions
  @Synchronized
  @Throws(
      PingTimeException::class,
      ClientAddressException::class,
      ConnectionTypeException::class,
      UserNameException::class,
      LoginException::class)
  override fun login() {
    updateLastActivity()
    server.login(this)
  }

  @Synchronized
  @Throws(ChatException::class, FloodException::class)
  override fun chat(message: String?) {
    updateLastActivity()
    server.chat(this, message)
    lastChatTime = System.currentTimeMillis()
  }

  @Synchronized
  @Throws(GameKickException::class)
  override fun gameKick(userID: Int) {
    updateLastActivity()
    if (game == null) {
      logger.atWarning().log("$this kick User $userID failed: Not in a game")
      throw GameKickException(EmuLang.getString("KailleraUserImpl.KickErrorNotInGame"))
    }
    game!!.kick(this, userID)
  }

  @Synchronized
  @Throws(CreateGameException::class, FloodException::class)
  override fun createGame(romName: String?): KailleraGame? {
    updateLastActivity()
    if (server.getUser(id) == null) {
      logger.atSevere().log("$this create game failed: User don't exist!")
      return null
    }
    if (status == UserStatus.PLAYING) {
      logger.atWarning().log("$this create game failed: User status is Playing!")
      throw CreateGameException(EmuLang.getString("KailleraUserImpl.CreateGameErrorAlreadyInGame"))
    } else if (status == UserStatus.CONNECTING) {
      logger.atWarning().log("$this create game failed: User status is Connecting!")
      throw CreateGameException(
          EmuLang.getString("KailleraUserImpl.CreateGameErrorNotFullyConnected"))
    }
    val game = server.createGame(this, romName)
    lastCreateGameTime = System.currentTimeMillis()
    return game
  }

  @Synchronized
  @Throws(
      QuitException::class,
      DropGameException::class,
      QuitGameException::class,
      CloseGameException::class)
  override fun quit(message: String?) {
    updateLastActivity()
    server.quit(this, message)
    loggedIn = false
  }

  @Synchronized
  @Throws(JoinGameException::class)
  override fun joinGame(gameID: Int): KailleraGame {
    updateLastActivity()
    if (game != null) {
      logger.atWarning().log("$this join game failed: Already in: $game")
      throw JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorAlreadyInGame"))
    }
    if (status == UserStatus.PLAYING) {
      logger.atWarning().log("$this join game failed: User status is Playing!")
      throw JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorAnotherGameRunning"))
    } else if (status == UserStatus.CONNECTING) {
      logger.atWarning().log("$this join game failed: User status is Connecting!")
      throw JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorNotFullConnected"))
    }
    val game = server.getGame(gameID)
    if (game == null) {
      logger.atWarning().log("$this join game failed: Game $gameID does not exist!")
      throw JoinGameException(EmuLang.getString("KailleraUserImpl.JoinGameErrorDoesNotExist"))
    }

    // if (connectionType != game.getOwner().getConnectionType())
    // {
    //	logger.atWarning().log(this + " join game denied: " + this + ": You must use the same
    // connection type as
    // the owner: " + game.getOwner().getConnectionType());
    //	throw new
    // JoinGameException(EmuLang.getString("KailleraGameImpl.StartGameConnectionTypeMismatchInfo"));
    //
    // }
    playerNumber = game.join(this)
    this.game = game as KailleraGameImpl?
    gameDataErrorTime = -1
    return game
  }

  // game actions
  @Synchronized
  @Throws(GameChatException::class)
  override fun gameChat(message: String, messageID: Int) {
    updateLastActivity()
    if (game == null) {
      logger.atWarning().log("$this game chat failed: Not in a game")
      throw GameChatException(EmuLang.getString("KailleraUserImpl.GameChatErrorNotInGame"))
    }
    if (isMuted) {
      logger.atWarning().log("$this gamechat denied: Muted: $message")
      game!!.announce("You are currently muted!", this)
      return
    }
    if (server.accessManager.isSilenced(socketAddress!!.address)) {
      logger.atWarning().log("$this gamechat denied: Silenced: $message")
      game!!.announce("You are currently silenced!", this)
      return
    }

    /*if(this == null){
    	throw new GameChatException("You don't exist!");
    }*/ game!!.chat(this, message)
  }

  @Synchronized
  @Throws(DropGameException::class)
  override fun dropGame() {
    updateLastActivity()
    if (status == UserStatus.IDLE) {
      return
    }
    status = UserStatus.IDLE
    if (game != null) {
      game!!.drop(this, playerNumber)
      // not necessary to show it twice
      /*if(p2P == true)
      	game.announce("Please Relogin, to update your client of missed server activity during P2P!", this);
      p2P = false;*/
    } else logger.atFine().log("$this drop game failed: Not in a game")
  }

  @Synchronized
  @Throws(DropGameException::class, QuitGameException::class, CloseGameException::class)
  override fun quitGame() {
    updateLastActivity()
    if (game == null) {
      logger.atFine().log("$this quit game failed: Not in a game")
      // throw new QuitGameException("You are not in a game!");
      return
    }
    if (status == UserStatus.PLAYING) {
      // first set STATUS_IDLE and then call game.drop, otherwise if someone
      // quit game whitout drop - game status will not change to STATUS_WAITING
      status = UserStatus.IDLE
      game!!.drop(this, playerNumber)
    }
    game!!.quit(this, playerNumber)
    if (status != UserStatus.IDLE) {
      status = UserStatus.IDLE
    }
    isMuted = false
    game = null
    addEvent(UserQuitGameEvent(game, this))
  }

  @Synchronized
  @Throws(StartGameException::class)
  override fun startGame() {
    updateLastActivity()
    if (game == null) {
      logger.atWarning().log("$this start game failed: Not in a game")
      throw StartGameException(EmuLang.getString("KailleraUserImpl.StartGameErrorNotInGame"))
    }
    game!!.start(this)
  }

  @Synchronized
  @Throws(UserReadyException::class)
  override fun playerReady() {
    updateLastActivity()
    if (game == null) {
      logger.atWarning().log("$this player ready failed: Not in a game")
      throw UserReadyException(EmuLang.getString("KailleraUserImpl.PlayerReadyErrorNotInGame"))
    }
    if (playerNumber > game!!.playerActionQueue!!.size ||
        game!!.playerActionQueue!![playerNumber - 1].synched) {
      return
    }
    totalDelay = game!!.highestUserFrameDelay + tempDelay + 5
    game!!.ready(this, playerNumber)
  }

  @Throws(GameDataException::class)
  override fun addGameData(data: ByteArray) {
    updateLastActivity()
    try {
      if (game == null)
          throw GameDataException(
              EmuLang.getString("KailleraUserImpl.GameDataErrorNotInGame"),
              data,
              connectionType.byteValue.toInt(),
              1,
              1)

      // Initial Delay
      // totalDelay = (game.getDelay() + tempDelay + 5)
      if (frameCount < totalDelay) {
        bytesPerAction = data.size / connectionType.byteValue
        arraySize = game!!.playerActionQueue!!.size * connectionType.byteValue * bytesPerAction
        val response = ByteArray(arraySize)
        for (i in response.indices) {
          response[i] = 0
        }
        lostInput.add(data)
        addEvent(GameDataEvent(game as KailleraGameImpl, response))
        frameCount++
      } else {
        // lostInput.add(data);
        if (lostInput.size > 0) {
          game!!.addData(this, playerNumber, lostInput[0])
          lostInput.removeAt(0)
        } else {
          game!!.addData(this, playerNumber, data)
        }
      }
      gameDataErrorTime = 0
    } catch (e: GameDataException) {
      // this should be warn level, but it creates tons of lines in the log
      logger.atFine().withCause(e).log("$this add game data failed")

      // i'm going to reflect the game data packet back at the user to prevent game lockups,
      // but this uses extra bandwidth, so we'll set a counter to prevent people from leaving
      // games running for a long time in this state
      if (gameDataErrorTime > 0) {
        if (System.currentTimeMillis() - gameDataErrorTime >
            30000) // give the user time to close the game
        {
          // this should be warn level, but it creates tons of lines in the log
          logger.atFine().log("$this: error game data exceeds drop timeout!")
          throw GameDataException(e.message)
        } else {
          // e.setReflectData(true);
          throw e
        }
      } else {
        gameDataErrorTime = System.currentTimeMillis()
        throw e
      }
    }
  }

  fun addEvent(event: KailleraEvent?) {
    if (event == null) {
      logger.atSevere().log("$this: ignoring null event!")
      return
    }
    if (status != UserStatus.IDLE) {
      if (p2P) {
        if (event.toString() == "InfoMessageEvent") return
      }
    }
    eventQueue.offer(event)
  }

  override fun run() {
    threadIsActive = true
    logger.atFine().log("$this thread running...")
    try {
      while (!stopFlag) {
        val event = eventQueue.poll(200, TimeUnit.SECONDS)
        if (event == null) continue else if (event is StopFlagEvent) break
        listener.actionPerformed(event)
        if (event is GameStartedEvent) {
          status = UserStatus.PLAYING
        } else if (event is UserQuitEvent && event.user == this) {
          stop()
        }
      }
    } catch (e: InterruptedException) {
      logger.atSevere().withCause(e).log("$this thread interrupted!")
    } catch (e: Throwable) {
      logger.atSevere().withCause(e).log("$this thread caught unexpected exception!")
    } finally {
      threadIsActive = false
      logger.atFine().log("$this thread exiting...")
    }
  }
}

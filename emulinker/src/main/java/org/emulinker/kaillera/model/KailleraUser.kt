package org.emulinker.kaillera.model

import java.net.InetSocketAddress
import java.time.Instant
import kotlin.Throws
import kotlinx.coroutines.sync.Mutex
import org.emulinker.kaillera.model.event.KailleraEventListener
import org.emulinker.kaillera.model.exception.*
import org.emulinker.kaillera.model.impl.KailleraGameImpl
import org.emulinker.kaillera.model.impl.KailleraUserImpl

interface KailleraUser {
  // Fields that only support getters.
  val id: Int
  /**
   * Level of access that the user has.
   *
   * See AdminCommandAction for available values. This should be turned into an enum.
   */
  val accessLevel: Int
  /** User action data response message size (in number of bytes). */
  val arraySize: Int
  val bytesPerAction: Int
  val connectSocketAddress: InetSocketAddress
  val connectTime: Instant
  val frameDelay: Int
  val game: KailleraGameImpl?
  val isEmuLinkerClient: Boolean
  val lastActivity: Instant
  val lastKeepAlive: Instant
  val listener: KailleraEventListener
  val loggedIn: Boolean
  val protocol: String
  val server: KailleraServer
  val status: UserStatus
  val users: Collection<KailleraUserImpl?>?

  // Fields with public getters and setters.
  var clientType: String?
  var connectionType: ConnectionType
  var frameCount: Int
  var ignoreAll: Boolean
  var lastMsgID: Int
  var isAcceptingDirectMessages: Boolean
  var isMuted: Boolean
  var name: String? // TODO(nue): Remove this "?"
  /**
   * This is called "p2p mode" in the code and commands.
   *
   * See the command /p2pon.
   */
  var ignoringUnnecessaryServerActivity: Boolean
  var ping: Int
  var playerNumber: Int // TODO(nue): Make this nullable.
  var socketAddress: InetSocketAddress
  var inStealthMode: Boolean
  var tempDelay: Int
  var timeouts: Int
  var smallLagSpikesCausedByUser: Long
  var bigLagSpikesCausedByUser: Long

  // Methods.
  @Throws(
      PingTimeException::class,
      ClientAddressException::class,
      ConnectionTypeException::class,
      UserNameException::class,
      LoginException::class)
  suspend fun login()

  fun updateLastActivity()

  fun updateLastKeepAlive()

  fun getLostInput(): ByteArray

  fun findIgnoredUser(address: String): Boolean

  fun removeIgnoredUser(address: String, removeAll: Boolean): Boolean

  fun searchIgnoredUsers(address: String): Boolean

  fun addIgnoredUser(address: String)

  @Throws(ChatException::class, FloodException::class)
  fun chat(message: String)

  @Throws(CreateGameException::class, FloodException::class)
  suspend fun createGame(romName: String): KailleraGame

  @Throws(
      QuitException::class,
      DropGameException::class,
      QuitGameException::class,
      CloseGameException::class)
  fun quit(message: String?)

  @Throws(JoinGameException::class)
  suspend fun joinGame(gameID: Int): KailleraGame

  @Throws(StartGameException::class)
  fun startGame()

  @Throws(GameChatException::class)
  fun gameChat(message: String, messageID: Int)

  @Throws(GameKickException::class)
  fun gameKick(userID: Int)

  @Throws(UserReadyException::class)
  fun playerReady()

  @Throws(GameDataException::class)
  fun addGameData(data: ByteArray)

  @Throws(DropGameException::class)
  fun dropGame()

  @Throws(DropGameException::class, QuitGameException::class, CloseGameException::class)
  fun quitGame()

  fun droppedPacket()

  suspend fun stop()
  val mutex: Mutex
}

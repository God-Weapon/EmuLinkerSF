package org.emulinker.kaillera.model

import java.net.InetSocketAddress
import kotlin.Throws
import org.emulinker.kaillera.model.event.KailleraEventListener
import org.emulinker.kaillera.model.exception.*
import org.emulinker.kaillera.model.impl.KailleraGameImpl
import org.emulinker.kaillera.model.impl.KailleraUserImpl

interface KailleraUser {
  // Fields that only support getters.
  val access: Int
  val arraySize: Int
  val bytesPerAction: Int
  val connectSocketAddress: InetSocketAddress
  val connectTime: Long
  val delay: Int
  val game: KailleraGameImpl?
  val id: Int
  val isEmuLinkerClient: Boolean
  val lastActivity: Long
  val lastKeepAlive: Long
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
  var msg: Boolean
  var mute: Boolean
  var name: String? // TODO(nue): Remove this "?"
  var p2P: Boolean
  var ping: Int
  var playerNumber: Int
  var socketAddress: InetSocketAddress?
  var stealth: Boolean
  var tempDelay: Int
  var timeouts: Int

  // Methods.
  @Throws(
      PingTimeException::class,
      ClientAddressException::class,
      ConnectionTypeException::class,
      UserNameException::class,
      LoginException::class)
  fun login()

  fun updateLastActivity()

  fun updateLastKeepAlive()

  fun getLostInput(): ByteArray

  fun findIgnoredUser(address: String): Boolean

  fun removeIgnoredUser(address: String, removeAll: Boolean): Boolean

  fun searchIgnoredUsers(address: String): Boolean

  fun addIgnoredUser(address: String)

  @Throws(ChatException::class, FloodException::class)
  fun chat(message: String?)

  @Throws(CreateGameException::class, FloodException::class)
  fun createGame(romName: String?): KailleraGame?

  @Throws(
      QuitException::class,
      DropGameException::class,
      QuitGameException::class,
      CloseGameException::class)
  fun quit(message: String?)

  @Throws(JoinGameException::class)
  fun joinGame(gameID: Int): KailleraGame

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

  fun stop()
}

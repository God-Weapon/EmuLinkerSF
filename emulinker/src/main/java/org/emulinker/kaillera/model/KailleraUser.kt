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
  val status: Int
  val users: Collection<KailleraUserImpl?>?

  // Fields with public getters and setters.
  var clientType: String?
  var connectionType: Byte
  var frameCount: Int
  var ignoreAll: Boolean
  var lastMsgID: Int
  var msg: Boolean
  var mute: Boolean
  var name: String?
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

  companion object {
    const val CONNECTION_TYPE_LAN: Byte = 1
    const val CONNECTION_TYPE_EXCELLENT: Byte = 2
    const val CONNECTION_TYPE_GOOD: Byte = 3
    const val CONNECTION_TYPE_AVERAGE: Byte = 4
    const val CONNECTION_TYPE_LOW: Byte = 5
    const val CONNECTION_TYPE_BAD: Byte = 6

    const val STATUS_PLAYING: Byte = 0
    const val STATUS_IDLE: Byte = 1
    const val STATUS_CONNECTING: Byte = 2

    val CONNECTION_TYPE_NAMES =
        arrayOf("DISABLED", "LAN", "Excellent", "Good", "Average", "Low", "Bad")

    val STATUS_NAMES = arrayOf("Playing", "Idle", "Connecting")
  }
}

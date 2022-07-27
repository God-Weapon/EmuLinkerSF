package org.emulinker.kaillera.model

import java.net.InetSocketAddress
import kotlin.Throws
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.model.event.KailleraEventListener
import org.emulinker.kaillera.model.exception.*
import org.emulinker.kaillera.model.impl.KailleraGameImpl
import org.emulinker.kaillera.model.impl.KailleraUserImpl
import org.emulinker.kaillera.model.impl.Trivia
import org.emulinker.kaillera.release.ReleaseInfo

interface KailleraServer {
  val maxUsers: Int
  val maxGames: Int
  val maxPing: Int

  val releaseInfo: ReleaseInfo
  val accessManager: AccessManager
  val users: Collection<KailleraUserImpl>
  val games: Collection<KailleraGameImpl>
  var trivia: Trivia?
  var switchTrivia: Boolean

  fun announce(message: String, gamesAlso: Boolean)
  fun announce(message: String, gamesAlso: Boolean, targetUser: KailleraUserImpl?)
  fun getUser(userID: Int): KailleraUser?
  fun getGame(gameID: Int): KailleraGame?
  fun checkMe(user: KailleraUser, message: String): Boolean

  @Throws(ServerFullException::class, NewConnectionException::class)
  fun newConnection(
      clientSocketAddress: InetSocketAddress, protocol: String, listener: KailleraEventListener
  ): KailleraUser

  @Throws(
      PingTimeException::class,
      ClientAddressException::class,
      ConnectionTypeException::class,
      UserNameException::class,
      LoginException::class)
  suspend fun login(user: KailleraUser)

  @Throws(ChatException::class, FloodException::class)
  fun chat(user: KailleraUser, message: String)

  @Throws(CreateGameException::class, FloodException::class)
  suspend fun createGame(user: KailleraUser, romName: String?): KailleraGame?

  @Throws(
      QuitException::class,
      DropGameException::class,
      QuitGameException::class,
      CloseGameException::class)
  fun quit(user: KailleraUser, message: String?)
  suspend fun stop()
}

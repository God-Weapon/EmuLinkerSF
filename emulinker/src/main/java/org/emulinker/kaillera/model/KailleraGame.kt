package org.emulinker.kaillera.model

import kotlin.Throws
import org.emulinker.kaillera.model.exception.CloseGameException
import org.emulinker.kaillera.model.exception.DropGameException
import org.emulinker.kaillera.model.exception.GameChatException
import org.emulinker.kaillera.model.exception.GameDataException
import org.emulinker.kaillera.model.exception.GameKickException
import org.emulinker.kaillera.model.exception.JoinGameException
import org.emulinker.kaillera.model.exception.QuitGameException
import org.emulinker.kaillera.model.exception.StartGameException
import org.emulinker.kaillera.model.exception.UserReadyException
import org.emulinker.kaillera.model.impl.PlayerActionQueue

interface KailleraGame {
  val clientType: String?
  val highestPing: Int
  val id: Int
  val numPlayers: Int
  val owner: KailleraUser?
  val playerActionQueue: Array<PlayerActionQueue?>?
  val players: MutableList<KailleraUser?>
  val romName: String?
  val server: KailleraServer?
  val startTimeoutTime: Long
  val status: Int

  var delay: Int
  var maxPing: Int
  var maxUsers: Int
  var p2P: Boolean
  var sameDelay: Boolean
  var startN: Int
  var startTimeout: Boolean

  fun getPlayerNumber(user: KailleraUser?): Int

  fun getPlayer(playerNumber: Int): KailleraUser?

  fun droppedPacket(user: KailleraUser?)

  @Throws(JoinGameException::class)
  fun join(user: KailleraUser?): Int

  @Throws(GameChatException::class)
  fun chat(user: KailleraUser?, message: String?)

  @Throws(GameKickException::class)
  fun kick(requester: KailleraUser?, userID: Int)

  @Throws(StartGameException::class)
  fun start(user: KailleraUser?)

  @Throws(UserReadyException::class)
  fun ready(user: KailleraUser?, playerNumber: Int)

  @Throws(GameDataException::class)
  fun addData(user: KailleraUser?, playerNumber: Int, data: ByteArray?)

  @Throws(DropGameException::class)
  fun drop(user: KailleraUser, playerNumber: Int)

  @Throws(DropGameException::class, QuitGameException::class, CloseGameException::class)
  fun quit(user: KailleraUser, playerNumber: Int)

  companion object {
    const val STATUS_WAITING: Byte = 0
    const val STATUS_PLAYING: Byte = 2
    const val STATUS_SYNCHRONIZING: Byte = 1

    val STATUS_NAMES = arrayOf("Waiting", "Playing", "Synchronizing")
  }
}

package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import java.lang.IndexOutOfBoundsException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Throws
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.CachedGameData
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification
import org.emulinker.kaillera.controller.v086.protocol.GameData.Companion.create
import org.emulinker.kaillera.model.exception.GameDataException

private val logger = FluentLogger.forEnclosingClass()

private const val DESC = "CachedGameDataAction"

@Singleton
class CachedGameDataAction @Inject internal constructor() : V086Action<CachedGameData> {
  override val actionPerformedCount = 0
  override fun toString() = DESC

  @Throws(FatalActionException::class)
  override fun performAction(message: CachedGameData, clientHandler: V086ClientHandler) {
    try {
      val user = clientHandler.user
      val data = clientHandler.clientGameDataCache[message.key]
      if (data == null) {
        logger.atFine().log("Game Cache Error: null data")
        return
      }
      user!!.addGameData(data)
    } catch (e: GameDataException) {
      logger.atFine().withCause(e).log("Game data error")
      if (e.response != null) {
        try {
          clientHandler.send(create(clientHandler.nextMessageNumber, e.response!!))
        } catch (e2: MessageFormatException) {
          logger.atSevere().withCause(e2).log("Failed to construct GameData message")
        }
      }
    } catch (e: IndexOutOfBoundsException) {
      logger
          .atSevere()
          .withCause(e)
          .log(
              "Game data error!  The client cached key " +
                  message.key +
                  " was not found in the cache!")

      // This may not always be the best thing to do...
      try {
        clientHandler.send(
            GameChat_Notification(
                clientHandler.nextMessageNumber,
                "Error",
                "Game Data Error!  Game state will be inconsistent!"))
      } catch (e2: MessageFormatException) {
        logger.atSevere().withCause(e2).log("Failed to construct new GameChat_Notification")
      }
    }
  }
}

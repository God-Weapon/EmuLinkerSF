package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.CachedGameData
import org.emulinker.kaillera.controller.v086.protocol.GameData
import org.emulinker.kaillera.model.event.GameDataEvent
import org.emulinker.kaillera.model.exception.GameDataException

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class GameDataAction @Inject internal constructor() :
    V086Action<GameData>, V086GameEventHandler<GameDataEvent> {
  override val actionPerformedCount = 0
  override val handledEventCount = 0

  override fun toString() = "GameDataAction"

  @Throws(FatalActionException::class)
  override suspend fun performAction(message: GameData, clientHandler: V086ClientHandler) {
    try {
      val user = clientHandler.user
      val data = message.gameData
      clientHandler.clientGameDataCache.add(data)
      user.addGameData(data)
    } catch (e: GameDataException) {
      logger.atFine().withCause(e).log("Game data error")
      if (e.response != null) {
        try {
          clientHandler.send(GameData.create(clientHandler.nextMessageNumber, e.response!!))
        } catch (e2: MessageFormatException) {
          logger.atSevere().withCause(e2).log("Failed to construct GameData message")
        }
      }
    }
  }

  override suspend fun handleEvent(event: GameDataEvent, clientHandler: V086ClientHandler) {
    val data = event.data
    val key = clientHandler.serverGameDataCache.indexOf(data)
    if (key < 0) {
      clientHandler.serverGameDataCache.add(data)
      try {
        clientHandler.send(GameData.create(clientHandler.nextMessageNumber, data))
      } catch (e: MessageFormatException) {
        logger.atSevere().withCause(e).log("Failed to construct GameData message")
      }
    } else {
      try {
        clientHandler.send(CachedGameData(clientHandler.nextMessageNumber, key))
      } catch (e: MessageFormatException) {
        logger.atSevere().withCause(e).log("Failed to construct CachedGameData message")
      }
    }
  }
}

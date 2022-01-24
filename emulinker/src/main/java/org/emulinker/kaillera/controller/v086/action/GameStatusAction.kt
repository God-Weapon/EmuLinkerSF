package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.GameStatus
import org.emulinker.kaillera.model.event.GameStatusChangedEvent

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class GameStatusAction @Inject internal constructor() :
    V086ServerEventHandler<GameStatusChangedEvent> {
  override var handledEventCount = 0
    private set

  override fun toString() = DESC

  override fun handleEvent(event: GameStatusChangedEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    try {
      val game = event.game
      var num = 0
      for (user in game.players) {
        if (!user.stealth) num++
      }
      clientHandler.send(
          GameStatus(
              clientHandler.nextMessageNumber,
              game.id,
              0.toShort().toInt(),
              game.status.byteValue,
              num.toByte(),
              game.maxUsers.toByte()))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct CreateGame_Notification message")
    }
  }

  companion object {
    private const val DESC = "GameStatusAction"
  }
}

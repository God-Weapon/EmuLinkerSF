package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.CloseGame
import org.emulinker.kaillera.model.event.GameClosedEvent

@Singleton
class CloseGameAction @Inject internal constructor() : V086ServerEventHandler<GameClosedEvent> {
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  override fun handleEvent(gameClosedEvent: GameClosedEvent, clientHandler: V086ClientHandler?) {
    handledEventCount++
    try {
      clientHandler!!.send(CloseGame(clientHandler.nextMessageNumber, gameClosedEvent.game.id, 0))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to contruct CloseGame_Notification message")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val DESC = "CloseGameAction"
  }
}

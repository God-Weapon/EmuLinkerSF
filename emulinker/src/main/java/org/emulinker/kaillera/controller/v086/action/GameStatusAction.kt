package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.GameStatus
import org.emulinker.kaillera.model.event.GameStatusChangedEvent

@Singleton
class GameStatusAction @Inject internal constructor() :
    V086ServerEventHandler<GameStatusChangedEvent> {
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  override fun handleEvent(
      statusChangeEvent: GameStatusChangedEvent, clientHandler: V086ClientHandler?
  ) {
    handledEventCount++
    try {
      val game = statusChangeEvent.game
      var num = 0
      for (user in game.players) {
        if (!user!!.stealth) num++
      }
      clientHandler!!.send(
          GameStatus(
              clientHandler.nextMessageNumber,
              game.id,
              0.toShort().toInt(),
              game.status.toByte(),
              num.toByte(),
              game.maxUsers.toByte()))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to contruct CreateGame_Notification message")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val DESC = "GameStatusAction"
  }
}

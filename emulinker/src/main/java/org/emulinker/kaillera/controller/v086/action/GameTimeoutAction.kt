package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.model.event.GameTimeoutEvent

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class GameTimeoutAction @Inject internal constructor() : V086GameEventHandler<GameTimeoutEvent> {
  override var handledEventCount = 0
    private set

  override fun toString() = DESC

  override fun handleEvent(event: GameTimeoutEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    val player = event.user
    val user = clientHandler.user
    if (player == user) {
      logger
          .atFine()
          .log(
              "$user received timeout event ${event.timeoutNumber} for ${event.game}: resending messages...")
      clientHandler.resend(event.timeoutNumber)
    } else {
      logger
          .atFine()
          .log(
              "${user.toString()} received timeout event ${event.timeoutNumber} from $player for ${event.game}")
    }
  }

  companion object {
    private const val DESC = "GameTimeoutAction"
  }
}

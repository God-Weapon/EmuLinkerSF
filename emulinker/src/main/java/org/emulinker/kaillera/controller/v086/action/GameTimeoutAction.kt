package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.model.event.GameTimeoutEvent

@Singleton
class GameTimeoutAction @Inject internal constructor() : V086GameEventHandler<GameTimeoutEvent> {
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  override fun handleEvent(timeoutEvent: GameTimeoutEvent, clientHandler: V086ClientHandler?) {
    handledEventCount++
    val player = timeoutEvent.user
    val user = clientHandler!!.user
    if (player == user) {
      logger
          .atFine()
          .log(
              user.toString() +
                  " received timeout event " +
                  timeoutEvent.timeoutNumber +
                  " for " +
                  timeoutEvent.game +
                  ": resending messages...")
      clientHandler.resend(timeoutEvent.timeoutNumber)
    } else {
      logger
          .atFine()
          .log(
              user.toString() +
                  " received timeout event " +
                  timeoutEvent.timeoutNumber +
                  " from " +
                  player +
                  " for " +
                  timeoutEvent.game)
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val DESC = "GameTimeoutAction"
  }
}

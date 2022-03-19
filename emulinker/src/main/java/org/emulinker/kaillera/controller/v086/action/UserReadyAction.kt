package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.AllReady
import org.emulinker.kaillera.model.event.GameEvent
import org.emulinker.kaillera.model.exception.UserReadyException

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class UserReadyAction @Inject internal constructor() :
    V086Action<AllReady>, V086GameEventHandler<GameEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString() = "UserReadyAction"

  @Throws(FatalActionException::class)
  override fun performAction(message: AllReady, clientHandler: V086ClientHandler) {
    actionPerformedCount++
    try {
      clientHandler.user!!.playerReady()
    } catch (e: UserReadyException) {
      logger.atFine().withCause(e).log("Ready signal failed")
    }
  }

  override fun handleEvent(event: GameEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    clientHandler.resetGameDataCache()
    try {
      clientHandler.send(AllReady(clientHandler.nextMessageNumber))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct AllReady message")
    }
  }
}

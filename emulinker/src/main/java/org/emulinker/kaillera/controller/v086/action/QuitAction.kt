package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.Quit_Notification
import org.emulinker.kaillera.controller.v086.protocol.Quit_Request
import org.emulinker.kaillera.model.event.UserQuitEvent
import org.emulinker.kaillera.model.exception.ActionException

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class QuitAction @Inject internal constructor() :
    V086Action<Quit_Request>, V086ServerEventHandler<UserQuitEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  @Throws(FatalActionException::class)
  override fun performAction(message: Quit_Request, clientHandler: V086ClientHandler) {
    actionPerformedCount++
    try {
      clientHandler.user!!.quit(message.message)
    } catch (e: ActionException) {
      throw FatalActionException("Failed to quit: " + e.message)
    }
  }

  override fun handleEvent(event: UserQuitEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    try {
      val user = event.user
      clientHandler.send(
          Quit_Notification(clientHandler.nextMessageNumber, user.name!!, user.id, event.message))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct Quit_Notification message")
    }
  }

  companion object {
    private const val DESC = "QuitAction"
  }
}

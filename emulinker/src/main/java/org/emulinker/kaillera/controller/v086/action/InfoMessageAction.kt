package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage
import org.emulinker.kaillera.model.event.InfoMessageEvent

@Singleton
class InfoMessageAction @Inject internal constructor() : V086UserEventHandler<InfoMessageEvent> {
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  override fun handleEvent(infoEvent: InfoMessageEvent, clientHandler: V086ClientHandler?) {
    handledEventCount++
    try {
      clientHandler!!.send(
          InformationMessage(clientHandler.nextMessageNumber, "server", infoEvent.message))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to contruct InformationMessage message")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val DESC = "InfoMessageAction"
  }
}

package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage
import org.emulinker.kaillera.model.event.InfoMessageEvent

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class InfoMessageAction @Inject internal constructor() : V086UserEventHandler<InfoMessageEvent> {
  override var handledEventCount = 0
    private set

  override fun toString() = "InfoMessageAction"

  override suspend fun handleEvent(event: InfoMessageEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    try {
      clientHandler.send(
          InformationMessage(clientHandler.nextMessageNumber, "server", event.message))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct InformationMessage message")
    }
  }
}

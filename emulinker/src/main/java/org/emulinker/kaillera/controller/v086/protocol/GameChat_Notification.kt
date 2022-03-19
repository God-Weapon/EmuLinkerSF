package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class GameChat_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, override val username: String, override val message: String
    ) : GameChat() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
  }
}

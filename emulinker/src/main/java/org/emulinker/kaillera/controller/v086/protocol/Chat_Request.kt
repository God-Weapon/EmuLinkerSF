package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class Chat_Request
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, override val message: String, override val username: String
    ) : Chat() {

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  companion object {
    private const val DESC = "Chat Request"
  }
}

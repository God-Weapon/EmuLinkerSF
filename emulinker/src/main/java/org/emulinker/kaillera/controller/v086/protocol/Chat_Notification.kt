package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class Chat_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, override val username: String, override val message: String
    ) : Chat() {

  override val description = DESC

  override val messageId = ID

  companion object {
    private const val DESC = "Chat Notification"
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class GameChat_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, override val username: String, override val message: String
    ) : GameChat() {

  override val shortName = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  companion object {
    private const val DESC = "In-Game Chat Notification"
  }
}

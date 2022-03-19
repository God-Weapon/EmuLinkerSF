package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class Quit_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val username: String,
        override val userId: Int,
        override val message: String
    ) : Quit() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
    require(userId in 0..0xFFFF) { "UserID out of acceptable range: $userId" }
    require(username.isNotBlank()) { "Username cannot be empty" }
  }
}

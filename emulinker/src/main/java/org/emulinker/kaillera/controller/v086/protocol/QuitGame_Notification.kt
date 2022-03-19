package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class QuitGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, override val username: String, override val userId: Int
    ) : QuitGame() {

  init {
    validateMessageNumber(messageNumber)
    require(userId in 0..0xFFFF) { "UserID out of acceptable range: $userId" }
  }
}

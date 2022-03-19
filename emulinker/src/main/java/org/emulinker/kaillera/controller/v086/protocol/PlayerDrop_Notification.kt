package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class PlayerDrop_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val username: String,
        override val playerNumber: Byte
    ) : PlayerDrop() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
    require(playerNumber in 0..255) { "playerNumber out of acceptable range: $playerNumber" }
    require(username.isNotBlank()) { "Username cannot be blank" }
  }
}

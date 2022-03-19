package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class PlayerDrop_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : PlayerDrop() {

  override val messageId = ID

  override val username = ""
  override val playerNumber = 0.toByte()

  init {
    validateMessageNumber(messageNumber)
  }
}

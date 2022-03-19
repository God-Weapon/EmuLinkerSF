package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class Quit_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, override val message: String) : Quit() {

  override val username = ""
  override val userId = 0xFFFF

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
  }
}

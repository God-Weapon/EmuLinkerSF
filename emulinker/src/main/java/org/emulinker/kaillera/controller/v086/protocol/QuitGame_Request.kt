package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class QuitGame_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : QuitGame() {

  override val username = ""
  override val userId = 0xFFFF

  init {
    validateMessageNumber(messageNumber)
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class CreateGame_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, override val romName: String) : CreateGame() {

  override val messageId = ID

  override val username = ""
  override val clientType = ""
  override val gameId = 0xFFFF
  override val val1 = 0xFFFF

  init {
    validateMessageNumber(messageNumber)
  }
}

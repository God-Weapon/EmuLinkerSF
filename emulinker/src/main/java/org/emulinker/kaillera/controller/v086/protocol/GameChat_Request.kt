package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

private const val DESC = "In-Game Chat Request"

data class GameChat_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, override val message: String) : GameChat() {

  override val shortName = DESC
  override val messageId = ID
  override val username = ""

  init {
    validateMessageNumber(messageNumber, DESC)
  }
}

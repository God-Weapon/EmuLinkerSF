package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class PlayerDrop_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : PlayerDrop() {

  override val description = DESC
  override val messageId = ID

  override val username = ""
  override val playerNumber = 0.toByte()

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  companion object {
    private const val DESC = "Player Drop Request"
  }
}

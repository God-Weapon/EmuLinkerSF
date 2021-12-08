package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class QuitGame_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : QuitGame() {

  override val username = ""
  override val userId = 0xFFFF

  override val shortName = DESC

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  companion object {
    private const val DESC = "Quit Game Request"
  }
}

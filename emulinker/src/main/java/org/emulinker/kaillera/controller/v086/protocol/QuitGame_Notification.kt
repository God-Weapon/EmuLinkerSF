package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class QuitGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, override val username: String, override val userId: Int
    ) : QuitGame() {

  override val description = DESC

  init {
    validateMessageNumber(messageNumber, DESC)
    if (userId < 0 || userId > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: userID out of acceptable range: $userId")
    }
  }

  companion object {
    private const val DESC = "Quit Game Notification"
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class PlayerDrop_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val username: String?,
        override val playerNumber: Byte
    ) : PlayerDrop() {

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (playerNumber < 0 || playerNumber > 255) {
      throw MessageFormatException(
          "Invalid $DESC format: playerNumber out of acceptable range: $playerNumber")
    }
    if (Strings.isNullOrEmpty(username)) {
      throw MessageFormatException("Invalid $DESC format: userName.length == 0")
    }
  }

  companion object {
    private const val DESC = "Player Drop Notification"
  }
}

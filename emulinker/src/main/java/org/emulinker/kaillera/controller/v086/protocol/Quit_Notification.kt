package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class Quit_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val username: String,
        override val userId: Int,
        override val message: String
    ) : Quit() {

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (userId < 0 || userId > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: userID out of acceptable range: $userId")
    }
    if (message == null) throw MessageFormatException("Invalid $DESC format: message == null!")
    if (Strings.isNullOrEmpty(username)) {
      throw MessageFormatException("Invalid $DESC format: userName.length == 0, (userID = $userId)")
    }
  }

  companion object {
    private const val DESC = "User Quit Notification"
  }
}

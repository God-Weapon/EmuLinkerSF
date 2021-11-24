package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class JoinGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val gameId: Int,
        override val val1: Int,
        override val username: String?,
        override val ping: Long,
        override val userId: Int,
        override val connectionType: Byte
    ) : JoinGame() {

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (gameId < 0 || gameId > 0xFFFF) {
      throw MessageFormatException(
          "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId)
    }
    if (ping < 0 || ping > 0xFFFF) {
      throw MessageFormatException(
          "Invalid " + DESC + " format: ping out of acceptable range: " + ping)
    }
    if (userId < 0 || userId > 0xFFFF) {
      throw MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId)
    }
    if (connectionType < 1 || connectionType > 6) {
      throw MessageFormatException(
          "Invalid " + DESC + " format: connectionType out of acceptable range: " + connectionType)
    }
    if (Strings.isNullOrEmpty(username)) {
      throw MessageFormatException("Invalid " + DESC + " format: Strings.isNullOrEmpty(userName)")
    }
  }

  companion object {
    private const val DESC = "Join Game Notification"
  }
}

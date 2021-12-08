package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class CreateGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val username: String,
        override val romName: String,
        override val clientType: String,
        override val gameId: Int,
        override val val1: Int
    ) : CreateGame() {

  override val messageId = ID
  override val shortName = DESC

  init {
    validateMessageNumber(messageNumber, DESC)
    if (Strings.isNullOrEmpty(romName)) {
      throw MessageFormatException("Invalid $DESC format: romName.length == 0")
    }
    if (gameId < 0 || gameId > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: gameID out of acceptable range: $gameId")
    }
    if (val1 != 0x0000 && val1 != 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: val1 out of acceptable range: $val1")
    }
  }

  companion object {
    private const val DESC = "Create Game Notification"
  }
}

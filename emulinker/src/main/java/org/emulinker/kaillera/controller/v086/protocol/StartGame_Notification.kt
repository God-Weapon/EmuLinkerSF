package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class StartGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val val1: Int,
        override val playerNumber: Short,
        override val numPlayers: Short
    ) : StartGame() {

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (val1 < 0 || val1 > 0xFFFF)
        throw MessageFormatException("Invalid $DESC format: val1 out of acceptable range: $val1")
    if (playerNumber < 0 || playerNumber > 0xFF)
        throw MessageFormatException(
            "Invalid $DESC format: playerNumber out of acceptable range: $playerNumber")
    if (numPlayers < 0 || numPlayers > 0xFF)
        throw MessageFormatException(
            "Invalid $DESC format: numPlayers out of acceptable range: $numPlayers")
  }

  companion object {
    private const val DESC = "Start Game Notification"
  }
}

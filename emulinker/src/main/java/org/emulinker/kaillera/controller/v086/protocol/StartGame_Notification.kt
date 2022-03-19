package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

data class StartGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val val1: Int,
        override val playerNumber: Short,
        override val numPlayers: Short
    ) : StartGame() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
    require(val1 in 0..0xFFFF) { "val1 out of acceptable range: $val1" }
    require(playerNumber in 0..0xFF) { "playerNumber out of acceptable range: $playerNumber" }
    require(numPlayers in 0..0xFF) { "numPlayers out of acceptable range: $numPlayers" }
  }
}

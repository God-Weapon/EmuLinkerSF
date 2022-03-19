package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber

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

  init {
    validateMessageNumber(messageNumber)
    require(romName.isNotBlank()) { "romName cannot be blank" }
    require(gameId in 0..0xFFFF) { "gameID out of acceptable range: $gameId" }
    require(val1 in 0..0xFFFF) { "val1 out of acceptable range: $val1" }
  }
}

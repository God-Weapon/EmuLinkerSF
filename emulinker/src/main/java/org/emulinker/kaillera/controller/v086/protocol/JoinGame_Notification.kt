package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber
import org.emulinker.kaillera.model.ConnectionType

data class JoinGame_Notification
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val gameId: Int,
        override val val1: Int,
        override val username: String,
        override val ping: Long,
        override val userId: Int,
        override val connectionType: ConnectionType
    ) : JoinGame() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
    require(gameId in 0..0xFFFF) { "gameID out of acceptable range: $gameId" }
    require(ping in 0..0xFFFF) { "ping out of acceptable range: $ping" }
    require(userId in 0..0xFFFF) { "UserID out of acceptable range: $userId" }
    require(username.isNotBlank()) { "Username cannot be empty" }
  }
}

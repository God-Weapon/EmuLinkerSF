package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.model.ConnectionType

data class JoinGame_Request
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val gameId: Int,
        override val connectionType: ConnectionType
    ) : JoinGame() {

  override val messageId = ID

  override val val1 = 0
  override val username = ""
  override val ping = 0L
  override val userId = 0xFFFF

  init {
    validateMessageNumber(messageNumber)
    require(gameId in 0..0xFFFF) { "gameID out of acceptable range: $gameId" }
  }
}

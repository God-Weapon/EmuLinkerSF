package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.model.ConnectionType

private const val DESC = "Join Game Request"

data class JoinGame_Request
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        override val gameId: Int,
        override val connectionType: ConnectionType
    ) : JoinGame() {

  override val shortName = DESC
  override val messageId = ID

  override val val1 = 0
  override val username = ""
  override val ping = 0L
  override val userId = 0xFFFF

  init {
    validateMessageNumber(messageNumber, DESC)
    if (gameId < 0 || gameId > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: gameID out of acceptable range: $gameId")
    }
  }
}

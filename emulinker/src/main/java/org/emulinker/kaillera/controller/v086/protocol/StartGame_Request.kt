package org.emulinker.kaillera.controller.v086.protocol

import org.emulinker.kaillera.controller.messaging.MessageFormatException

data class StartGame_Request
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : StartGame() {

  override val val1 = 0xFFFF
  override val playerNumber = 0xFF.toShort()
  override val numPlayers = 0xFF.toShort()

  override val description = DESC

  companion object {
    private const val DESC = "Start Game Request"
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class GameStatus
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        val gameId: Int,
        val val1: Int,
        val gameStatus: Byte,
        val numPlayers: Byte,
        val maxPlayers: Byte
    ) : V086Message() {

  override val shortName = DESC
  override val messageId = ID

  override val bodyLength = 8

  init {
    validateMessageNumber(messageNumber, DESC)
    if (gameId < 0 || gameId > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: gameID out of acceptable range: $gameId")
    }
    if (val1 < 0 || val1 > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: val1 out of acceptable range: $val1")
    }
    if (gameStatus < 0 || gameStatus > 2) {
      throw MessageFormatException(
          "Invalid $DESC format: gameStatus out of acceptable range: $gameStatus")
    }
    if (numPlayers < 0 || numPlayers > 0xFF) {
      throw MessageFormatException(
          "Invalid $DESC format: numPlayers out of acceptable range: $numPlayers")
    }
    if (maxPlayers < 0 || maxPlayers > 0xFF) {
      throw MessageFormatException(
          "Invalid $DESC format: maxPlayers out of acceptable range: $maxPlayers")
    }
  }

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedShort(buffer, gameId)
    UnsignedUtil.putUnsignedShort(buffer, val1)
    buffer.put(gameStatus)
    buffer.put(numPlayers)
    buffer.put(maxPlayers)
  }

  companion object {
    const val ID: Byte = 0x0E
    private const val DESC = "Game Status"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): GameStatus {
      if (buffer.remaining() < 8) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      if (b.toInt() != 0x00)
          throw MessageFormatException(
              "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b))
      val gameID = UnsignedUtil.getUnsignedShort(buffer)
      val val1 = UnsignedUtil.getUnsignedShort(buffer)
      val gameStatus = buffer.get()
      val numPlayers = buffer.get()
      val maxPlayers = buffer.get()
      return GameStatus(messageNumber, gameID, val1, gameStatus, numPlayers, maxPlayers)
    }
  }
}

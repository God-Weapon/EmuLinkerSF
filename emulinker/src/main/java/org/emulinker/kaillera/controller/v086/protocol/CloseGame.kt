package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class CloseGame
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val gameId: Int, val val1: Int) : V086Message() {

  override val messageId = ID

  override val bodyLength = 5

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedShort(buffer, gameId)
    UnsignedUtil.putUnsignedShort(buffer, val1)
  }

  init {
    validateMessageNumber(messageNumber)
    require(gameId in 0..0xFFFF) { "gameID out of acceptable range: $gameId" }
    require(val1 in 0..0xFFFF) { "val1 out of acceptable range: $val1" }
  }

  companion object {
    const val ID: Byte = 0x10

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): CloseGame {
      if (buffer.remaining() < 5) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      if (b.toInt() != 0x00)
          throw MessageFormatException(
              "Invalid Close Game format: byte 0 = " + EmuUtil.byteToHex(b))
      val gameID = UnsignedUtil.getUnsignedShort(buffer)
      val val1 = UnsignedUtil.getUnsignedShort(buffer)
      return CloseGame(messageNumber, gameID, val1)
    }
  }
}

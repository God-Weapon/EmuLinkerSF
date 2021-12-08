package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class ServerACK
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : ACK() {

  override val val1 = 0L
  override val val2 = 1L
  override val val3 = 2L
  override val val4 = 3L

  override val shortName = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  companion object {
    const val ID: Byte = 0x05
    private const val DESC = "Server to Client ACK"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): ServerACK {
      if (buffer.remaining() < 17) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      if (b.toInt() != 0x00)
          throw MessageFormatException(
              "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b))
      val val1 = UnsignedUtil.getUnsignedInt(buffer)
      val val2 = UnsignedUtil.getUnsignedInt(buffer)
      val val3 = UnsignedUtil.getUnsignedInt(buffer)
      val val4 = UnsignedUtil.getUnsignedInt(buffer)
      if (val1 != 0L || val2 != 1L || val3 != 2L || val4 != 3L)
          throw MessageFormatException(
              "Invalid $DESC format: bytes do not match acceptable format!")
      return ServerACK(messageNumber)
    }
  }
}

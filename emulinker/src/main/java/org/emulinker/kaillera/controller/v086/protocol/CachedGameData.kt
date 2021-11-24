package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.UnsignedUtil

data class CachedGameData
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val key: Int) : V086Message() {
  override val description = DESC
  override val messageId = ID

  override val bodyLength: Int
    get() = 2

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedByte(buffer, key)
  }

  companion object {
    const val ID: Byte = 0x13
    private const val DESC = "Cached Game Data"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): CachedGameData {
      if (buffer.remaining() < 2) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      // removed to increase speed
      // if (b != 0x00)
      // throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " +
      // EmuUtil.byteToHex(b));
      return CachedGameData(messageNumber, UnsignedUtil.getUnsignedByte(buffer).toInt())
    }
  }
}

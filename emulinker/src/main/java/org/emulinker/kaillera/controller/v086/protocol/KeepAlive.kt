package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.UnsignedUtil

data class KeepAlive
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val value: Short) : V086Message() {

  override val messageId = ID
  override val bodyLength = 1

  init {
    validateMessageNumber(messageNumber)
    require(value in 0..0xFF) { "val out of acceptable range: $value" }
  }

  public override fun writeBodyTo(buffer: ByteBuffer) {
    UnsignedUtil.putUnsignedByte(buffer, value.toInt())
  }

  companion object {
    const val ID: Byte = 0x09

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): KeepAlive {
      if (buffer.remaining() < 1) throw ParseException("Failed byte count validation!")
      return KeepAlive(messageNumber, UnsignedUtil.getUnsignedByte(buffer))
    }
  }
}

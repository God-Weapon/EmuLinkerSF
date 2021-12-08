package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.UnsignedUtil

data class KeepAlive
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val `val`: Short) : V086Message() {

  override val shortName = DESC
  override val messageId = ID
  override val bodyLength = 1

  init {
    validateMessageNumber(messageNumber, DESC)
    if (`val` < 0 || `val` > 0xFF) {
      throw MessageFormatException("Invalid $DESC format: val out of acceptable range: $`val`")
    }
  }

  public override fun writeBodyTo(buffer: ByteBuffer) {
    UnsignedUtil.putUnsignedByte(buffer, `val`.toInt())
  }

  companion object {
    const val ID: Byte = 0x09
    private const val DESC = "KeepAlive"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): KeepAlive {
      if (buffer.remaining() < 1) throw ParseException("Failed byte count validation!")
      return KeepAlive(messageNumber, UnsignedUtil.getUnsignedByte(buffer))
    }
  }
}

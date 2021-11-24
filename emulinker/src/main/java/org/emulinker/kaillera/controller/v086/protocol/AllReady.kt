package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.EmuUtil

data class AllReady
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int) : V086Message() {
  override val bodyLength = 1

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
  }

  companion object {
    const val ID: Byte = 0x15
    private const val DESC = "All Ready Signal"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): AllReady {
      if (buffer.remaining() < 1) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      if (b.toInt() != 0x00)
          throw MessageFormatException(
              "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b))
      return AllReady(messageNumber)
    }
  }
}

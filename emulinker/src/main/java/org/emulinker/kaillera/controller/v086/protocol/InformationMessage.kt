package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil

data class InformationMessage
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val source: String, val message: String) :
    V086Message() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
    require(source.isNotBlank()) { "source cannot be blank" }
    require(message.isNotBlank()) { "message cannot be blank" }
  }

  override val bodyLength: Int
    get() = getNumBytes(source) + getNumBytes(message) + 2

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, source, 0x00, AppModule.charsetDoNotUse)
    EmuUtil.writeString(buffer, message, 0x00, AppModule.charsetDoNotUse)
  }

  companion object {
    const val ID: Byte = 0x17

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): InformationMessage {
      if (buffer.remaining() < 4) throw ParseException("Failed byte count validation!")
      val source = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 2) throw ParseException("Failed byte count validation!")
      val message = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      return InformationMessage(messageNumber, source, message)
    }
  }
}

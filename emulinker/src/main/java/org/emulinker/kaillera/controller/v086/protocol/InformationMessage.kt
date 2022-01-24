package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil

data class InformationMessage
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val source: String, val message: String) :
    V086Message() {

  override val shortName = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (Strings.isNullOrEmpty(source)) {
      throw MessageFormatException("Invalid $DESC format: source.length == 0")
    }
    if (Strings.isNullOrEmpty(message)) {
      throw MessageFormatException("Invalid $DESC format: message.length == 0")
    }
  }

  override val bodyLength: Int
    get() = getNumBytes(source) + getNumBytes(message) + 2

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, source, 0x00, AppModule.charsetDoNotUse)
    EmuUtil.writeString(buffer, message, 0x00, AppModule.charsetDoNotUse)
  }

  companion object {
    const val ID: Byte = 0x17
    private const val DESC = "Information Message"

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

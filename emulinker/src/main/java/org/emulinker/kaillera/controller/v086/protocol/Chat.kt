package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil

abstract class Chat
    @Throws(MessageFormatException::class)
    constructor() : V086Message() {
  abstract val username: String
  abstract val message: String

  override val bodyLength: Int
    get() = getNumBytes(username) + getNumBytes(message) + 2

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    EmuUtil.writeString(buffer, message, 0x00, AppModule.charsetDoNotUse)
  }

  companion object {
    const val ID: Byte = 0x07
    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): Chat {
      if (buffer.remaining() < 3) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 2) throw ParseException("Failed byte count validation!")
      val message = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      return if (Strings.isNullOrEmpty(userName)) {
        Chat_Request(messageNumber, message, username = "")
      } else {
        Chat_Notification(messageNumber, userName, message)
      }
    }
  }
}

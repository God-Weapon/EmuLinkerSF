package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class ConnectionRejected
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, val username: String, val userId: Int, val message: String
    ) : V086Message() {

  override val shortName = DESC
  override val messageId = ID

  override val bodyLength: Int
    get() = getNumBytes(username) + getNumBytes(message) + 4

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedShort(buffer, userId)
    EmuUtil.writeString(buffer, message, 0x00, AppModule.charsetDoNotUse)
  }

  init {
    validateMessageNumber(messageNumber, DESC)
    if (Strings.isNullOrEmpty(username)) {
      throw MessageFormatException("Invalid $DESC format: userName.length == 0")
    }
    if (userId < 0 || userId > 0xFFFF) {
      throw MessageFormatException("Invalid $DESC format: userID out of acceptable range: $userId")
    }
    if (Strings.isNullOrEmpty(message)) {
      throw MessageFormatException("Invalid $DESC format: message.length == 0")
    }
  }

  companion object {
    const val ID: Byte = 0x16
    private const val DESC = "Connection Rejected"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): ConnectionRejected {
      if (buffer.remaining() < 6) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 4) throw ParseException("Failed byte count validation!")
      val userID = UnsignedUtil.getUnsignedShort(buffer)
      if (buffer.remaining() < 2) throw ParseException("Failed byte count validation!")
      val message = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      return ConnectionRejected(messageNumber, userName, userID, message)
    }
  }
}

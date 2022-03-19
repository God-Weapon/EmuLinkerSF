package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class ConnectionRejected
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int, val username: String, val userId: Int, val message: String
    ) : V086Message() {

  override val messageId = ID

  override val bodyLength: Int
    get() = getNumBytes(username) + getNumBytes(message) + 4

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedShort(buffer, userId)
    EmuUtil.writeString(buffer, message, 0x00, AppModule.charsetDoNotUse)
  }

  init {
    validateMessageNumber(messageNumber)
    require(username.isNotBlank()) { "Username cannot be empty" }
    require(userId in 0..0xFFFF) { "UserID out of acceptable range: $userId" }
    require(message.isNotBlank()) { "Message cannot be empty" }
  }

  companion object {
    const val ID: Byte = 0x16

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

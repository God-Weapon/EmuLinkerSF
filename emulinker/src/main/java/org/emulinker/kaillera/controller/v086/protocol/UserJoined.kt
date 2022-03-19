package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.validateMessageNumber
import org.emulinker.kaillera.model.ConnectionType
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class UserJoined
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        val username: String,
        val userId: Int,
        val ping: Long,
        val connectionType: ConnectionType
    ) : V086Message() {

  override val messageId = ID

  init {
    validateMessageNumber(messageNumber)
    if (username.isBlank()) throw MessageFormatException("Empty username: $username")
    require(userId in 0..65535) { "UserID out of acceptable range: $userId" }
    require(ping in 0..2048) { "Ping out of acceptable range: $ping" }
  }

  override val bodyLength = getNumBytes(username) + 8

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedShort(buffer, userId)
    UnsignedUtil.putUnsignedInt(buffer, ping)
    buffer.put(connectionType.byteValue)
  }

  companion object {
    const val ID: Byte = 0x02

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): UserJoined {
      if (buffer.remaining() < 9) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 7) throw ParseException("Failed byte count validation!")
      val userID = UnsignedUtil.getUnsignedShort(buffer)
      val ping = UnsignedUtil.getUnsignedInt(buffer)
      val connectionType = buffer.get()
      return UserJoined(
          messageNumber, userName, userID, ping, ConnectionType.fromByteValue(connectionType))
    }
  }
}

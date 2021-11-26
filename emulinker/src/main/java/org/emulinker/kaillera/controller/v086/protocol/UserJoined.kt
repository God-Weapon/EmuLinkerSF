package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.model.KailleraUser.Companion.CONNECTION_TYPE_NAMES
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
        val connectionType: Byte
    ) : V086Message() {

  override val description = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (Strings.isNullOrEmpty(username))
        throw MessageFormatException(
            "Invalid $DESC format: userName.length == 0, (userID = $userId)")
    if (userId < 0 || userId > 65535)
        throw MessageFormatException(
            "Invalid $DESC format: userID out of acceptable range: $userId")
    if (ping < 0 || ping > 2048)
        throw MessageFormatException("Invalid $DESC format: ping out of acceptable range: $ping")
    if (connectionType < 1 || connectionType > 6)
        throw MessageFormatException(
            "Invalid $DESC format: connectionType out of acceptable range: $connectionType")
  }

  // TODO(nue): Get rid of this.
  override fun toString(): String {
    return "$infoString[userName=$username userID=$userId ping=$ping connectionType=${CONNECTION_TYPE_NAMES[connectionType.toInt()]}]"
  }

  override val bodyLength = getNumBytes(username) + 8

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedShort(buffer, userId)
    UnsignedUtil.putUnsignedInt(buffer, ping)
    buffer.put(connectionType)
  }

  companion object {
    const val ID: Byte = 0x02
    private const val DESC = "User Joined"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): UserJoined {
      if (buffer.remaining() < 9) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 7) throw ParseException("Failed byte count validation!")
      val userID = UnsignedUtil.getUnsignedShort(buffer)
      val ping = UnsignedUtil.getUnsignedInt(buffer)
      val connectionType = buffer.get()
      return UserJoined(messageNumber, userName, userID, ping, connectionType)
    }
  }
}

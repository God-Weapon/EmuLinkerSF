package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.model.KailleraUser.Companion.CONNECTION_TYPE_NAMES
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil

data class UserInformation
    @Throws(MessageFormatException::class)
    constructor(
        override val messageNumber: Int,
        val username: String,
        val clientType: String,
        val connectionType: Byte
    ) : V086Message() {

  override val shortName = DESC
  override val messageId = ID

  init {
    validateMessageNumber(messageNumber, DESC)
    if (connectionType < 1 || connectionType > 6) {
      throw MessageFormatException(
          "Invalid $DESC format: connectionType out of acceptable range: $connectionType")
    }
  }

  override val bodyLength: Int
    get() = getNumBytes(username) + getNumBytes(clientType) + 3

  // TODO(nue): Get rid of this.
  override fun toString(): String {
    return (infoString +
        "[userName=" +
        username +
        " clientType=" +
        clientType +
        " connectionType=" +
        CONNECTION_TYPE_NAMES[connectionType.toInt()] +
        "]")
  }

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    EmuUtil.writeString(buffer, clientType, 0x00, AppModule.charsetDoNotUse)
    buffer.put(connectionType)
  }

  companion object {
    const val ID: Byte = 0x03
    private const val DESC = "User Information"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): UserInformation {
      if (buffer.remaining() < 5) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 3) throw ParseException("Failed byte count validation!")
      val clientType = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 1) throw ParseException("Failed byte count validation!")
      val connectionType = buffer.get()
      return UserInformation(messageNumber, userName, clientType, connectionType)
    }
  }
}

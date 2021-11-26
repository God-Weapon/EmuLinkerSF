package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

abstract class CreateGame : V086Message() {
  abstract val username: String
  abstract val romName: String
  abstract val clientType: String
  abstract val gameId: Int
  abstract val val1: Int
  override val bodyLength: Int
    get() = getNumBytes(username) + getNumBytes(romName) + getNumBytes(clientType) + 7

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    EmuUtil.writeString(buffer, romName, 0x00, AppModule.charsetDoNotUse)
    EmuUtil.writeString(buffer, clientType, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedShort(buffer, gameId)
    UnsignedUtil.putUnsignedShort(buffer, val1)
  }

  companion object {
    const val ID: Byte = 0x0A
    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): CreateGame {
      if (buffer.remaining() < 8) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 6) throw ParseException("Failed byte count validation!")
      val romName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 5) throw ParseException("Failed byte count validation!")
      val clientType = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 4) throw ParseException("Failed byte count validation!")
      val gameID = UnsignedUtil.getUnsignedShort(buffer)
      val val1 = UnsignedUtil.getUnsignedShort(buffer)
      return if (Strings.isNullOrEmpty(userName) && gameID == 0xFFFF && val1 == 0xFFFF)
          CreateGame_Request(messageNumber, romName)
      else CreateGame_Notification(messageNumber, userName, romName, clientType, gameID, val1)
    }
  }
}

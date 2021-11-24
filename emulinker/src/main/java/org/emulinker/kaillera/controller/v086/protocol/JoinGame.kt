package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

abstract class JoinGame : V086Message() {
  abstract val gameId: Int
  abstract val val1: Int
  abstract val username: String?
  abstract val ping: Long
  abstract val userId: Int
  abstract val connectionType: Byte

  override val bodyLength: Int
    get() = getNumBytes(username!!) + 13

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedShort(buffer, gameId)
    UnsignedUtil.putUnsignedShort(buffer, val1)
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedInt(buffer, ping)
    UnsignedUtil.putUnsignedShort(buffer, userId)
    buffer.put(connectionType)
  }

  companion object {
    const val ID: Byte = 0x0C
    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): JoinGame {
      if (buffer.remaining() < 13) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      if (b.toInt() != 0x00)
          throw MessageFormatException("Invalid format: byte 0 = " + EmuUtil.byteToHex(b))
      val gameID = UnsignedUtil.getUnsignedShort(buffer)
      val val1 = UnsignedUtil.getUnsignedShort(buffer)
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 7) throw ParseException("Failed byte count validation!")
      val ping = UnsignedUtil.getUnsignedInt(buffer)
      val userID = UnsignedUtil.getUnsignedShort(buffer)
      val connectionType = buffer.get()
      return if (Strings.isNullOrEmpty(userName) && ping == 0L && userID == 0xFFFF)
          JoinGame_Request(messageNumber, gameID, connectionType)
      else
          JoinGame_Notification(messageNumber, gameID, val1, userName, ping, userID, connectionType)
    }
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

abstract class QuitGame : V086Message() {
  abstract val username: String?
  abstract val userId: Int

  override val messageId = ID

  override val bodyLength: Int
    get() = getNumBytes(username!!) + 3

  public override fun writeBodyTo(buffer: ByteBuffer) {
    EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
    UnsignedUtil.putUnsignedShort(buffer, userId)
  }

  companion object {
    const val ID: Byte = 0x0B

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): QuitGame {
      if (buffer.remaining() < 3) throw ParseException("Failed byte count validation!")
      val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
      if (buffer.remaining() < 2) throw ParseException("Failed byte count validation!")
      val userID = UnsignedUtil.getUnsignedShort(buffer)
      return if (Strings.isNullOrEmpty(userName) && userID == 0xFFFF) {
        QuitGame_Request(messageNumber)
      } else QuitGame_Notification(messageNumber, userName, userID)
    }
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.UnsignedUtil

abstract class StartGame : V086Message() {
  override val messageId = ID

  abstract val val1: Int
  abstract val playerNumber: Short
  abstract val numPlayers: Short

  override val bodyLength: Int
    get() = 5

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedShort(buffer, val1)
    UnsignedUtil.putUnsignedByte(buffer, playerNumber.toInt())
    UnsignedUtil.putUnsignedByte(buffer, numPlayers.toInt())
  }

  companion object {
    const val ID: Byte = 0x11
    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): StartGame {
      if (buffer.remaining() < 5) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      if (b.toInt() != 0x00) throw ParseException("Failed byte count validation!")
      val val1 = UnsignedUtil.getUnsignedShort(buffer)
      val playerNumber = UnsignedUtil.getUnsignedByte(buffer)
      val numPlayers = UnsignedUtil.getUnsignedByte(buffer)
      return if (val1 == 0xFFFF && playerNumber.toInt() == 0xFF && numPlayers.toInt() == 0xFF)
          StartGame_Request(messageNumber)
      else StartGame_Notification(messageNumber, val1, playerNumber, numPlayers)
    }
  }
}

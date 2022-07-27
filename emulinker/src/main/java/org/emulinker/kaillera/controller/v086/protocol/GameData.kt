package org.emulinker.kaillera.controller.v086.protocol

import java.nio.Buffer
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.util.UnsignedUtil

data class GameData
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val gameData: ByteArray) : V086Message() {

  override val messageId = ID

  override val bodyLength: Int
    get() = gameData.size + 3

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedShort(buffer, gameData.size)
    buffer.put(gameData)
  }

  init {
    validateMessageNumber(messageNumber)
    require(gameData.isNotEmpty()) { "gameData is empty" }
    require(gameData.size in 0..0xFFFF) { "gameData size out of range: ${gameData.size}" }
  }

  companion object {
    const val ID: Byte = 0x12

    // TODO(nue): Get rid of this.
    @Throws(Exception::class)
    fun main() {
      val data = ByteArray(9)
      val st = System.currentTimeMillis()
      val msg: GameData = create(0, data)
      val byteBuffer = ByteBuffer.allocateDirect(4096)
      for (i in 0..0xfffe) {
        msg.writeTo(byteBuffer)
        // Cast to avoid issue with java version mismatch:
        // https://stackoverflow.com/a/61267496/2875073
        (byteBuffer as Buffer).clear()
      }
      println("et=" + (System.currentTimeMillis() - st))
    }

    /** Same as the constructor, but it makes a deep copy of the array. */
    @Throws(MessageFormatException::class)
    fun create(messageNumber: Int, gameData: ByteArray): GameData {
      return GameData(messageNumber, gameData.copyOf(gameData.size))
    }

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): GameData {
      if (buffer.remaining() < 4) throw ParseException("Failed byte count validation!")
      val b = buffer.get()
      // removed to increase speed
      //		if (b != 0x00)
      //			throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " +
      // EmuUtil.byteToHex(b));
      val dataSize = UnsignedUtil.getUnsignedShort(buffer)
      if (dataSize <= 0 || dataSize > buffer.remaining())
          throw MessageFormatException("Invalid Game Data format: dataSize = $dataSize")
      val gameData = ByteArray(dataSize)
      buffer[gameData]
      return create(messageNumber, gameData)
    }
  }
}

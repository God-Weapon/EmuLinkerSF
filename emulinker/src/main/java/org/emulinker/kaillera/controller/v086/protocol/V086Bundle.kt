package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.protocol.V086Message.Companion.parse
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil.getUnsignedShort

private const val DESC = "Kaillera v.086 Message Bundle"

class V086Bundle
    @JvmOverloads
    constructor(messages: Array<V086Message?>, numToWrite: Int = Int.MAX_VALUE) :
    ByteBufferMessage() {
  var messages: Array<V086Message?>
    private set

  var numMessages: Int
    private set

  override var length = -1
    private set
    get() {
      if (field == -1) {
        for (i in 0 until numMessages) {
          if (messages[i] == null) break
          field += messages[i]!!.length
        }
      }
      return field
    }

  override val description = DESC

  override fun toString(): String {
    val sb = StringBuilder()
    sb.append("$description ($numMessages messages) ($length bytes)")
    sb.append(EmuUtil.LB)
    for (i in 0 until numMessages) {
      if (messages[i] == null) break
      sb.append("\tMessage " + (i + 1) + ": " + messages[i].toString() + EmuUtil.LB)
    }
    return sb.toString()
  }

  override fun writeTo(buffer: ByteBuffer?) {
    buffer!!.order(ByteOrder.LITTLE_ENDIAN)
    // no real need for unsigned
    // UnsignedUtil.putUnsignedByte(buffer, numToWrite);
    buffer.put(numMessages.toByte())
    for (i in 0 until numMessages) {
      if (messages[i] == null) break
      messages[i]!!.writeTo(buffer)
    }
  }

  companion object {

    @JvmStatic
    @JvmOverloads
    @Throws(ParseException::class, V086BundleFormatException::class, MessageFormatException::class)
    fun parse(buffer: ByteBuffer, lastMessageID: Int = -1): V086Bundle {
      buffer.order(ByteOrder.LITTLE_ENDIAN)
      if (buffer.limit() < 5)
          throw V086BundleFormatException(
              "Invalid buffer length: " + buffer.limit(), /* cause= */ null)

      // again no real need for unsigned
      // int messageCount = UnsignedUtil.getUnsignedByte(buffer);
      var messageCount = buffer.get().toInt()
      if (messageCount <= 0 || messageCount > 32)
          throw V086BundleFormatException("Invalid message count: $messageCount", cause = null)
      if (buffer.limit() < 1 + messageCount * 6)
          throw V086BundleFormatException("Invalid bundle length: " + buffer.limit(), cause = null)
      var parsedCount = 0
      val messages: Array<V086Message?>
      val msgNum =
          buffer.getChar(1)
              .code // buffer.getShort(1); - mistake. max value of short is 0x7FFF but we need
      // 0xFFFF
      if (msgNum - 1 == lastMessageID ||
          msgNum == 0 && lastMessageID == 0xFFFF) { // exception for 0 and 0xFFFF
        messageCount = 1
        messages = arrayOfNulls(messageCount)
        val messageNumber = getUnsignedShort(buffer)
        val messageLength = buffer.short
        if (messageLength < 2 || messageLength > buffer.remaining())
            throw ParseException("Invalid message length: $messageLength")
        messages[parsedCount] = parse(messageNumber, messageLength.toInt(), buffer)
        parsedCount++
      } else {
        messages = arrayOfNulls(messageCount)
        parsedCount = 0
        while (parsedCount < messageCount) {
          val messageNumber = getUnsignedShort(buffer)
          if (messageNumber <= lastMessageID) {
            if (messageNumber < 0x20 && lastMessageID > 0xFFDF) {
              // exception when messageNumber with lower value is greater
              // do nothing
            } else {
              break
            }
          } else if (messageNumber > 0xFFBF && lastMessageID < 0x40) {
            // exception when disorder messageNumber greater that lastMessageID
            break
          }
          val messageLength = buffer.short
          if (messageLength < 2 || messageLength > buffer.remaining())
              throw ParseException("Invalid message length: $messageLength")
          messages[parsedCount] = parse(messageNumber, messageLength.toInt(), buffer)
          parsedCount++
        }
      }
      return V086Bundle(messages, parsedCount)
    }
  }

  init {
    numMessages = messages.size
    if (numToWrite < numMessages) numMessages = numToWrite
    this.messages = messages
  }
}

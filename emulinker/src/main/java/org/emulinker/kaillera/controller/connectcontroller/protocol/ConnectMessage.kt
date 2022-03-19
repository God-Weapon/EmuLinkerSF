package org.emulinker.kaillera.controller.connectcontroller.protocol

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import kotlin.Throws
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.util.EmuUtil

abstract class ConnectMessage : ByteBufferMessage() {
  protected abstract val iD: String?

  companion object {
    // TODO(nue): Check if this can be made a constant.
    @JvmField var charset = Charsets.ISO_8859_1

    @Throws(MessageFormatException::class)
    fun parse(buffer: ByteBuffer): ConnectMessage {
      val messageStr =
          try {
            val stringDecoder = charset.newDecoder()
            stringDecoder.decode(buffer).toString()
          } catch (e: CharacterCodingException) {
            throw MessageFormatException("Invalid bytes received: failed to decode to a string!", e)
          }

      if (messageStr.startsWith(ConnectMessage_TOO.ID)) {
        return ConnectMessage_TOO.parse(messageStr)
      } else if (messageStr.startsWith(ConnectMessage_HELLOD00D.ID)) {
        return ConnectMessage_HELLOD00D.parse(messageStr)
      } else if (messageStr.startsWith(ConnectMessage_HELLO.ID)) {
        return ConnectMessage_HELLO.parse(messageStr)
      } else if (messageStr.startsWith(ConnectMessage_PING.ID)) {
        return ConnectMessage_PING.parse(messageStr)
      } else if (messageStr.startsWith(ConnectMessage_PONG.ID)) {
        return ConnectMessage_PONG.parse(messageStr)
      }
      buffer.rewind()
      throw MessageFormatException("Unrecognized connect message: " + EmuUtil.dumpBuffer(buffer))
    }
  }
}

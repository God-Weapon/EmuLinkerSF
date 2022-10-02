package org.emulinker.kaillera.controller.connectcontroller.protocol

import io.ktor.utils.io.core.*
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
    var charset = Charsets.ISO_8859_1

    @Throws(MessageFormatException::class)
    fun parse(buffer: ByteBuffer): ConnectMessage {
      val messageStr =
          try {
            val stringDecoder = charset.newDecoder()
            stringDecoder.decode(buffer).toString()
          } catch (e: CharacterCodingException) {
            throw MessageFormatException("Invalid bytes received: failed to decode to a string!", e)
          }

      when {
        messageStr.startsWith(ConnectMessage_TOO.ID) -> {
          return ConnectMessage_TOO.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_HELLOD00D.ID) -> {
          return ConnectMessage_HELLOD00D.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_HELLO.ID) -> {
          return ConnectMessage_HELLO.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_PING.ID) -> {
          return ConnectMessage_PING.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_PONG.ID) -> {
          return ConnectMessage_PONG.parse(messageStr)
        }
        else -> {
          buffer.rewind()
          throw MessageFormatException(
              "Unrecognized connect message: " + EmuUtil.dumpBuffer(buffer))
        }
      }
    }

    @Throws(MessageFormatException::class)
    fun parse(byteReadPacket: ByteReadPacket): ConnectMessage {
      val messageStr =
          try {
            //            val stringDecoder = charset.newDecoder()
            byteReadPacket.readText(charset)
            //            stringDecoder.decode(byteReadPacket).toString()
          } catch (e: CharacterCodingException) {
            throw MessageFormatException("Invalid bytes received: failed to decode to a string!", e)
          }

      when {
        messageStr.startsWith(ConnectMessage_TOO.ID) -> {
          return ConnectMessage_TOO.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_HELLOD00D.ID) -> {
          return ConnectMessage_HELLOD00D.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_HELLO.ID) -> {
          return ConnectMessage_HELLO.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_PING.ID) -> {
          return ConnectMessage_PING.parse(messageStr)
        }
        messageStr.startsWith(ConnectMessage_PONG.ID) -> {
          return ConnectMessage_PONG.parse(messageStr)
        }
        //      byteReadPacket.rewind()
        else ->
            throw MessageFormatException(
                "Unrecognized connect message: " +
                    EmuUtil.dumpBuffer(byteReadPacket.readByteBuffer()))
      }
    }
  }
}

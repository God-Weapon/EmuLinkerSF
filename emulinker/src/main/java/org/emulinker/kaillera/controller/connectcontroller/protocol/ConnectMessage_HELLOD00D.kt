package org.emulinker.kaillera.controller.connectcontroller.protocol

import java.lang.NumberFormatException
import java.nio.ByteBuffer
import kotlin.Throws
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.util.EmuUtil

class ConnectMessage_HELLOD00D(val port: Int) : ConnectMessage() {

  override val iD = ID
  override val description = DESC

  override fun toString(): String {
    return "$description: port: $port"
  }

  override val length = ID.length + port.toString().length + 1

  override fun writeTo(buffer: ByteBuffer?) {
    buffer!!.put(charset.encode(ID))
    EmuUtil.writeString(buffer, port.toString(), 0x00, charset)
  }

  companion object {
    const val ID = "HELLOD00D"
    const val DESC = "Server Connection Response"

    @Throws(MessageFormatException::class)
    fun parse(msg: String): ConnectMessage {
      if (msg.length < ID.length + 2) throw MessageFormatException("Invalid message length!")
      if (!msg.startsWith(ID)) throw MessageFormatException("Invalid message identifier!")
      if (msg[msg.length - 1].code != 0x00)
          throw MessageFormatException("Invalid message stop byte!")
      return try {
        val port = msg.substring(ID.length, msg.length - 1).toInt()
        ConnectMessage_HELLOD00D(port)
      } catch (e: NumberFormatException) {
        throw MessageFormatException("Invalid port number!")
      }
    }
  }
}

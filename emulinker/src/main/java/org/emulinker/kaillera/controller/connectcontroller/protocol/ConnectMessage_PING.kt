package org.emulinker.kaillera.controller.connectcontroller.protocol

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.Throws
import org.emulinker.kaillera.controller.messaging.MessageFormatException

// TODO(nue): Turn into a data class?
class ConnectMessage_PING : ConnectMessage() {

  override val iD = ID

  var clientSocketAddress: InetSocketAddress? = null
  override fun toString() = "Client Ping"

  override val length: Int
    get() = ID.length + 1

  override fun writeTo(buffer: ByteBuffer) {
    buffer.put(charset.encode(ID))
    buffer.put(0x00.toByte())
  }

  companion object {
    const val ID = "PING"

    @Throws(MessageFormatException::class)
    fun parse(msg: String): ConnectMessage {
      if (msg.length != 5) throw MessageFormatException("Invalid message length!")
      if (!msg.startsWith(ID)) throw MessageFormatException("Invalid message identifier!")
      if (msg.last().code != 0x00) throw MessageFormatException("Invalid message stop byte!")
      return ConnectMessage_PING()
    }
  }
}

package org.emulinker.kaillera.controller.connectcontroller.protocol

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.Throws
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.util.EmuUtil

data class ConnectMessage_HELLO(val protocol: String) : ConnectMessage() {

  override val iD = ID

  override val length = ID.length + protocol.length + 1

  var clientSocketAddress: InetSocketAddress? = null

  override fun writeTo(buffer: ByteBuffer?) {
    buffer!!.put(charset.encode(iD))
    EmuUtil.writeString(buffer, protocol, 0x00, charset)
  }

  companion object {
    const val ID = "HELLO"

    @Throws(MessageFormatException::class)
    fun parse(msg: String): ConnectMessage {
      if (msg.length < ID.length + 2) throw MessageFormatException("Invalid message length!")
      if (!msg.startsWith(ID)) throw MessageFormatException("Invalid message identifier!")
      if (msg[msg.length - 1].code != 0x00)
          throw MessageFormatException("Invalid message stop byte!")
      return ConnectMessage_HELLO(msg.substring(ID.length, msg.length - 1))
    }
  }
}

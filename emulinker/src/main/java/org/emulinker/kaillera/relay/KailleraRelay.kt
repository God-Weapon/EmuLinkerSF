package org.emulinker.kaillera.relay

import com.codahale.metrics.MetricRegistry
import com.google.common.flogger.FluentLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.net.InetSocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage.Companion.parse
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLO
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLOD00D
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_TOO
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.net.UDPRelay
import org.emulinker.util.EmuUtil.formatSocketAddress

@Deprecated("This doesn't seem to be used anywhere! Maybe we can get rid of it. ")
internal class KailleraRelay
    @AssistedInject
    constructor(
        @Assisted listenPort: Int,
        @Assisted serverSocketAddress: InetSocketAddress?,
        metrics: MetricRegistry?,
        private val v086RelayFactory: V086Relay.Factory
    ) : UDPRelay(listenPort, serverSocketAddress!!) {
  // TODO(nue): Can we just remove this?
  // public static void main(String args[]) throws Exception {
  //   int localPort = Integer.parseInt(args[0]);
  //   String serverIP = args[1];
  //   int serverPort = Integer.parseInt(args[2]);
  //   new KailleraRelay(localPort, new InetSocketAddress(serverIP, serverPort), new
  // MetricRegistry());
  // }
  @AssistedFactory
  interface Factory {
    fun create(listenPort: Int, serverSocketAddress: InetSocketAddress?): KailleraRelay?
  }

  override fun toString(): String {
    return "Kaillera main datagram relay on port " + super.listenPort
  }

  override fun processClientToServer(
      receiveBuffer: ByteBuffer, fromAddress: InetSocketAddress, toAddress: InetSocketAddress
  ): ByteBuffer? {
    var inMessage: ConnectMessage? = null
    inMessage =
        try {
          parse(receiveBuffer)
        } catch (e: MessageFormatException) {
          logger.atWarning().withCause(e).log("Unrecognized message format!")
          return null
        }
    logger
        .atFine()
        .log(
            formatSocketAddress(fromAddress) +
                " -> " +
                formatSocketAddress(toAddress) +
                ": " +
                inMessage)
    if (inMessage is ConnectMessage_HELLO) {
      logger.atInfo().log("Client version is " + inMessage.protocol)
    } else {
      logger.atWarning().log("Client sent an invalid message: $inMessage")
      return null
    }
    val sendBuffer = ByteBuffer.allocate(receiveBuffer.limit())
    receiveBuffer.rewind()
    sendBuffer.put(receiveBuffer)
    // Cast to avoid issue with java version mismatch:
    // https://stackoverflow.com/a/61267496/2875073
    (sendBuffer as Buffer).flip()
    return sendBuffer
  }

  override fun processServerToClient(
      receiveBuffer: ByteBuffer, fromAddress: InetSocketAddress, toAddress: InetSocketAddress
  ): ByteBuffer? {
    var inMessage: ConnectMessage? = null
    inMessage =
        try {
          parse(receiveBuffer)
        } catch (e: MessageFormatException) {
          logger.atWarning().withCause(e).log("Unrecognized message format!")
          return null
        }
    logger
        .atFine()
        .log(
            formatSocketAddress(fromAddress) +
                " -> " +
                formatSocketAddress(toAddress) +
                ": " +
                inMessage)
    if (inMessage is ConnectMessage_HELLOD00D) {
      val portMsg = inMessage
      logger.atInfo().log("Starting client relay on port " + (portMsg.port - 1))
      try {
        v086RelayFactory.create(
            portMsg.port, InetSocketAddress(serverSocketAddress.address, portMsg.port))
      } catch (e: Exception) {
        logger.atSevere().withCause(e).log("Failed to start!")
        return null
      }
    } else if (inMessage is ConnectMessage_TOO) {
      logger.atWarning().log("Failed to connect: Server is FULL!")
    } else {
      logger.atWarning().log("Server sent an invalid message: $inMessage")
      return null
    }
    val sendBuffer = ByteBuffer.allocate(receiveBuffer.limit())
    receiveBuffer.rewind()
    sendBuffer.put(receiveBuffer)
    // Cast to avoid issue with java version mismatch:
    // https://stackoverflow.com/a/61267496/2875073
    (sendBuffer as Buffer).flip()
    return sendBuffer
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}

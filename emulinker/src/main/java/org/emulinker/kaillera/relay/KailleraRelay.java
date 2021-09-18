package org.emulinker.kaillera.relay;

import com.google.common.flogger.FluentLogger;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.controller.connectcontroller.protocol.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.net.UDPRelay;
import org.emulinker.util.EmuUtil;

public class KailleraRelay extends UDPRelay {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  // This config is provided as a global static variable for purposes of speed.
  // We might want to consider something like Dagger for injection in the future.
  /** Runtime config of the binary. */
  public static RuntimeFlags config = null;

  public static void main(String args[]) throws Exception {
    int localPort = Integer.parseInt(args[0]);
    String serverIP = args[1];
    int serverPort = Integer.parseInt(args[2]);

    new KailleraRelay(localPort, new InetSocketAddress(serverIP, serverPort));
  }

  public KailleraRelay(int listenPort, InetSocketAddress serverSocketAddress) throws Exception {
    super(listenPort, serverSocketAddress);
  }

  @Override
  public String toString() {
    return "Kaillera main datagram relay on port " + super.getListenPort();
  }

  @Override
  protected ByteBuffer processClientToServer(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    ConnectMessage inMessage = null;

    try {
      inMessage = ConnectMessage.parse(receiveBuffer);
    } catch (MessageFormatException e) {
      logger.atWarning().withCause(e).log("Unrecognized message format!");
      return null;
    }

    logger.atFine().log(
        EmuUtil.formatSocketAddress(fromAddress)
            + " -> "
            + EmuUtil.formatSocketAddress(toAddress)
            + ": "
            + inMessage);

    if (inMessage instanceof ConnectMessage_HELLO) {
      ConnectMessage_HELLO clientTypeMessage = (ConnectMessage_HELLO) inMessage;
      logger.atInfo().log("Client version is " + clientTypeMessage.getProtocol());
    } else {
      logger.atWarning().log("Client sent an invalid message: " + inMessage);
      return null;
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    ((Buffer) sendBuffer).flip();
    return sendBuffer;
  }

  @Override
  protected ByteBuffer processServerToClient(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    ConnectMessage inMessage = null;

    try {
      inMessage = ConnectMessage.parse(receiveBuffer);
    } catch (MessageFormatException e) {
      logger.atWarning().withCause(e).log("Unrecognized message format!");
      return null;
    }

    logger.atFine().log(
        EmuUtil.formatSocketAddress(fromAddress)
            + " -> "
            + EmuUtil.formatSocketAddress(toAddress)
            + ": "
            + inMessage);

    if (inMessage instanceof ConnectMessage_HELLOD00D) {
      ConnectMessage_HELLOD00D portMsg = (ConnectMessage_HELLOD00D) inMessage;
      logger.atInfo().log("Starting client relay on port " + (portMsg.getPort() - 1));

      try {
        new V086Relay(
            portMsg.getPort(),
            new InetSocketAddress(getServerSocketAddress().getAddress(), portMsg.getPort()));
      } catch (Exception e) {
        logger.atSevere().withCause(e).log("Failed to start!");
        return null;
      }
    } else if (inMessage instanceof ConnectMessage_TOO) {
      logger.atWarning().log("Failed to connect: Server is FULL!");
    } else {
      logger.atWarning().log("Server sent an invalid message: " + inMessage);
      return null;
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    ((Buffer) sendBuffer).flip();
    return sendBuffer;
  }
}

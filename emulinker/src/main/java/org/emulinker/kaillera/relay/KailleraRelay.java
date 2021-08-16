package org.emulinker.kaillera.relay;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.connectcontroller.protocol.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.net.UDPRelay;
import org.emulinker.util.EmuUtil;

public class KailleraRelay extends UDPRelay {
  private static Log log = LogFactory.getLog(KailleraRelay.class);

  public static void main(String args[]) throws Exception {
    int localPort = Integer.parseInt(args[0]);
    String serverIP = args[1];
    int serverPort = Integer.parseInt(args[2]);

    new KailleraRelay(localPort, new InetSocketAddress(serverIP, serverPort));
  }

  public KailleraRelay(int listenPort, InetSocketAddress serverSocketAddress) throws Exception {
    super(listenPort, serverSocketAddress);
  }

  public String toString() {
    return "Kaillera main datagram relay on port " + super.getListenPort();
  }

  protected ByteBuffer processClientToServer(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    ConnectMessage inMessage = null;

    try {
      inMessage = ConnectMessage.parse(receiveBuffer);
    } catch (MessageFormatException e) {
      log.warn("Unrecognized message format!", e);
      return null;
    }

    log.debug(
        EmuUtil.formatSocketAddress(fromAddress)
            + " -> "
            + EmuUtil.formatSocketAddress(toAddress)
            + ": "
            + inMessage);

    if (inMessage instanceof ConnectMessage_HELLO) {
      ConnectMessage_HELLO clientTypeMessage = (ConnectMessage_HELLO) inMessage;
      log.info("Client version is " + clientTypeMessage.getProtocol());
    } else {
      log.warn("Client sent an invalid message: " + inMessage);
      return null;
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    sendBuffer.flip();
    return sendBuffer;
  }

  protected ByteBuffer processServerToClient(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    ConnectMessage inMessage = null;

    try {
      inMessage = ConnectMessage.parse(receiveBuffer);
    } catch (MessageFormatException e) {
      log.warn("Unrecognized message format!", e);
      return null;
    }

    log.debug(
        EmuUtil.formatSocketAddress(fromAddress)
            + " -> "
            + EmuUtil.formatSocketAddress(toAddress)
            + ": "
            + inMessage);

    if (inMessage instanceof ConnectMessage_HELLOD00D) {
      ConnectMessage_HELLOD00D portMsg = (ConnectMessage_HELLOD00D) inMessage;
      log.info("Starting client relay on port " + (portMsg.getPort() - 1));

      try {
        new V086Relay(
            portMsg.getPort(),
            new InetSocketAddress(getServerSocketAddress().getAddress(), portMsg.getPort()));
      } catch (Exception e) {
        log.error("Failed to start!", e);
        return null;
      }
    } else if (inMessage instanceof ConnectMessage_TOO) {
      log.warn("Failed to connect: Server is FULL!");
    } else {
      log.warn("Server sent an invalid message: " + inMessage);
      return null;
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    sendBuffer.flip();
    return sendBuffer;
  }
}

package org.emulinker.kaillera.relay;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.net.UDPRelay;
import org.emulinker.util.EmuUtil;

public class V086Relay extends UDPRelay {
  private static Log log = LogFactory.getLog(V086Relay.class);

  private int lastServerMessageNumber = -1;
  private int lastClientMessageNumber = -1;

  public V086Relay(int listenPort, InetSocketAddress serverSocketAddress) throws Exception {
    super(listenPort, serverSocketAddress);
  }

  public String toString() {
    return "Kaillera client datagram relay version 0.86 on port " + super.getListenPort();
  }

  protected ByteBuffer processClientToServer(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    V086Bundle inBundle = null;

    log.debug("-> " + EmuUtil.dumpBuffer(receiveBuffer));

    try {
      // inBundle = V086Bundle.parse(receiveBuffer, lastClientMessageNumber);
      inBundle = V086Bundle.parse(receiveBuffer, -1);
    } catch (ParseException e) {
      receiveBuffer.rewind();
      log.warn("Failed to parse: " + EmuUtil.dumpBuffer(receiveBuffer), e);
      return null;
    } catch (V086BundleFormatException e) {
      receiveBuffer.rewind();
      log.warn("Invalid message bundle format: " + EmuUtil.dumpBuffer(receiveBuffer), e);
      return null;
    } catch (MessageFormatException e) {
      receiveBuffer.rewind();
      log.warn("Invalid message format: " + EmuUtil.dumpBuffer(receiveBuffer), e);
      return null;
    }

    log.info("-> " + inBundle);

    V086Message[] inMessages = inBundle.getMessages();
    for (int i = 0; i < inBundle.getNumMessages(); i++) {
      if (inMessages[i].getNumber() > lastClientMessageNumber)
        lastClientMessageNumber = inMessages[i].getNumber();
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    sendBuffer.flip();

    return sendBuffer;
  }

  protected ByteBuffer processServerToClient(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    V086Bundle inBundle = null;

    log.debug("<- " + EmuUtil.dumpBuffer(receiveBuffer));

    try {
      // inBundle = V086Bundle.parse(receiveBuffer, lastServerMessageNumber);
      inBundle = V086Bundle.parse(receiveBuffer, -1);
    } catch (ParseException e) {
      receiveBuffer.rewind();
      log.warn("Failed to parse: " + EmuUtil.dumpBuffer(receiveBuffer), e);
      return null;
    } catch (V086BundleFormatException e) {
      receiveBuffer.rewind();
      log.warn("Invalid message bundle format: " + EmuUtil.dumpBuffer(receiveBuffer), e);
      return null;
    } catch (MessageFormatException e) {
      receiveBuffer.rewind();
      log.warn("Invalid message format: " + EmuUtil.dumpBuffer(receiveBuffer), e);
      return null;
    }

    log.info("<- " + inBundle);

    V086Message[] inMessages = inBundle.getMessages();
    for (int i = 0; i < inBundle.getNumMessages(); i++) {
      if (inMessages[i].getNumber() > lastServerMessageNumber)
        lastServerMessageNumber = inMessages[i].getNumber();
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    sendBuffer.flip();

    return sendBuffer;
  }
}

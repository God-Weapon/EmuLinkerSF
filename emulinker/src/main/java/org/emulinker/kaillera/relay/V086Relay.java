package org.emulinker.kaillera.relay;

import com.google.common.flogger.FluentLogger;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.net.UDPRelay;
import org.emulinker.util.EmuUtil;

public final class V086Relay extends UDPRelay {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private int lastServerMessageNumber = -1;
  private int lastClientMessageNumber = -1;

  @AssistedFactory
  public interface Factory {
    V086Relay create(int listenPort, InetSocketAddress serverSocketAddress);
  }

  @AssistedInject
  V086Relay(@Assisted int listenPort, @Assisted InetSocketAddress serverSocketAddress) {
    super(listenPort, serverSocketAddress);
  }

  @Override
  public String toString() {
    return "Kaillera client datagram relay version 0.86 on port " + super.getListenPort();
  }

  @Override
  protected ByteBuffer processClientToServer(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress) {
    V086Bundle inBundle = null;

    logger.atFine().log("-> " + EmuUtil.dumpBuffer(receiveBuffer));

    try {
      // inBundle = V086Bundle.parse(receiveBuffer, lastClientMessageNumber);
      inBundle = V086Bundle.parse(receiveBuffer, -1);
    } catch (ParseException e) {
      receiveBuffer.rewind();
      logger.atWarning().withCause(e).log("Failed to parse: " + EmuUtil.dumpBuffer(receiveBuffer));
      return null;
    } catch (V086BundleFormatException e) {
      receiveBuffer.rewind();
      logger.atWarning().withCause(e).log(
          "Invalid message bundle format: " + EmuUtil.dumpBuffer(receiveBuffer));
      return null;
    } catch (MessageFormatException e) {
      receiveBuffer.rewind();
      logger.atWarning().withCause(e).log(
          "Invalid message format: " + EmuUtil.dumpBuffer(receiveBuffer));
      return null;
    }

    logger.atFine().log("-> " + inBundle);

    V086Message[] inMessages = inBundle.getMessages();
    for (int i = 0; i < inBundle.getNumMessages(); i++) {
      if (inMessages[i].messageNumber() > lastClientMessageNumber)
        lastClientMessageNumber = inMessages[i].messageNumber();
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
    V086Bundle inBundle = null;

    logger.atFine().log("<- " + EmuUtil.dumpBuffer(receiveBuffer));

    try {
      // inBundle = V086Bundle.parse(receiveBuffer, lastServerMessageNumber);
      inBundle = V086Bundle.parse(receiveBuffer, -1);
    } catch (ParseException e) {
      receiveBuffer.rewind();
      logger.atWarning().withCause(e).log("Failed to parse: " + EmuUtil.dumpBuffer(receiveBuffer));
      return null;
    } catch (V086BundleFormatException e) {
      receiveBuffer.rewind();
      logger.atWarning().withCause(e).log(
          "Invalid message bundle format: " + EmuUtil.dumpBuffer(receiveBuffer));
      return null;
    } catch (MessageFormatException e) {
      receiveBuffer.rewind();
      logger.atWarning().withCause(e).log(
          "Invalid message format: " + EmuUtil.dumpBuffer(receiveBuffer));
      return null;
    }

    logger.atInfo().log("<- " + inBundle);

    V086Message[] inMessages = inBundle.getMessages();
    for (int i = 0; i < inBundle.getNumMessages(); i++) {
      if (inMessages[i].messageNumber() > lastServerMessageNumber)
        lastServerMessageNumber = inMessages[i].messageNumber();
    }

    ByteBuffer sendBuffer = ByteBuffer.allocate(receiveBuffer.limit());
    receiveBuffer.rewind();
    sendBuffer.put(receiveBuffer);
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    ((Buffer) sendBuffer).flip();

    return sendBuffer;
  }
}

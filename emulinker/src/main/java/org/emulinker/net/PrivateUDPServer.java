package org.emulinker.net;

import java.net.*;
import java.nio.ByteBuffer;
import org.apache.commons.logging.*;
import org.emulinker.util.*;

public abstract class PrivateUDPServer extends UDPServer {
  private static Log log = LogFactory.getLog(PrivateUDPServer.class);

  private InetAddress remoteAddress;
  private InetSocketAddress remoteSocketAddress;

  public PrivateUDPServer(boolean shutdownOnExit, InetAddress remoteAddress) {
    super(shutdownOnExit);
    this.remoteAddress = remoteAddress;
  }

  public InetAddress getRemoteInetAddress() {
    return remoteAddress;
  }

  public InetSocketAddress getRemoteSocketAddress() {
    return remoteSocketAddress;
  }

  @Override
  protected void handleReceived(ByteBuffer buffer, InetSocketAddress inboundSocketAddress) {
    if (remoteSocketAddress == null) remoteSocketAddress = inboundSocketAddress;
    else if (!inboundSocketAddress.equals(remoteSocketAddress)) {
      log.warn(
          "Rejecting packet received from wrong address: "
              + EmuUtil.formatSocketAddress(inboundSocketAddress)
              + " != "
              + EmuUtil.formatSocketAddress(remoteSocketAddress));
      return;
    }

    handleReceived(buffer);
  }

  protected abstract void handleReceived(ByteBuffer buffer);

  protected void send(ByteBuffer buffer) {
    super.send(buffer, remoteSocketAddress);
  }
}

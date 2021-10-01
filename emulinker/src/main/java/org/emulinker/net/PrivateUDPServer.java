package org.emulinker.net;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.flogger.FluentLogger;
import java.net.*;
import java.nio.ByteBuffer;
import org.emulinker.util.*;

public abstract class PrivateUDPServer extends UDPServer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final InetAddress remoteAddress;
  private final Timer clientRequestTimer;

  private InetSocketAddress remoteSocketAddress;

  public PrivateUDPServer(
      boolean shutdownOnExit, InetAddress remoteAddress, MetricRegistry metrics) {
    super(shutdownOnExit, metrics);
    this.remoteAddress = remoteAddress;
    this.clientRequestTimer = metrics.timer(name(this.getClass(), "clientRequests"));
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
      logger.atWarning().log(
          "Rejecting packet received from wrong address: "
              + EmuUtil.formatSocketAddress(inboundSocketAddress)
              + " != "
              + EmuUtil.formatSocketAddress(remoteSocketAddress));
      return;
    }

    try (final Timer.Context context = clientRequestTimer.time()) {
      handleReceived(buffer);
    }
  }

  protected abstract void handleReceived(ByteBuffer buffer);

  protected void send(ByteBuffer buffer) {
    super.send(buffer, remoteSocketAddress);
  }
}

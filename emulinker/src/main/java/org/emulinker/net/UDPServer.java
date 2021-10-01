package org.emulinker.net;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.net.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import org.emulinker.util.*;

public abstract class UDPServer implements Executable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  /*
  	private static int		artificalPacketLossPercentage = 0;
  	private static int		artificalDelay = 0;
  	private static Random	random = new Random();

  	static
  	{
  		try
  		{
  			artificalPacketLossPercentage = Integer.parseInt(System.getProperty("artificalPacketLossPercentage"));
  			artificalDelay = Integer.parseInt(System.getProperty("artificalDelay"));
  		}
  		catch(Exception e) {}

  		if(artificalPacketLossPercentage > 0)
  			logger.atWarning().log("Introducing " + artificalPacketLossPercentage + "% artifical packet loss!");

  		if(artificalDelay > 0)
  			logger.atWarning().log("Introducing " + artificalDelay + "ms artifical delay!");
  	}
  */
  private int bindPort;
  private DatagramChannel channel;
  private boolean isRunning = false;
  private boolean stopFlag = false;

  protected final Timer serverToClientRequests;

  public UDPServer(boolean shutdownOnExit, MetricRegistry metrics) {
    if (shutdownOnExit) Runtime.getRuntime().addShutdownHook(new ShutdownThread());

    this.serverToClientRequests =
        metrics.timer(MetricRegistry.name(UDPServer.class, "clientToServerRequests"));
  }

  public int getBindPort() {
    return bindPort;
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }

  public synchronized boolean isBound() {
    if (channel == null) return false;
    if (channel.socket() == null) return false;
    return !channel.socket().isClosed();
  }

  public boolean isConnected() {
    return channel.isConnected();
  }

  public synchronized void start() {
    logger.atFine().log(toString() + " received start request!");
    if (isRunning) {
      logger.atFine().log(toString() + " start request ignored: already running!");
      return;
    }

    stopFlag = false;
  }

  protected boolean getStopFlag() {
    return stopFlag;
  }

  @Override
  public synchronized void stop() {
    stopFlag = true;

    if (channel != null) {
      try {
        channel.close();
      } catch (IOException e) {
        logger.atSevere().withCause(e).log("Failed to close DatagramChannel");
      }
    }
  }

  protected synchronized void bind() throws BindException {
    bind(-1);
  }

  protected synchronized void bind(int port) throws BindException {
    try {
      channel = DatagramChannel.open();

      if (port > 0) channel.socket().bind(new InetSocketAddress(port));
      else channel.socket().bind(null);

      bindPort = channel.socket().getLocalPort();

      ByteBuffer tempBuffer = getBuffer();
      int bufferSize = (tempBuffer.capacity() * 2);
      releaseBuffer(tempBuffer);

      channel.socket().setReceiveBufferSize(bufferSize);
      channel.socket().setSendBufferSize(bufferSize);
    } catch (IOException e) {
      throw new BindException("Failed to bind to port " + port, port, e);
    }

    this.start();
  }

  protected abstract ByteBuffer getBuffer();

  protected abstract void releaseBuffer(ByteBuffer buffer);

  protected abstract void handleReceived(ByteBuffer buffer, InetSocketAddress remoteSocketAddress);

  protected void send(ByteBuffer buffer, InetSocketAddress toSocketAddress) {
    if (!isBound()) {
      logger.atWarning().log(
          "Failed to send to "
              + EmuUtil.formatSocketAddress(toSocketAddress)
              + ": UDPServer is not bound!");
      return;
    }
    /*
    if(artificalPacketLossPercentage > 0 && Math.abs(random.nextInt()%100) < artificalPacketLossPercentage)
    {
    	return;
    }
    */
    try {
      //			logger.atFine().log("send("+EmuUtil.dumpBuffer(buffer, false)+")");
      channel.send(buffer, toSocketAddress);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to send on port " + getBindPort());
    }
  }

  @Override
  public void run() {
    isRunning = true;
    logger.atFine().log(toString() + ": thread running...");

    try {
      while (!stopFlag) {
        try {
          ByteBuffer buffer = getBuffer();
          InetSocketAddress fromSocketAddress = (InetSocketAddress) channel.receive(buffer);

          if (stopFlag) break;

          if (fromSocketAddress == null)
            throw new IOException(
                "Failed to receive from DatagramChannel: fromSocketAddress == null");
          /*
          if(artificalPacketLossPercentage > 0 && Math.abs(random.nextInt()%100) < artificalPacketLossPercentage)
          {
          	releaseBuffer(buffer);
          	continue;
          }

          if(artificalDelay > 0)
          {
          	try
          	{
          		Thread.sleep(artificalDelay);
          	}
          	catch(Exception e) {}
          }
          */
          // Cast to avoid issue with java version mismatch:
          // https://stackoverflow.com/a/61267496/2875073
          ((Buffer) buffer).flip();
          //					logger.atFine().log("receive("+EmuUtil.dumpBuffer(buffer, false)+")");
          // TODO(nue): time this
          handleReceived(buffer, fromSocketAddress);
          releaseBuffer(buffer);
        } catch (SocketException e) {
          if (stopFlag) break;

          logger.atSevere().withCause(e).log("Failed to receive on port %d", getBindPort());
        } catch (IOException e) {
          if (stopFlag) break;

          logger.atSevere().withCause(e).log("Failed to receive on port %d", getBindPort());
        }
      }
    } catch (Throwable e) {
      logger.atSevere().withCause(e).log(
          "UDPServer on port %d caught unexpected exception!", getBindPort());
      stop();
    } finally {
      isRunning = false;
      logger.atFine().log(toString() + ": thread exiting...");
    }
  }

  private class ShutdownThread extends Thread {
    private ShutdownThread() {}

    @Override
    public void run() {
      UDPServer.this.stop();
    }
  }
}

package org.emulinker.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;
import org.apache.commons.logging.*;
import org.emulinker.util.EmuUtil;

public abstract class UDPRelay2 {
  private static Log log = LogFactory.getLog(UDPRelay2.class);

  public static final int DEFAULT_BUFFER_SIZE = 4096;
  private static int threadCounter = 0;

  protected int listenPort;
  protected int bufferSize;
  protected InetSocketAddress serverSocketAddress;
  protected boolean started = false;
  protected boolean stopFlag = false;
  protected Exception exception;
  protected Hashtable relayThreads = new Hashtable();
  protected Hashtable<Integer, DatagramChannel> channels =
      new Hashtable<Integer, DatagramChannel>();

  public UDPRelay2(InetSocketAddress serverSocketAddress, int listenPort) {
    this(serverSocketAddress, listenPort, DEFAULT_BUFFER_SIZE);
  }

  public UDPRelay2(InetSocketAddress serverSocketAddress, int listenPort, int bufferSize) {
    this.serverSocketAddress = serverSocketAddress;
    this.listenPort = listenPort;
    this.bufferSize = bufferSize;
  }

  public int getListenPort() {
    return listenPort;
  }

  public InetSocketAddress getServerSocketAddress() {
    return serverSocketAddress;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public Exception getException() {
    return exception;
  }

  public boolean isStarted() {
    return started;
  }

  protected abstract ByteBuffer processClientToServer(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress);

  protected abstract ByteBuffer processServerToClient(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress);

  public synchronized void start() throws IOException {
    if (!started) {
      stopFlag = false;
      relayThreads.put(serverSocketAddress, new RelayThread(listenPort, serverSocketAddress));
      started = true;
    } else {
      log.warn("Already started");
    }
  }

  public synchronized void stop() {
    if (started) {
      log.debug("Stopping...");

      stopFlag = true;

      Enumeration e = relayThreads.elements();
      while (e.hasMoreElements()) {
        ((RelayThread) e.nextElement()).close();
      }

      started = false;
    } else {
      log.warn("Not running");
    }
  }

  // TODO: Make Relay work when server is locahost
  protected class RelayThread extends Thread {
    protected int port;

    protected DatagramChannel channel;

    protected InetSocketAddress forwardAddress;

    protected String name;

    protected long lastActivity;

    protected boolean running = false;

    protected RelayThread(InetSocketAddress forwardAddress) throws IOException {
      this(-1, forwardAddress);
    }

    protected RelayThread(int port, InetSocketAddress forwardAddress) throws IOException {
      if (port > 0) {
        channel = channels.get(port);
        if (channel == null) {
          channel = DatagramChannel.open();
          channel.socket().bind(new InetSocketAddress(port));
          channels.put(port, channel);
          log.debug(
              "Created new DatagramChannel bound to specific port "
                  + channel.socket().getLocalPort()
                  + " that will forward to "
                  + EmuUtil.formatSocketAddress(forwardAddress));
        } else {
          log.debug(
              "Using previously created DatagramChannel bound to port "
                  + channel.socket().getLocalPort()
                  + " that will forward to "
                  + EmuUtil.formatSocketAddress(forwardAddress));
        }
      } else {
        channel = DatagramChannel.open();
        channel.socket().bind(null);
        log.debug(
            "Creating new DatagramChannel bound to arbitrary port "
                + channel.socket().getLocalPort()
                + " that will forward to "
                + EmuUtil.formatSocketAddress(forwardAddress));
      }

      lastActivity = System.currentTimeMillis();

      this.forwardAddress = forwardAddress;
      this.name =
          "RelayThread."
              + threadCounter++
              + ": "
              + channel.socket().getLocalPort()
              + "->"
              + EmuUtil.formatSocketAddress(forwardAddress);
      this.start();

      while (!running) {
        try {
          Thread.sleep(100);
        } catch (Exception e) {
          log.error("Sleep Interrupted!", e);
        }
      }
    }

    @Override
    public String toString() {
      return name;
    }

    public InetSocketAddress getForwardAddress() {
      return forwardAddress;
    }

    public long getLastActivity() {
      return lastActivity;
    }

    public DatagramChannel getChannel() {
      return channel;
    }

    public void send(ByteBuffer buffer, InetSocketAddress target) throws IOException {
      // log.debug("Port " + channel.socket().getLocalPort() + " sending
      // to " + EmuUtil.formatSocketAddress(target) + ": " +
      // EmuUtil.dumpBuffer(buffer));
      channel.send(buffer, target);
      lastActivity = System.currentTimeMillis();
    }

    public void close() {
      try {
        channel.close();
      } catch (Exception e) {
      }
    }

    @Override
    public void run() {
      log.debug(name + " Running");

      try {
        ByteBuffer receiveBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);

        while (!stopFlag) {
          running = true;

          // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
          ((Buffer) receiveBuffer).clear();

          InetSocketAddress fromAddress = (InetSocketAddress) channel.receive(receiveBuffer);
          // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
          ((Buffer) receiveBuffer).flip();

          lastActivity = System.currentTimeMillis();

          ByteBuffer sendBuffer = null;

          if (fromAddress.equals(getServerSocketAddress())) {
            // log.debug("Server at " +
            // EmuUtil.formatSocketAddress(fromAddress) + " sent " +
            // receiveBuffer.limit() + " bytes to relay port " +
            // channel.socket().getLocalPort() + " which it will
            // forward to " +
            // EmuUtil.formatSocketAddress(getForwardAddress()));
            // log.debug("Buffer Dump: " +
            // EmuUtil.dumpBuffer(receiveBuffer));
            sendBuffer = processServerToClient(receiveBuffer, fromAddress, getForwardAddress());
          } else {
            // log.debug("Client at " +
            // EmuUtil.formatSocketAddress(fromAddress) + " sent " +
            // receiveBuffer.limit() + " bytes to relay port " +
            // channel.socket().getLocalPort() + " which it will
            // forward to " +
            // EmuUtil.formatSocketAddress(getForwardAddress()));
            // log.debug("Buffer Dump: " +
            // EmuUtil.dumpBuffer(receiveBuffer));
            sendBuffer = processClientToServer(receiveBuffer, fromAddress, getForwardAddress());
          }

          if (sendBuffer == null || sendBuffer.limit() <= 0) continue;

          RelayThread responseThread = (RelayThread) relayThreads.get(fromAddress);
          if (responseThread == null) {
            log.debug(
                "No RelayThread is registered to forward to "
                    + EmuUtil.formatSocketAddress(fromAddress)
                    + "... creating new RelayThread");
            responseThread = new RelayThread(fromAddress);
            relayThreads.put(fromAddress, responseThread);
          }

          responseThread.send(sendBuffer, getForwardAddress());
        }
      } catch (IOException e) {
        log.warn(name + " caught IOException", e);
        if (exception != null) exception = e;
      } catch (Exception e) {
        log.fatal(name + " caught unexpected exception", e);
        if (exception != null) exception = e;
      } finally {
        UDPRelay2.this.stop();
        running = false;
      }

      log.debug(name + " Exiting");
    }
  }
}

package org.emulinker.net;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emulinker.util.*;

public abstract class UDPRelay implements Runnable {
  protected static Log log = LogFactory.getLog(UDPRelay.class);

  protected static ExecutorService threadPool = Executors.newCachedThreadPool();

  protected DatagramChannel listenChannel;

  protected int listenPort;
  protected InetSocketAddress serverSocketAddress;

  protected Map<InetSocketAddress, ClientHandler> clients =
      Collections.synchronizedMap(new HashMap<InetSocketAddress, ClientHandler>());

  public UDPRelay(int listenPort, InetSocketAddress serverSocketAddress) throws Exception {
    this.listenPort = listenPort;
    this.serverSocketAddress = serverSocketAddress;

    listenChannel = DatagramChannel.open();
    listenChannel.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), this.listenPort));

    log.info("Bound to port " + listenPort);

    threadPool.execute(this);
  }

  public int getListenPort() {
    return listenPort;
  }

  public DatagramChannel getListenChannel() {
    return listenChannel;
  }

  public InetSocketAddress getServerSocketAddress() {
    return serverSocketAddress;
  }

  protected abstract ByteBuffer processClientToServer(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress);

  protected abstract ByteBuffer processServerToClient(
      ByteBuffer receiveBuffer, InetSocketAddress fromAddress, InetSocketAddress toAddress);

  @Override
  public void run() {
    log.info("Main port " + listenPort + " thread running...");

    try {
      while (true) {
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        InetSocketAddress clientAddress = (InetSocketAddress) listenChannel.receive(buffer);

        ClientHandler clientHandler = clients.get(clientAddress);
        if (clientHandler == null) {
          try {
            clientHandler = new ClientHandler(clientAddress);
          } catch (Exception e) {
            log.error(
                "Failed to start new ClientHandler for "
                    + EmuUtil.formatSocketAddress(clientAddress),
                e);
            continue;
          }

          clients.put(clientAddress, clientHandler);
          threadPool.execute(clientHandler);
        }

        // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
        ((Buffer) buffer).flip();
        clientHandler.send(buffer);
      }
    } catch (Exception e) {
      log.error("Main port " + listenPort + " thread caught exception: " + e.getMessage(), e);
    } finally {
      try {
        listenChannel.close();
      } catch (Exception e) {
      }

      threadPool.shutdownNow();
    }

    log.info("Main port " + listenPort + " thread exiting...");
  }

  protected class ClientHandler implements Runnable {
    protected InetSocketAddress clientSocketAddress;
    protected DatagramChannel clientChannel;

    protected ClientHandler(InetSocketAddress clientSocketAddress) throws Exception {
      this.clientSocketAddress = clientSocketAddress;
      clientChannel = DatagramChannel.open();
      clientChannel.socket().bind(null);
      log.info(
          "ClientHandler for "
              + EmuUtil.formatSocketAddress(clientSocketAddress)
              + " bound to port "
              + clientChannel.socket().getPort());
    }

    protected void send(ByteBuffer buffer) throws Exception {
      //			log.info(EmuUtil.formatSocketAddress(clientSocketAddress) + " -> \t" +
      // EmuUtil.dumpBuffer(buffer));
      ByteBuffer newBuffer =
          processClientToServer(buffer, clientSocketAddress, serverSocketAddress);
      clientChannel.send(newBuffer, serverSocketAddress);
    }

    @Override
    public void run() {
      log.info(
          "ClientHandler thread for "
              + EmuUtil.formatSocketAddress(clientSocketAddress)
              + " runnning...");

      try {
        while (true) {
          ByteBuffer buffer = ByteBuffer.allocate(2048);
          InetSocketAddress receiveAddress = (InetSocketAddress) clientChannel.receive(buffer);

          if (!receiveAddress.getAddress().equals(serverSocketAddress.getAddress())) continue;

        // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
          ((Buffer) buffer).flip();

          //					log.info(EmuUtil.formatSocketAddress(clientSocketAddress) + " <- \t" +
          // EmuUtil.dumpBuffer(buffer));
          ByteBuffer newBuffer = processServerToClient(buffer, receiveAddress, clientSocketAddress);
          listenChannel.send(newBuffer, clientSocketAddress);
        }
      } catch (Exception e) {
        log.info(
            "ClientHandler thread for "
                + EmuUtil.formatSocketAddress(clientSocketAddress)
                + " caught exception: "
                + e.getMessage(),
            e);
      } finally {
        try {
          clientChannel.close();
        } catch (Exception e) {
        }

        clients.remove(clientSocketAddress);
      }

      log.info(
          "ClientHandler thread for "
              + EmuUtil.formatSocketAddress(clientSocketAddress)
              + " exiting...");
    }
  }
}

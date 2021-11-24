package org.emulinker.kaillera.controller.v086;

import com.codahale.metrics.MetricRegistry;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.messaging.ParseException;
import org.emulinker.kaillera.controller.v086.action.FatalActionException;
import org.emulinker.kaillera.controller.v086.action.V086Action;
import org.emulinker.kaillera.controller.v086.action.V086GameEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086ServerEventHandler;
import org.emulinker.kaillera.controller.v086.action.V086UserEventHandler;
import org.emulinker.kaillera.controller.v086.protocol.V086Bundle;
import org.emulinker.kaillera.controller.v086.protocol.V086BundleFormatException;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.GameEvent;
import org.emulinker.kaillera.model.event.KailleraEvent;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.event.ServerEvent;
import org.emulinker.kaillera.model.event.UserEvent;
import org.emulinker.net.BindException;
import org.emulinker.net.PrivateUDPServer;
import org.emulinker.util.ClientGameDataCache;
import org.emulinker.util.EmuUtil;
import org.emulinker.util.GameDataCache;

public final class V086ClientHandler extends PrivateUDPServer implements KailleraEventListener {
  private final V086Controller v086Controller;

  private KailleraUser user;
  private int messageNumberCounter = 0;
  private int prevMessageNumber = -1;
  private int lastMessageNumber = -1;
  private GameDataCache clientCache = null;
  private GameDataCache serverCache = null;

  // private LinkedList<V086Message>	lastMessages			= new LinkedList<V086Message>();
  private LastMessageBuffer lastMessageBuffer =
      new LastMessageBuffer(V086Controller.MAX_BUNDLE_SIZE);

  private V086Message[] outMessages = new V086Message[V086Controller.MAX_BUNDLE_SIZE];

  private final ByteBuffer inBuffer;
  private final ByteBuffer outBuffer;

  private Object inSynch = new Object();
  private Object outSynch = new Object();

  private long testStart;
  private long lastMeasurement;
  private int measurementCount = 0;
  private int bestTime = Integer.MAX_VALUE;

  private int clientRetryCount = 0;
  private long lastResend = 0;

  @AssistedFactory
  public interface Factory {
    V086ClientHandler create(InetSocketAddress remoteSocketAddress, V086Controller v086Controller);
  }

  @AssistedInject
  public V086ClientHandler(
      MetricRegistry metrics,
      RuntimeFlags flags,
      @Assisted InetSocketAddress remoteSocketAddress,
      @Assisted V086Controller v086Controller) {
    super(false, remoteSocketAddress.getAddress(), metrics);

    this.v086Controller = v086Controller;

    inBuffer = ByteBuffer.allocateDirect(flags.getV086BufferSize());
    outBuffer = ByteBuffer.allocateDirect(flags.getV086BufferSize());
    inBuffer.order(ByteOrder.LITTLE_ENDIAN);
    outBuffer.order(ByteOrder.LITTLE_ENDIAN);

    resetGameDataCache();
  }

  @Override
  public String toString() {
    if (getBindPort() > 0) return "V086Controller(" + getBindPort() + ")";
    else return "V086Controller(unbound)";
  }

  public V086Controller getController() {
    return v086Controller;
  }

  public KailleraUser getUser() {
    return user;
  }

  public synchronized int getNextMessageNumber() {
    if (messageNumberCounter > 0xFFFF) messageNumberCounter = 0;

    return messageNumberCounter++;
  }

  /*
  public List<V086Message> getLastMessage()
  {
  return lastMessages;
  }
  */

  public int getPrevMessageNumber() {
    return prevMessageNumber;
  }

  public int getLastMessageNumber() {
    return lastMessageNumber;
  }

  public GameDataCache getClientGameDataCache() {
    return clientCache;
  }

  public GameDataCache getServerGameDataCache() {
    return serverCache;
  }

  public void resetGameDataCache() {
    clientCache = new ClientGameDataCache(256);
    /*SF MOD - Button Ghosting Patch
    serverCache = new ServerGameDataCache(256);
    */
    serverCache = new ClientGameDataCache(256);
  }

  public void startSpeedTest() {
    testStart = lastMeasurement = System.currentTimeMillis();
    measurementCount = 0;
  }

  public void addSpeedMeasurement() {
    int et = (int) (System.currentTimeMillis() - lastMeasurement);
    if (et < bestTime) bestTime = et;
    measurementCount++;
    lastMeasurement = System.currentTimeMillis();
  }

  public int getSpeedMeasurementCount() {
    return measurementCount;
  }

  public int getBestNetworkSpeed() {
    return bestTime;
  }

  public int getAverageNetworkSpeed() {
    return (int) ((lastMeasurement - testStart) / measurementCount);
  }

  @Override
  public void bind(int port) throws BindException {
    super.bind(port);
  }

  public void start(KailleraUser user) {
    this.user = user;
    V086Controller.logger
        .atFine()
        .log(
            toString()
                + " thread starting (ThreadPool:"
                + v086Controller.threadPool.getActiveCount()
                + "/"
                + v086Controller.threadPool.getPoolSize()
                + ")");
    v086Controller.threadPool.execute(this);
    Thread.yield();

    /*
    long s = System.currentTimeMillis();
    while (!isBound() && (System.currentTimeMillis() - s) < 1000)
    {
    try
    {
    Thread.sleep(100);
    }
    catch (Exception e)
    {
    logger.atSevere().withCause(e).log("Sleep Interrupted!");
    }
    }

    if (!isBound())
    {
    logger.atSevere().log("V086ClientHandler failed to start for client from " + getRemoteInetAddress().getHostAddress());
    return;
    }
    */

    V086Controller.logger
        .atFine()
        .log(
            toString()
                + " thread started (ThreadPool:"
                + v086Controller.threadPool.getActiveCount()
                + "/"
                + v086Controller.threadPool.getPoolSize()
                + ")");
    v086Controller.clientHandlers.put(user.getId(), this);
  }

  @Override
  public void stop() {
    synchronized (this) {
      if (getStopFlag()) return;

      int port = -1;
      if (isBound()) port = getBindPort();
      V086Controller.logger.atFine().log(this.toString() + " Stopping!");
      super.stop();

      if (port > 0) {
        V086Controller.logger
            .atFine()
            .log(
                toString()
                    + " returning port "
                    + port
                    + " to available port queue: "
                    + (v086Controller.portRangeQueue.size() + 1)
                    + " available");
        v086Controller.portRangeQueue.add(port);
      }
    }

    if (user != null) {
      v086Controller.clientHandlers.remove(user.getId());
      user.stop();
      user = null;
    }
  }

  @Override
  protected ByteBuffer getBuffer() {
    // return ByteBufferMessage.getBuffer(bufferSize);
    // Cast to avoid issue with java version mismatch:
    // https://stackoverflow.com/a/61267496/2875073
    ((Buffer) inBuffer).clear();
    return inBuffer;
  }

  @Override
  protected void releaseBuffer(ByteBuffer buffer) {
    // ByteBufferMessage.releaseBuffer(buffer);
    // buffer.clear();
  }

  @Override
  protected void handleReceived(ByteBuffer buffer) {
    V086Bundle inBundle = null;

    try {
      inBundle = V086Bundle.parse(buffer, lastMessageNumber);
      // inBundle = V086Bundle.parse(buffer, -1);
    } catch (ParseException e) {
      buffer.rewind();
      V086Controller.logger
          .atWarning()
          .withCause(e)
          .log(toString() + " failed to parse: " + EmuUtil.dumpBuffer(buffer));
      return;
    } catch (V086BundleFormatException e) {
      buffer.rewind();
      V086Controller.logger
          .atWarning()
          .withCause(e)
          .log(toString() + " received invalid message bundle: " + EmuUtil.dumpBuffer(buffer));
      return;
    } catch (MessageFormatException e) {
      buffer.rewind();
      V086Controller.logger
          .atWarning()
          .withCause(e)
          .log(toString() + " received invalid message: " + EmuUtil.dumpBuffer(buffer));
      return;
    }

    // logger.atFine().log("-> " + inBundle.getNumMessages());

    if (inBundle.getNumMessages() == 0) {
      V086Controller.logger
          .atFine()
          .log(
              toString()
                  + " received bundle of "
                  + inBundle.getNumMessages()
                  + " messages from "
                  + user);
      clientRetryCount++;
      resend(clientRetryCount);
      return;
    } else {
      clientRetryCount = 0;
    }

    try {
      synchronized (inSynch) {
        V086Message[] messages = inBundle.getMessages();
        if (inBundle.getNumMessages() == 1) {
          lastMessageNumber = messages[0].getMessageNumber();

          V086Action action = v086Controller.actions[messages[0].getMessageId()];
          if (action == null) {
            V086Controller.logger
                .atSevere()
                .log("No action defined to handle client message: " + messages[0]);
          }

          action.performAction(messages[0], this);
        } else {
          // read the bundle from back to front to process the oldest messages first
          for (int i = (inBundle.getNumMessages() - 1); i >= 0; i--) {
            /**
             * already extracts messages with higher numbers when parsing, it does not need to be
             * checked and this causes an error if messageNumber is 0 and lastMessageNumber is
             * 0xFFFF if (messages[i].getNumber() > lastMessageNumber)
             */
            {
              prevMessageNumber = lastMessageNumber;
              lastMessageNumber = messages[i].getMessageNumber();

              if ((prevMessageNumber + 1) != lastMessageNumber) {
                if (prevMessageNumber == 0xFFFF && lastMessageNumber == 0) {
                  // exception; do nothing
                } else {
                  V086Controller.logger
                      .atWarning()
                      .log(
                          user
                              + " dropped a packet! ("
                              + prevMessageNumber
                              + " to "
                              + lastMessageNumber
                              + ")");
                  user.droppedPacket();
                }
              }

              V086Action action = v086Controller.actions[messages[i].getMessageId()];
              if (action == null) {
                V086Controller.logger
                    .atSevere()
                    .log("No action defined to handle client message: " + messages[i]);
                continue;
              }

              // logger.atFine().log(user + " -> " + message);
              action.performAction(messages[i], this);
            }
          }
        }
      }
    } catch (FatalActionException e) {
      V086Controller.logger
          .atWarning()
          .withCause(e)
          .log(toString() + " fatal action, closing connection");
      Thread.yield();
      stop();
    }
  }

  @Override
  public void actionPerformed(KailleraEvent event) {
    if (event instanceof GameEvent) {
      V086GameEventHandler eventHandler = v086Controller.gameEventHandlers.get(event.getClass());
      if (eventHandler == null) {
        V086Controller.logger
            .atSevere()
            .log(
                toString()
                    + " found no GameEventHandler registered to handle game event: "
                    + event);
        return;
      }

      eventHandler.handleEvent((GameEvent) event, this);
    } else if (event instanceof ServerEvent) {
      V086ServerEventHandler eventHandler =
          v086Controller.serverEventHandlers.get(event.getClass());
      if (eventHandler == null) {
        V086Controller.logger
            .atSevere()
            .log(
                toString()
                    + " found no ServerEventHandler registered to handle server event: "
                    + event);
        return;
      }

      eventHandler.handleEvent((ServerEvent) event, this);
    } else if (event instanceof UserEvent) {
      V086UserEventHandler eventHandler = v086Controller.userEventHandlers.get(event.getClass());
      if (eventHandler == null) {
        V086Controller.logger
            .atSevere()
            .log(
                toString()
                    + " found no UserEventHandler registered to handle user event: "
                    + event);
        return;
      }

      eventHandler.handleEvent((UserEvent) event, this);
    }
  }

  public void resend(int timeoutCounter) {

    synchronized (outSynch) {
      // if ((System.currentTimeMillis() - lastResend) > (user.getPing()*3))
      if ((System.currentTimeMillis() - lastResend) > v086Controller.server.getMaxPing()) {
        // int numToSend = (3+timeoutCounter);
        int numToSend = (3 * timeoutCounter);
        if (numToSend > V086Controller.MAX_BUNDLE_SIZE) numToSend = V086Controller.MAX_BUNDLE_SIZE;

        V086Controller.logger.atFine().log(this + ": resending last " + numToSend + " messages");
        send(null, numToSend);
        lastResend = System.currentTimeMillis();
      } else {
        V086Controller.logger.atFine().log("Skipping resend...");
      }
    }
  }

  public void send(V086Message outMessage) {
    send(outMessage, 5);
  }

  public void send(V086Message outMessage, int numToSend) {
    synchronized (outSynch) {
      if (outMessage != null) {
        lastMessageBuffer.add(outMessage);
      }

      numToSend = lastMessageBuffer.fill(outMessages, numToSend);
      // System.out.println("Server -> " + numToSend);
      V086Bundle outBundle = new V086Bundle(outMessages, numToSend);
      //				logger.atFine().log("<- " + outBundle);
      outBundle.writeTo(outBuffer);
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      ((Buffer) outBuffer).flip();
      super.send(outBuffer);
      ((Buffer) outBuffer).clear();
    }
  }
}

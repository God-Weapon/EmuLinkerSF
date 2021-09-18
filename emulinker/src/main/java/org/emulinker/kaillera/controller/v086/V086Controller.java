package org.emulinker.kaillera.controller.v086;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import java.net.InetSocketAddress;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.configuration.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.controller.v086.action.*;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.net.*;
import org.emulinker.util.*;

@Singleton
public final class V086Controller implements KailleraServerController {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private int MAX_BUNDLE_SIZE = 9;

  private int bufferSize = 4096;
  private boolean isRunning = false;

  private ThreadPoolExecutor threadPool;
  private KailleraServer server;
  private String[] clientTypes;
  private Map<Integer, V086ClientHandler> clientHandlers =
      new ConcurrentHashMap<Integer, V086ClientHandler>();

  private int portRangeStart;
  private int extraPorts;
  private Queue<Integer> portRangeQueue = new ConcurrentLinkedQueue<Integer>();

  private final ImmutableMap<Class<?>, V086ServerEventHandler> serverEventHandlers;
  private final ImmutableMap<Class<?>, V086GameEventHandler> gameEventHandlers;
  private final ImmutableMap<Class<?>, V086UserEventHandler> userEventHandlers;

  private V086Action[] actions = new V086Action[25];

  @Inject
  V086Controller(
      KailleraServer server,
      ThreadPoolExecutor threadPool,
      AccessManager accessManager,
      Configuration config,
      LoginAction loginAction,
      ACKAction ackAction,
      ChatAction chatAction,
      CreateGameAction createGameAction,
      JoinGameAction joinGameAction,
      KeepAliveAction keepAliveAction,
      QuitGameAction quitGameAction,
      QuitAction quitAction,
      StartGameAction startGameAction,
      GameChatAction gameChatAction,
      GameKickAction gameKickAction,
      UserReadyAction userReadyAction,
      CachedGameDataAction cachedGameDataAction,
      GameDataAction gameDataAction,
      DropGameAction dropGameAction,
      CloseGameAction closeGameAction,
      GameStatusAction gameStatusAction,
      GameDesynchAction gameDesynchAction,
      PlayerDesynchAction playerDesynchAction,
      GameInfoAction gameInfoAction,
      GameTimeoutAction gameTimeoutAction,
      InfoMessageAction infoMessageAction) {
    this.threadPool = threadPool;
    this.server = server;
    this.bufferSize = config.getInt("controllers.v086.bufferSize");
    this.clientTypes = config.getStringArray("controllers.v086.clientTypes.clientType");

    this.portRangeStart = config.getInt("controllers.v086.portRangeStart");
    this.extraPorts = config.getInt("controllers.v086.extraPorts", 0);
    int maxPort = 0;
    for (int i = portRangeStart; i <= (portRangeStart + server.getMaxUsers() + extraPorts); i++) {
      portRangeQueue.add(i);
      maxPort = i;
    }

    logger.atWarning().log(
        "Listening on UDP ports: "
            + portRangeStart
            + " to "
            + maxPort
            + ".  Make sure these ports are open in your firewall!");

    Preconditions.checkArgument(bufferSize > 0, "controllers.v086.bufferSize must be > 0");

    // array access should be faster than a hash and we won't have to create
    // a new Integer each time
    actions[UserInformation.ID] = loginAction;
    actions[ClientACK.ID] = ackAction;
    actions[Chat.ID] = chatAction;
    actions[CreateGame.ID] = createGameAction;
    actions[JoinGame.ID] = joinGameAction;
    actions[KeepAlive.ID] = keepAliveAction;
    actions[QuitGame.ID] = quitGameAction;
    actions[Quit.ID] = quitAction;
    actions[StartGame.ID] = startGameAction;
    actions[GameChat.ID] = gameChatAction;
    actions[GameKick.ID] = gameKickAction;
    actions[AllReady.ID] = userReadyAction;
    actions[CachedGameData.ID] = cachedGameDataAction;
    actions[GameData.ID] = gameDataAction;
    actions[PlayerDrop.ID] = dropGameAction;

    // setup the server event handlers
    serverEventHandlers =
        ImmutableMap.<Class<?>, V086ServerEventHandler>builder()
            .put(ChatEvent.class, chatAction)
            .put(GameCreatedEvent.class, createGameAction)
            .put(UserJoinedEvent.class, loginAction)
            .put(GameClosedEvent.class, closeGameAction)
            .put(UserQuitEvent.class, quitAction)
            .put(GameStatusChangedEvent.class, gameStatusAction)
            .build();

    // setup the game event handlers
    gameEventHandlers =
        ImmutableMap.<Class<?>, V086GameEventHandler>builder()
            .put(UserJoinedGameEvent.class, joinGameAction)
            .put(UserQuitGameEvent.class, quitGameAction)
            .put(GameStartedEvent.class, startGameAction)
            .put(GameChatEvent.class, gameChatAction)
            .put(AllReadyEvent.class, userReadyAction)
            .put(GameDataEvent.class, gameDataAction)
            .put(UserDroppedGameEvent.class, dropGameAction)
            .put(GameDesynchEvent.class, gameDesynchAction)
            .put(PlayerDesynchEvent.class, playerDesynchAction)
            .put(GameInfoEvent.class, gameInfoAction)
            .put(GameTimeoutEvent.class, gameTimeoutAction)
            .build();

    // setup the user event handlers
    userEventHandlers =
        ImmutableMap.<Class<?>, V086UserEventHandler>builder()
            .put(ConnectedEvent.class, ackAction)
            .put(InfoMessageEvent.class, infoMessageAction)
            .build();
  }

  @Override
  public String getVersion() {
    return "v086";
  }

  @Override
  public String[] getClientTypes() {
    return clientTypes;
  }

  @Override
  public KailleraServer getServer() {
    return server;
  }

  @Override
  public int getNumClients() {
    return clientHandlers.size();
  }

  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  public final ImmutableMap<Class<?>, V086ServerEventHandler> getServerEventHandlers() {
    return serverEventHandlers;
  }

  public final ImmutableMap<Class<?>, V086GameEventHandler> getGameEventHandlers() {
    return gameEventHandlers;
  }

  public final ImmutableMap<Class<?>, V086UserEventHandler> getUserEventHandlers() {
    return userEventHandlers;
  }

  public final V086Action[] getActions() {
    return actions;
  }

  public final Map<Integer, V086ClientHandler> getClientHandlers() {
    return clientHandlers;
  }

  public boolean isRunning() {
    return isRunning;
  }

  @Override
  public String toString() {
    return "V086Controller[clients=" + clientHandlers.size() + " isRunning=" + isRunning + "]";
  }

  @Override
  public int newConnection(InetSocketAddress clientSocketAddress, String protocol)
      throws ServerFullException, NewConnectionException {
    if (!isRunning) throw new NewConnectionException("Controller is not running");

    V086ClientHandler clientHandler = new V086ClientHandler(clientSocketAddress);
    KailleraUser user = server.newConnection(clientSocketAddress, protocol, clientHandler);

    int boundPort = -1;
    int bindAttempts = 0;
    while (bindAttempts++ < 5) {
      Integer portInteger = portRangeQueue.poll();
      if (portInteger == null) {
        logger.atSevere().log("No ports are available to bind for: " + user);
      } else {
        int port = portInteger.intValue();
        logger.atInfo().log("Private port " + port + " allocated to: " + user);

        try {
          clientHandler.bind(port);
          boundPort = port;
          break;
        } catch (BindException e) {
          logger.atSevere().withCause(e).log("Failed to bind to port " + port + " for: " + user);
          logger.atFine().log(
              toString()
                  + " returning port "
                  + port
                  + " to available port queue: "
                  + (portRangeQueue.size() + 1)
                  + " available");
          portRangeQueue.add(port);
        }
      }

      try {
        // pause very briefly to give the OS a chance to free a port
        Thread.sleep(5);
      } catch (InterruptedException e) {
      }
    }

    if (boundPort < 0) {
      clientHandler.stop();
      throw new NewConnectionException("Failed to bind!");
    }

    clientHandler.start(user);
    return boundPort;
  }

  @Override
  public synchronized void start() {
    isRunning = true;
  }

  @Override
  public synchronized void stop() {
    isRunning = false;

    for (V086ClientHandler clientHandler : clientHandlers.values()) clientHandler.stop();

    clientHandlers.clear();
  }

  public class V086ClientHandler extends PrivateUDPServer implements KailleraEventListener {
    private KailleraUser user;
    private int messageNumberCounter = 0;
    private int prevMessageNumber = -1;
    private int lastMessageNumber = -1;
    private GameDataCache clientCache = null;
    private GameDataCache serverCache = null;

    // private LinkedList<V086Message>	lastMessages			= new LinkedList<V086Message>();
    private LastMessageBuffer lastMessageBuffer = new LastMessageBuffer(MAX_BUNDLE_SIZE);

    private V086Message[] outMessages = new V086Message[MAX_BUNDLE_SIZE];

    private ByteBuffer inBuffer = ByteBuffer.allocateDirect(bufferSize);
    private ByteBuffer outBuffer = ByteBuffer.allocateDirect(bufferSize);

    private Object inSynch = new Object();
    private Object outSynch = new Object();

    private long testStart;
    private long lastMeasurement;
    private int measurementCount = 0;
    private int bestTime = Integer.MAX_VALUE;

    private int clientRetryCount = 0;
    private long lastResend = 0;

    private V086ClientHandler(InetSocketAddress remoteSocketAddress) {
      super(false, remoteSocketAddress.getAddress());

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
      return V086Controller.this;
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
      logger.atFine().log(
          toString()
              + " thread starting (ThreadPool:"
              + threadPool.getActiveCount()
              + "/"
              + threadPool.getPoolSize()
              + ")");
      threadPool.execute(this);
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

      logger.atFine().log(
          toString()
              + " thread started (ThreadPool:"
              + threadPool.getActiveCount()
              + "/"
              + threadPool.getPoolSize()
              + ")");
      clientHandlers.put(user.getID(), this);
    }

    @Override
    public void stop() {
      synchronized (this) {
        if (getStopFlag()) return;

        int port = -1;
        if (isBound()) port = getBindPort();
        logger.atFine().log(this.toString() + " Stopping!");
        super.stop();

        if (port > 0) {
          logger.atFine().log(
              toString()
                  + " returning port "
                  + port
                  + " to available port queue: "
                  + (portRangeQueue.size() + 1)
                  + " available");
          portRangeQueue.add(port);
        }
      }

      if (user != null) {
        clientHandlers.remove(user.getID());
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
        logger.atWarning().withCause(e).log(
            toString() + " failed to parse: " + EmuUtil.dumpBuffer(buffer));
        return;
      } catch (V086BundleFormatException e) {
        buffer.rewind();
        logger.atWarning().withCause(e).log(
            toString() + " received invalid message bundle: " + EmuUtil.dumpBuffer(buffer));
        return;
      } catch (MessageFormatException e) {
        buffer.rewind();
        logger.atWarning().withCause(e).log(
            toString() + " received invalid message: " + EmuUtil.dumpBuffer(buffer));
        return;
      }

      // logger.atFine().log("-> " + inBundle.getNumMessages());

      if (inBundle.getNumMessages() == 0) {
        logger.atFine().log(
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
            lastMessageNumber = messages[0].messageNumber();

            V086Action action = actions[messages[0].messageId()];
            if (action == null) {
              logger.atSevere().log("No action defined to handle client message: " + messages[0]);
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
                lastMessageNumber = messages[i].messageNumber();

                if ((prevMessageNumber + 1) != lastMessageNumber) {
                  if (prevMessageNumber == 0xFFFF && lastMessageNumber == 0) {
                    // exception; do nothing
                  } else {
                    logger.atWarning().log(
                        user
                            + " dropped a packet! ("
                            + prevMessageNumber
                            + " to "
                            + lastMessageNumber
                            + ")");
                    user.droppedPacket();
                  }
                }

                V086Action action = actions[messages[i].messageId()];
                if (action == null) {
                  logger.atSevere().log(
                      "No action defined to handle client message: " + messages[i]);
                  continue;
                }

                // logger.atFine().log(user + " -> " + message);
                action.performAction(messages[i], this);
              }
            }
          }
        }
      } catch (FatalActionException e) {
        logger.atWarning().withCause(e).log(toString() + " fatal action, closing connection");
        Thread.yield();
        stop();
      }
    }

    @Override
    public void actionPerformed(KailleraEvent event) {
      if (event instanceof GameEvent) {
        V086GameEventHandler eventHandler = gameEventHandlers.get(event.getClass());
        if (eventHandler == null) {
          logger.atSevere().log(
              toString() + " found no GameEventHandler registered to handle game event: " + event);
          return;
        }

        eventHandler.handleEvent((GameEvent) event, this);
      } else if (event instanceof ServerEvent) {
        V086ServerEventHandler eventHandler = serverEventHandlers.get(event.getClass());
        if (eventHandler == null) {
          logger.atSevere().log(
              toString()
                  + " found no ServerEventHandler registered to handle server event: "
                  + event);
          return;
        }

        eventHandler.handleEvent((ServerEvent) event, this);
      } else if (event instanceof UserEvent) {
        V086UserEventHandler eventHandler = userEventHandlers.get(event.getClass());
        if (eventHandler == null) {
          logger.atSevere().log(
              toString() + " found no UserEventHandler registered to handle user event: " + event);
          return;
        }

        eventHandler.handleEvent((UserEvent) event, this);
      }
    }

    public void resend(int timeoutCounter) {

      synchronized (outSynch) {
        // if ((System.currentTimeMillis() - lastResend) > (user.getPing()*3))
        if ((System.currentTimeMillis() - lastResend) > server.getMaxPing()) {
          // int numToSend = (3+timeoutCounter);
          int numToSend = (3 * timeoutCounter);
          if (numToSend > MAX_BUNDLE_SIZE) numToSend = MAX_BUNDLE_SIZE;

          logger.atFine().log(this + ": resending last " + numToSend + " messages");
          send(null, numToSend);
          lastResend = System.currentTimeMillis();
        } else {
          logger.atFine().log("Skipping resend...");
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
}

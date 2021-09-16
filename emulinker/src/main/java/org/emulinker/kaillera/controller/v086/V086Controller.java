package org.emulinker.kaillera.controller.v086;

import com.google.common.collect.ImmutableMap;
import java.net.InetSocketAddress;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;
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

public class V086Controller implements KailleraServerController {
  private static Log log = LogFactory.getLog(V086Controller.class);

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

  public V086Controller(
      KailleraServer server,
      ThreadPoolExecutor threadPool,
      AccessManager accessManager,
      Configuration config)
      throws NoSuchElementException, ConfigurationException {
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

    log.warn(
        "Listening on UDP ports: "
            + portRangeStart
            + " to "
            + maxPort
            + ".  Make sure these ports are open in your firewall!");

    if (bufferSize <= 0)
      throw new ConfigurationException("controllers.v086.bufferSize must be > 0");

    // array access should be faster than a hash and we won't have to create
    // a new Integer each time
    actions[UserInformation.ID] = LoginAction.getInstance();
    actions[ClientACK.ID] = ACKAction.getInstance();
    actions[Chat.ID] = ChatAction.getInstance();
    actions[CreateGame.ID] = CreateGameAction.getInstance();
    actions[JoinGame.ID] = JoinGameAction.getInstance();
    actions[KeepAlive.ID] = KeepAliveAction.getInstance();
    actions[QuitGame.ID] = QuitGameAction.getInstance();
    actions[Quit.ID] = QuitAction.getInstance();
    actions[StartGame.ID] = StartGameAction.getInstance();
    actions[GameChat.ID] = GameChatAction.getInstance();
    actions[GameKick.ID] = GameKickAction.getInstance();
    actions[AllReady.ID] = UserReadyAction.getInstance();
    actions[CachedGameData.ID] = CachedGameDataAction.getInstance();
    actions[GameData.ID] = GameDataAction.getInstance();
    actions[PlayerDrop.ID] = DropGameAction.getInstance();

    // setup the server event handlers
    serverEventHandlers =
        ImmutableMap.<Class<?>, V086ServerEventHandler>builder()
            .put(ChatEvent.class, ChatAction.getInstance())
            .put(GameCreatedEvent.class, CreateGameAction.getInstance())
            .put(UserJoinedEvent.class, LoginAction.getInstance())
            .put(GameClosedEvent.class, CloseGameAction.getInstance())
            .put(UserQuitEvent.class, QuitAction.getInstance())
            .put(GameStatusChangedEvent.class, GameStatusAction.getInstance())
            .build();

    // setup the game event handlers
    gameEventHandlers =
        ImmutableMap.<Class<?>, V086GameEventHandler>builder()
            .put(UserJoinedGameEvent.class, JoinGameAction.getInstance())
            .put(UserQuitGameEvent.class, QuitGameAction.getInstance())
            .put(GameStartedEvent.class, StartGameAction.getInstance())
            .put(GameChatEvent.class, GameChatAction.getInstance())
            .put(AllReadyEvent.class, UserReadyAction.getInstance())
            .put(GameDataEvent.class, GameDataAction.getInstance())
            .put(UserDroppedGameEvent.class, DropGameAction.getInstance())
            .put(GameDesynchEvent.class, GameDesynchAction.getInstance())
            .put(PlayerDesynchEvent.class, PlayerDesynchAction.getInstance())
            .put(GameInfoEvent.class, GameInfoAction.getInstance())
            .put(GameTimeoutEvent.class, GameTimeoutAction.getInstance())
            .build();

    // setup the user event handlers
    userEventHandlers =
        ImmutableMap.<Class<?>, V086UserEventHandler>builder()
            .put(ConnectedEvent.class, ACKAction.getInstance())
            .put(InfoMessageEvent.class, InfoMessageAction.getInstance())
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
        log.error("No ports are available to bind for: " + user);
      } else {
        int port = portInteger.intValue();
        log.info("Private port " + port + " allocated to: " + user);

        try {
          clientHandler.bind(port);
          boundPort = port;
          break;
        } catch (BindException e) {
          log.error("Failed to bind to port " + port + " for: " + user + ": " + e.getMessage(), e);
          log.debug(
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
      log.debug(
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
      log.error("Sleep Interrupted!", e);
      }
      }

      if (!isBound())
      {
      log.error("V086ClientHandler failed to start for client from " + getRemoteInetAddress().getHostAddress());
      return;
      }
      */

      log.debug(
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
        log.debug(this.toString() + " Stopping!");
        super.stop();

        if (port > 0) {
          log.debug(
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
        log.warn(toString() + " failed to parse: " + EmuUtil.dumpBuffer(buffer), e);
        return;
      } catch (V086BundleFormatException e) {
        buffer.rewind();
        log.warn(toString() + " received invalid message bundle: " + EmuUtil.dumpBuffer(buffer), e);
        return;
      } catch (MessageFormatException e) {
        buffer.rewind();
        log.warn(toString() + " received invalid message: " + EmuUtil.dumpBuffer(buffer), e);
        return;
      }

      // log.debug("-> " + inBundle.getNumMessages());

      if (inBundle.getNumMessages() == 0) {
        log.debug(
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
              log.error("No action defined to handle client message: " + messages[0]);
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
                    log.warn(
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
                  log.error("No action defined to handle client message: " + messages[i]);
                  continue;
                }

                // log.debug(user + " -> " + message);
                action.performAction(messages[i], this);
              }
            }
          }
        }
      } catch (FatalActionException e) {
        log.warn(toString() + " fatal action, closing connection: " + e.getMessage());
        Thread.yield();
        stop();
      }
    }

    @Override
    public void actionPerformed(KailleraEvent event) {
      if (event instanceof GameEvent) {
        V086GameEventHandler eventHandler = gameEventHandlers.get(event.getClass());
        if (eventHandler == null) {
          log.error(
              toString() + " found no GameEventHandler registered to handle game event: " + event);
          return;
        }

        eventHandler.handleEvent((GameEvent) event, this);
      } else if (event instanceof ServerEvent) {
        V086ServerEventHandler eventHandler = serverEventHandlers.get(event.getClass());
        if (eventHandler == null) {
          log.error(
              toString()
                  + " found no ServerEventHandler registered to handle server event: "
                  + event);
          return;
        }

        eventHandler.handleEvent((ServerEvent) event, this);
      } else if (event instanceof UserEvent) {
        V086UserEventHandler eventHandler = userEventHandlers.get(event.getClass());
        if (eventHandler == null) {
          log.error(
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

          log.debug(this + ": resending last " + numToSend + " messages");
          send(null, numToSend);
          lastResend = System.currentTimeMillis();
        } else {
          log.debug("Skipping resend...");
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
        //				log.debug("<- " + outBundle);
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

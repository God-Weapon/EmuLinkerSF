package org.emulinker.kaillera.controller.v086;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.configuration.*;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.v086.action.*;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.net.*;

@Singleton
public final class V086Controller implements KailleraServerController {
  static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final int MAX_BUNDLE_SIZE = 9;

  private boolean isRunning = false;

  ThreadPoolExecutor threadPool;
  KailleraServer server;
  private String[] clientTypes;
  Map<Integer, V086ClientHandler> clientHandlers =
      new ConcurrentHashMap<Integer, V086ClientHandler>();

  private int portRangeStart;
  private int extraPorts;
  Queue<Integer> portRangeQueue = new ConcurrentLinkedQueue<Integer>();

  final ImmutableMap<Class<?>, V086ServerEventHandler> serverEventHandlers;
  final ImmutableMap<Class<?>, V086GameEventHandler> gameEventHandlers;
  final ImmutableMap<Class<?>, V086UserEventHandler> userEventHandlers;

  V086Action[] actions = new V086Action[25];

  private final V086ClientHandler.Factory v086ClientHandlerFactory;
  private final RuntimeFlags flags;

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
      InfoMessageAction infoMessageAction,
      V086ClientHandler.Factory v086ClientHandlerFactory,
      RuntimeFlags flags) {
    this.v086ClientHandlerFactory = v086ClientHandlerFactory;
    this.flags = flags;
    this.threadPool = threadPool;
    this.server = server;
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

    Preconditions.checkArgument(
        flags.v086BufferSize() > 0, "controllers.v086.bufferSize must be > 0");

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
    return flags.v086BufferSize();
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

    V086ClientHandler clientHandler = v086ClientHandlerFactory.create(clientSocketAddress, this);
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
}

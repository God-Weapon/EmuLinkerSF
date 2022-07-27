package org.emulinker.kaillera.controller.v086

import com.google.common.collect.ImmutableMap
import com.google.common.flogger.FluentLogger
import java.net.InetSocketAddress
import java.net.SocketException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import org.apache.commons.configuration.Configuration
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.controller.KailleraServerController
import org.emulinker.kaillera.controller.v086.action.*
import org.emulinker.kaillera.controller.v086.protocol.*
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.event.*
import org.emulinker.kaillera.model.exception.NewConnectionException
import org.emulinker.kaillera.model.exception.ServerFullException
import org.emulinker.net.UdpSocketProvider

private val logger = FluentLogger.forEnclosingClass()

/** High level logic for handling messages on a port. Not tied to an individual user. */
@Singleton
class V086Controller
    @Inject
    internal constructor(
        override var server: KailleraServer,
        config: Configuration,
        loginAction: LoginAction,
        ackAction: ACKAction,
        chatAction: ChatAction,
        createGameAction: CreateGameAction,
        joinGameAction: JoinGameAction,
        keepAliveAction: KeepAliveAction,
        quitGameAction: QuitGameAction,
        quitAction: QuitAction,
        startGameAction: StartGameAction,
        gameChatAction: GameChatAction,
        gameKickAction: GameKickAction,
        userReadyAction: UserReadyAction,
        cachedGameDataAction: CachedGameDataAction,
        gameDataAction: GameDataAction,
        dropGameAction: DropGameAction,
        closeGameAction: CloseGameAction,
        gameStatusAction: GameStatusAction,
        gameDesynchAction: GameDesynchAction,
        playerDesynchAction: PlayerDesynchAction,
        gameInfoAction: GameInfoAction,
        gameTimeoutAction: GameTimeoutAction,
        infoMessageAction: InfoMessageAction,
        private val v086ClientHandlerFactory: V086ClientHandler.Factory,
        flags: RuntimeFlags
    ) : KailleraServerController {
  var isRunning = false
    private set

  override val clientTypes: Array<String> =
      config.getStringArray("controllers.v086.clientTypes.clientType")

  override val clientHandlers: MutableMap<Int, V086ClientHandler> = ConcurrentHashMap()

  private val portRangeStart: Int = config.getInt("controllers.v086.portRangeStart")
  private val extraPorts: Int = config.getInt("controllers.v086.extraPorts", 0)

  var portRangeQueue: Queue<Int> = ConcurrentLinkedQueue()
  val serverEventHandlers: ImmutableMap<Class<*>, V086ServerEventHandler<*>>
  val gameEventHandlers: ImmutableMap<Class<*>, V086GameEventHandler<*>>
  val userEventHandlers: ImmutableMap<Class<*>, V086UserEventHandler<*>>

  var actions: Array<V086Action<*>?> = arrayOfNulls(25)

  override val version = "v086"

  override val numClients = clientHandlers.size

  override val bufferSize = flags.v086BufferSize

  override fun toString(): String {
    return "V086Controller[clients=" + clientHandlers.size + " isRunning=" + isRunning + "]"
  }

  /**
   * Receives new connections and delegates to a new V086ClientHandler instance for communication
   * over a separate port.
   */
  @OptIn(DelicateCoroutinesApi::class) // For GlobalScope.
  @Throws(ServerFullException::class, NewConnectionException::class)
  override suspend fun newConnection(
      udpSocketProvider: UdpSocketProvider, clientSocketAddress: InetSocketAddress, protocol: String
  ): Int {
    if (!isRunning) throw NewConnectionException("Controller is not running")
    logger
        .atFine()
        .log("Creating new connection for address %d, protocol %s", clientSocketAddress, protocol)

    val clientHandler = v086ClientHandlerFactory.create(clientSocketAddress, this)
    val user = server.newConnection(clientSocketAddress, protocol, clientHandler)
    var boundPort: Int? = null
    var bindAttempts = 0
    while (bindAttempts++ < 5) {
      val portInteger = portRangeQueue.poll()
      if (portInteger == null) {
        logger.atSevere().log("No ports are available to bind for: $user")
      } else {
        val port = portInteger.toInt()
        logger.atInfo().log("Private port $port allocated to: $user")
        try {
          clientHandler.bind(udpSocketProvider, port)
          GlobalScope.launch(Dispatchers.IO) { clientHandler.run(coroutineContext) }
          boundPort = port
          break
        } catch (e: SocketException) {
          logger.atSevere().withCause(e).log("Failed to bind to port $port for: $user")
          logger
              .atFine()
              .log(
                  "$this returning port $port to available port queue: ${portRangeQueue.size + 1} available")
          portRangeQueue.add(port)
        }
      }
      // pause very briefly to give the OS a chance to free a port
      delay(5.milliseconds)
    }
    if (boundPort == null) {
      clientHandler.stop()
      throw NewConnectionException("Failed to bind!")
    }
    clientHandler.start(user)
    return boundPort
  }

  @Synchronized
  override fun start() {
    isRunning = true
  }

  @Synchronized
  override suspend fun stop() {
    isRunning = false
    clientHandlers.values.forEach { it.stop() }
    clientHandlers.clear()
  }

  companion object {
    const val MAX_BUNDLE_SIZE = 9
  }

  init {
    var maxPort = 0
    for (i in portRangeStart..portRangeStart + server.maxUsers + extraPorts) {
      portRangeQueue.add(i)
      maxPort = i
    }
    logger
        .atWarning()
        .log(
            "Listening on UDP ports: $portRangeStart to $maxPort.  Make sure these ports are open in your firewall!")

    // array access should be faster than a hash and we won't have to create
    // a new Integer each time
    actions[UserInformation.ID.toInt()] = loginAction
    actions[ClientACK.ID.toInt()] = ackAction
    actions[Chat.ID.toInt()] = chatAction
    actions[CreateGame.ID.toInt()] = createGameAction
    actions[JoinGame.ID.toInt()] = joinGameAction
    actions[KeepAlive.ID.toInt()] = keepAliveAction
    actions[QuitGame.ID.toInt()] = quitGameAction
    actions[Quit.ID.toInt()] = quitAction
    actions[StartGame.ID.toInt()] = startGameAction
    actions[GameChat.ID.toInt()] = gameChatAction
    actions[GameKick.ID.toInt()] = gameKickAction
    actions[AllReady.ID.toInt()] = userReadyAction
    actions[CachedGameData.ID.toInt()] = cachedGameDataAction
    actions[GameData.ID.toInt()] = gameDataAction
    actions[PlayerDrop.ID.toInt()] = dropGameAction

    serverEventHandlers =
        ImmutableMap.builder<Class<*>, V086ServerEventHandler<*>>()
            .put(ChatEvent::class.java, chatAction)
            .put(GameCreatedEvent::class.java, createGameAction)
            .put(UserJoinedEvent::class.java, loginAction)
            .put(GameClosedEvent::class.java, closeGameAction)
            .put(UserQuitEvent::class.java, quitAction)
            .put(GameStatusChangedEvent::class.java, gameStatusAction)
            .build()
    gameEventHandlers =
        ImmutableMap.builder<Class<*>, V086GameEventHandler<*>>()
            .put(UserJoinedGameEvent::class.java, joinGameAction)
            .put(UserQuitGameEvent::class.java, quitGameAction)
            .put(GameStartedEvent::class.java, startGameAction)
            .put(GameChatEvent::class.java, gameChatAction)
            .put(AllReadyEvent::class.java, userReadyAction)
            .put(GameDataEvent::class.java, gameDataAction)
            .put(UserDroppedGameEvent::class.java, dropGameAction)
            .put(GameDesynchEvent::class.java, gameDesynchAction)
            .put(PlayerDesynchEvent::class.java, playerDesynchAction)
            .put(GameInfoEvent::class.java, gameInfoAction)
            .put(GameTimeoutEvent::class.java, gameTimeoutAction)
            .build()
    userEventHandlers =
        ImmutableMap.builder<Class<*>, V086UserEventHandler<*>>()
            .put(ConnectedEvent::class.java, ackAction)
            .put(InfoMessageEvent::class.java, infoMessageAction)
            .build()
  }
}

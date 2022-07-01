package org.emulinker.eval.client

import com.google.common.flogger.FluentLogger
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLO
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLOD00D
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.LastMessageBuffer
import org.emulinker.kaillera.controller.v086.V086Controller
import org.emulinker.kaillera.controller.v086.protocol.*
import org.emulinker.kaillera.model.ConnectionType
import org.emulinker.util.ClientGameDataCache
import org.emulinker.util.GameDataCache

private val logger = FluentLogger.forEnclosingClass()

class EvalClient(
    private val username: String,
    private val connectControllerAddress: InetSocketAddress,
    private val simulateGameLag: Boolean = false,
    private val connectionType: ConnectionType = ConnectionType.LAN,
    private val frameDelay: Int = 1
) : Closeable {
  private val lastMessageBuffer = LastMessageBuffer(V086Controller.MAX_BUNDLE_SIZE)

  var socket: ConnectedDatagramSocket? = null

  var gameDataCache: GameDataCache = ClientGameDataCache(256)

  private val outMutex = Mutex()

  private var killSwitch = false

  var lastOutgoingMessageNumber = -1

  private var latestServerStatus: ServerStatus? = null

  private var playerNumber: Int? = null

  private val delayBetweenPackets = 1.seconds.div(connectionType.updatesPerSecond).times(frameDelay)

  /**
   * Registers as a new user with the ConnectController server and joins the dedicated port
   * allocated for the user.
   */
  suspend fun connectToDedicatedPort() {
    val selectorManager = SelectorManager(Dispatchers.IO)
    socket = aSocket(selectorManager).udp().connect(connectControllerAddress)

    val allocatedPort =
        socket?.use { connectedSocket ->
          logger.atInfo().log("Started new eval client at %s", connectedSocket.localAddress)

          sendConnectMessage(ConnectMessage_HELLO(protocol = "0.83"))

          val response = ConnectMessage.parse(connectedSocket.receive().packet.readByteBuffer())
          logger.atInfo().log("<<<<<<<< Received message: %s", response)
          require(response is ConnectMessage_HELLOD00D)

          response.port
        }
    requireNotNull(allocatedPort)

    socket =
        aSocket(selectorManager)
            .udp()
            .connect(InetSocketAddress(connectControllerAddress.hostname, allocatedPort))
    logger.atInfo().log("Changing connection to: %s", socket!!.remoteAddress)
  }

  /** Interacts in the server */
  @OptIn(DelicateCoroutinesApi::class) // GlobalScope.
  suspend fun start() {

    GlobalScope.launch(Dispatchers.IO) {
      while (!killSwitch) {
        try {
          val response = V086Bundle.parse(socket!!.receive().packet.readByteBuffer())
          handleIncoming(response)
        } catch (e: ParseException) {

          if (e.message?.contains("Failed byte count validation") == true &&
              e.stackTrace.firstOrNull()?.fileName == "PlayerInformation.kt") {
            // TODO(nue): There's a PlayerInformation parsing failure here and I don't understand..
            // We need to figure out what's going on, but for now log and continue.
            logger.atSevere().withCause(e).log("Failed to parse the PlayerInformation message!")
          } else {
            throw e
          }
        }
      }
      logger.atInfo().log("EvalClient shut down.")
    }

    sendWithMessageId {
      UserInformation(
          messageNumber = it, username, "Project 64k 0.13 (01 Aug 2003)", connectionType)
    }
  }

  private suspend fun handleIncoming(bundle: V086Bundle) {
    val message = bundle.messages.first()

    logger.atInfo().log("<<<<<<<< Received message: %s", message)

    when (message) {
      is ServerACK -> {
        sendWithMessageId { ClientACK(messageNumber = it) }
      }
      is ServerStatus -> {
        latestServerStatus = message
      }
      is InformationMessage -> {}
      is UserJoined -> {}
      is CreateGame_Notification -> {}
      is GameStatus -> {}
      is PlayerInformation -> {}
      is JoinGame_Notification -> {}
      is AllReady -> {
        if (simulateGameLag) {
          delay(delayBetweenPackets)
        }
        sendWithMessageId {
          GameData(
              messageNumber = it,
              gameData =
                  when (playerNumber) {
                    1 -> {
                      byteArrayOf(
                          16, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0)
                    }
                    2 -> {
                      byteArrayOf(
                          17, 32, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    }
                    else -> {
                      logger.atSevere().log("Unexpected message type: %s", message)
                      throw IllegalStateException()
                    }
                  })
        }
      }
      is GameData -> {
        val index = gameDataCache.indexOf(message.gameData)
        if (index < 0) {
          gameDataCache.add(message.gameData)
        }
        if (simulateGameLag) {
          delay(delayBetweenPackets)
        }
        sendWithMessageId {
          GameData(
              messageNumber = it,
              gameData =
                  when (playerNumber) {
                    1 -> {
                      byteArrayOf(
                          16, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0)
                    }
                    2 -> {
                      byteArrayOf(
                          17, 32, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    }
                    else -> {
                      logger.atSevere().log("Unexpected message type: %s", message)
                      throw IllegalStateException()
                    }
                  })
        }
      }
      is CachedGameData -> {
        require(gameDataCache[message.key] != null)
        if (simulateGameLag) {
          delay(delayBetweenPackets)
        }
        sendWithMessageId {
          GameData(
              messageNumber = it,
              gameData =
                  when (playerNumber) {
                    1 -> {
                      byteArrayOf(
                          16, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0)
                    }
                    2 -> {
                      byteArrayOf(
                          17, 32, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                    }
                    else -> {
                      logger.atSevere().log("Unexpected message type: %s", message)
                      throw IllegalStateException()
                    }
                  })
        }
      }
      is StartGame_Notification -> {
        playerNumber = message.playerNumber.toInt()
        delay(1.seconds)
        sendWithMessageId { AllReady(messageNumber = it) }
      }
      else -> {
        logger.atSevere().log("Unexpected message type: %s", message)
      }
    }
  }

  suspend fun createGame() {
    sendWithMessageId {
      CreateGame_Request(
          messageNumber = it, romName = "Nintendo All-Star! Dairantou Smash Brothers (J)")
    }
  }

  suspend fun startOwnGame() {
    sendWithMessageId { StartGame_Request(messageNumber = it) }
  }

  suspend fun joinAnyAvailableGame() {
    // TODO(nue): Make it listen to individual game creation updates too.
    val games = requireNotNull(latestServerStatus?.games)
    sendWithMessageId {
      JoinGame_Request(messageNumber = it, gameId = games.first().gameId, connectionType)
    }
  }

  override fun close() {
    logger.atInfo().log("Shutting down EvalClient.")
    killSwitch = true
    socket?.close()
  }

  private suspend fun sendConnectMessage(message: ConnectMessage) {
    socket!!.send(Datagram(ByteReadPacket(message.toBuffer()!!), socket!!.remoteAddress))
  }

  private suspend fun sendWithMessageId(messageIdToMessage: (messageNumber: Int) -> V086Message) {
    outMutex.withLock {
      lastOutgoingMessageNumber++
      val messageAsArray: Array<V086Message?> =
          arrayOf(messageIdToMessage(lastOutgoingMessageNumber))

      val outBuffer = ByteBuffer.allocateDirect(4096)
      lastMessageBuffer.fill(messageAsArray, messageAsArray.size)
      val outBundle = V086Bundle(messageAsArray, messageAsArray.size)
      outBundle.writeTo(outBuffer)
      (outBuffer as Buffer).flip()
      logger.atInfo().log(">>>>>>>> SENT message: %s", outBundle.messages.first())
      socket!!.send(Datagram(ByteReadPacket(outBuffer), socket!!.remoteAddress))
    }
  }

  suspend fun dropGame() {
    sendWithMessageId { PlayerDrop_Request(messageNumber = it) }
  }

  suspend fun quitGame() {
    sendWithMessageId { QuitGame_Request(messageNumber = it) }
  }

  suspend fun quitServer() {
    sendWithMessageId { Quit_Request(messageNumber = it, message = "End of test.") }
  }
}

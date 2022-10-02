package org.emulinker.kaillera.controller.connectcontroller

import com.google.common.flogger.FluentLogger
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.Set as JavaSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.configuration.Configuration
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.KailleraServerController
import org.emulinker.kaillera.controller.connectcontroller.protocol.*
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage.Companion.parse
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage.Companion.getBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.model.exception.NewConnectionException
import org.emulinker.kaillera.model.exception.ServerFullException
import org.emulinker.net.UDPServer
import org.emulinker.net.UdpSocketProvider
import org.emulinker.util.EmuUtil.dumpBuffer
import org.emulinker.util.EmuUtil.formatSocketAddress

private val logger = FluentLogger.forEnclosingClass()

/**
 * The UDP Server implementation.
 *
 * This is the main server for new connections (usually on 27888).
 */
@Singleton
class ConnectController
    @Inject
    internal constructor(
        // TODO(nue): This makes no sense because KailleraServerController is a singleton...
        kailleraServerControllers: JavaSet<KailleraServerController>,
        private val accessManager: AccessManager,
        private val config: Configuration,
        flags: RuntimeFlags,
    ) : UDPServer() {

  private val mutex = Mutex()

  private val controllersMap: MutableMap<String, KailleraServerController> = HashMap()

  override val bufferSize = flags.connectControllerBufferSize

  private var internalBufferSize = 0
  var startTime: Long = 0
    private set
  var requestCount = 0
    private set
  var messageFormatErrorCount = 0
    private set
  var protocolErrorCount = 0
    private set
  var deniedServerFullCount = 0
    private set
  var deniedOtherCount = 0
    private set
  private var lastAddress: String? = null
  private var lastAddressCount = 0
  var failedToStartCount = 0
    private set
  private var connectCount = 0
  var pingCount = 0
    private set

  private lateinit var udpSocketProvider: UdpSocketProvider

  private fun getController(clientType: String?): KailleraServerController? {
    return controllersMap[clientType]
  }

  val controllers: Collection<KailleraServerController>
    get() = controllersMap.values

  override fun allocateBuffer(): ByteBuffer {
    return getBuffer(internalBufferSize)
  }

  override fun toString(): String =
      if (bindPort > 0) "ConnectController($bindPort)" else "ConnectController(unbound)"

  override suspend fun start(
      udpSocketProvider: UdpSocketProvider, globalContext: CoroutineContext
  ) {
    this.udpSocketProvider = udpSocketProvider
    this.globalContext = globalContext
    val port = config.getInt("controllers.connect.port")
    startTime = System.currentTimeMillis()

    super.bind(udpSocketProvider, port)
    this.run(globalContext)
  }

  override suspend fun stop() {
    mutex.withLock {
      super.stop()
      for (controller in controllersMap.values) controller.stop()
    }
  }

  override suspend fun handleReceived(
      buffer: ByteBuffer, remoteSocketAddress: InetSocketAddress, requestScope: CoroutineScope
  ) {
    requestCount++
    val formattedSocketAddress = formatSocketAddress(remoteSocketAddress)
    // TODO(nue): Remove this catch logic.
    val inMessage: ConnectMessage =
        try {
          parse(buffer)
        } catch (e: MessageFormatException) {
          messageFormatErrorCount++
          buffer.rewind()
          logger
              .atWarning()
              .log("Received invalid message from $formattedSocketAddress: ${dumpBuffer(buffer)}")
          return
        } catch (e: IllegalArgumentException) {
          messageFormatErrorCount++
          buffer.rewind()
          logger
              .atWarning()
              .log("Received invalid message from $formattedSocketAddress: ${dumpBuffer(buffer)}")
          return
        }

    logger.atFinest().log("-> FROM %s: %s", formattedSocketAddress, inMessage)

    // the message set of the ConnectController isn't really complex enough to warrant a complicated
    // request/action class
    // structure, so I'm going to handle it  all in this class alone
    if (inMessage is ConnectMessage_PING) {
      pingCount++
      logger.atFine().log("Ping from: $formattedSocketAddress")
      send(ConnectMessage_PONG(), remoteSocketAddress)
      return
    }
    if (inMessage !is ConnectMessage_HELLO) {
      messageFormatErrorCount++
      logger
          .atWarning()
          .log("Received unexpected message type from $formattedSocketAddress: $inMessage")
      return
    }

    // now we need to find the specific server this client is request to
    // connect to using the client type
    val protocolController = getController(inMessage.protocol)
    if (protocolController == null) {
      protocolErrorCount++
      logger
          .atSevere()
          .log(
              "Client requested an unhandled protocol $formattedSocketAddress: ${inMessage.protocol}")
      return
    }
    if (!accessManager.isAddressAllowed(remoteSocketAddress.address)) {
      deniedOtherCount++
      logger.atWarning().log("AccessManager denied connection from $formattedSocketAddress")
      return
    } else {
      val privatePort: Int
      val access = accessManager.getAccess(remoteSocketAddress.address)
      try {
        mutex.withLock {
          // SF MOD - Hammer Protection
          if (access < AccessManager.ACCESS_ADMIN && connectCount > 0) {
            if (lastAddress == remoteSocketAddress.address.hostAddress) {
              lastAddressCount++
              if (lastAddressCount >= 4) {
                lastAddressCount = 0
                failedToStartCount++
                logger
                    .atFine()
                    .log("SF MOD: HAMMER PROTECTION (2 Min Ban): $formattedSocketAddress")
                accessManager.addTempBan(remoteSocketAddress.address.hostAddress, 2.minutes)
                return
              }
            } else {
              lastAddress = remoteSocketAddress.address.hostAddress
              lastAddressCount = 0
            }
          } else lastAddress = remoteSocketAddress.address.hostAddress
          privatePort =
              protocolController.newConnection(
                  udpSocketProvider, remoteSocketAddress, inMessage.protocol)
          if (privatePort <= 0) {
            failedToStartCount++
            logger.atSevere().log("$protocolController failed to start for $formattedSocketAddress")
            return
          }
          connectCount++
          logger
              .atFine()
              .log(
                  "$protocolController allocated port $privatePort to client from ${remoteSocketAddress.address.hostAddress}")
          send(ConnectMessage_HELLOD00D(privatePort), remoteSocketAddress)
        }
      } catch (e: ServerFullException) {
        deniedServerFullCount++
        logger.atFine().withCause(e).log("Sending server full response to $formattedSocketAddress")
        send(ConnectMessage_TOO(), remoteSocketAddress)
        return
      } catch (e: NewConnectionException) {
        deniedOtherCount++
        logger
            .atWarning()
            .withCause(e)
            .log("$protocolController denied connection from $formattedSocketAddress")
        return
      }
    }
  }

  private suspend fun send(outMessage: ConnectMessage, toSocketAddress: InetSocketAddress) {
    logger.atFinest().log("<- TO %s: %s", formatSocketAddress(toSocketAddress), outMessage)

    send(outMessage.toBuffer(), toSocketAddress)
    outMessage.releaseBuffer()
  }

  init {
    kailleraServerControllers.forEach { controller ->
      controller.clientTypes.forEach { type ->
        logger.atFine().log("Mapping client type $type to $controller")
        controllersMap[type] = controller
      }
    }
  }
}

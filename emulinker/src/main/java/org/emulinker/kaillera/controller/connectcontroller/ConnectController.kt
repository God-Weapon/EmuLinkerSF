package org.emulinker.kaillera.controller.connectcontroller

import com.codahale.metrics.MetricRegistry
import com.google.common.base.Preconditions
import com.google.common.flogger.FluentLogger
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Inject
import javax.inject.Singleton
import org.apache.commons.configuration.Configuration
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.KailleraServerController
import org.emulinker.kaillera.controller.connectcontroller.protocol.*
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage.Companion.parse
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage.Companion.getBuffer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.model.exception.NewConnectionException
import org.emulinker.kaillera.model.exception.ServerFullException
import org.emulinker.net.BindException
import org.emulinker.net.UDPServer
import org.emulinker.util.EmuUtil.dumpBuffer
import org.emulinker.util.EmuUtil.formatSocketAddress

private val logger = FluentLogger.forEnclosingClass()

/** The UDP Server implementation. */
@Singleton
class ConnectController
    @Inject
    internal constructor(
        private val threadPool: ThreadPoolExecutor,
        kailleraServerControllers: java.util.Set<KailleraServerController>,
        private val accessManager: AccessManager,
        config: Configuration,
        metrics: MetricRegistry?
    ) : UDPServer(/* shutdownOnExit= */ true, metrics) {

  private val controllersMap: MutableMap<String?, KailleraServerController>

  var bufferSize = 0
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
  var connectCount = 0
    private set
  var pingCount = 0
    private set

  fun getController(clientType: String?): KailleraServerController? {
    return controllersMap[clientType]
  }

  val controllers: Collection<KailleraServerController>
    get() = controllersMap.values
  override val buffer: ByteBuffer
    protected get() = getBuffer(bufferSize)

  override fun releaseBuffer(buffer: ByteBuffer) {
    ByteBufferMessage.releaseBuffer(buffer)
  }

  override fun toString(): String {
    // return "ConnectController[port=" + getBindPort() + " isRunning=" + isRunning() + "]";
    // return "ConnectController[port=" + getBindPort() + "]";
    return if (bindPort > 0) "ConnectController($bindPort)" else "ConnectController(unbound)"
  }

  @Synchronized
  override fun start() {
    startTime = System.currentTimeMillis()
    logger
        .atFine()
        .log(
            toString() +
                " Thread starting (ThreadPool:" +
                threadPool.activeCount +
                "/" +
                threadPool.poolSize +
                ")")
    threadPool.execute(this)
    Thread.yield()
    logger
        .atFine()
        .log(
            toString() +
                " Thread started (ThreadPool:" +
                threadPool.activeCount +
                "/" +
                threadPool.poolSize +
                ")")
  }

  @Synchronized
  override fun stop() {
    super.stop()
    for (controller in controllersMap.values) controller.stop()
  }

  @Synchronized
  override fun handleReceived(buffer: ByteBuffer, fromSocketAddress: InetSocketAddress) {
    requestCount++
    var inMessage: ConnectMessage? = null
    inMessage =
        try {
          parse(buffer)
        } catch (e: Exception) {
          when (e) {
            is MessageFormatException, is IllegalArgumentException -> {
              messageFormatErrorCount++
              buffer.rewind()
              logger
                  .atWarning()
                  .log(
                      "Received invalid message from " +
                          formatSocketAddress(fromSocketAddress) +
                          ": " +
                          dumpBuffer(buffer))
              return
            }
            else -> throw e
          }
        }

    // the message set of the ConnectController isn't really complex enough to warrant a complicated
    // request/action class
    // structure, so I'm going to handle it  all in this class alone
    if (inMessage is ConnectMessage_PING) {
      pingCount++
      logger.atFine().log("Ping from: " + formatSocketAddress(fromSocketAddress))
      send(ConnectMessage_PONG(), fromSocketAddress)
      return
    }
    if (inMessage !is ConnectMessage_HELLO) {
      messageFormatErrorCount++
      logger
          .atWarning()
          .log(
              "Received unexpected message type from " +
                  formatSocketAddress(fromSocketAddress) +
                  ": " +
                  inMessage)
      return
    }
    val connectMessage = inMessage

    // now we need to find the specific server this client is request to
    // connect to using the client type
    val protocolController = getController(connectMessage.protocol)
    if (protocolController == null) {
      protocolErrorCount++
      logger
          .atSevere()
          .log(
              "Client requested an unhandled protocol " +
                  formatSocketAddress(fromSocketAddress) +
                  ": " +
                  connectMessage.protocol)
      return
    }
    if (!accessManager.isAddressAllowed(fromSocketAddress.address)) {
      deniedOtherCount++
      logger
          .atWarning()
          .log("AccessManager denied connection from " + formatSocketAddress(fromSocketAddress))
      return
    } else {
      var privatePort = -1
      val access = accessManager.getAccess(fromSocketAddress.address)
      try {
        // SF MOD - Hammer Protection
        if (access < AccessManager.ACCESS_ADMIN && connectCount > 0) {
          if (lastAddress == fromSocketAddress.address.hostAddress) {
            lastAddressCount++
            if (lastAddressCount >= 4) {
              lastAddressCount = 0
              failedToStartCount++
              logger
                  .atFine()
                  .log(
                      "SF MOD: HAMMER PROTECTION (2 Min Ban): " +
                          formatSocketAddress(fromSocketAddress))
              accessManager.addTempBan(fromSocketAddress.address.hostAddress, 2)
              return
            }
          } else {
            lastAddress = fromSocketAddress.address.hostAddress
            lastAddressCount = 0
          }
        } else lastAddress = fromSocketAddress.address.hostAddress
        privatePort = protocolController.newConnection(fromSocketAddress, connectMessage.protocol)
        if (privatePort <= 0) {
          failedToStartCount++
          logger
              .atSevere()
              .log(
                  protocolController.toString() +
                      " failed to start for " +
                      formatSocketAddress(fromSocketAddress))
          return
        }
        connectCount++
        logger
            .atFine()
            .log(
                protocolController.toString() +
                    " allocated port " +
                    privatePort +
                    " to client from " +
                    fromSocketAddress.address.hostAddress)
        send(ConnectMessage_HELLOD00D(privatePort), fromSocketAddress)
      } catch (e: ServerFullException) {
        deniedServerFullCount++
        logger
            .atFine()
            .withCause(e)
            .log("Sending server full response to " + formatSocketAddress(fromSocketAddress))
        send(ConnectMessage_TOO(), fromSocketAddress)
        return
      } catch (e: NewConnectionException) {
        deniedOtherCount++
        logger
            .atWarning()
            .withCause(e)
            .log(
                protocolController.toString() +
                    " denied connection from " +
                    formatSocketAddress(fromSocketAddress))
        return
      }
    }
  }

  protected fun send(outMessage: ConnectMessage, toSocketAddress: InetSocketAddress?) {
    send(outMessage.toBuffer(), toSocketAddress)
    outMessage.releaseBuffer()
  }

  init {
    val port = config.getInt("controllers.connect.port")
    bufferSize = config.getInt("controllers.connect.bufferSize")
    Preconditions.checkArgument(bufferSize > 0, "controllers.connect.bufferSize must be > 0")
    controllersMap = HashMap()
    for (controller in kailleraServerControllers) {
      val clientTypes = controller.clientTypes
      for (j in clientTypes.indices) {
        logger.atFine().log("Mapping client type " + clientTypes[j] + " to " + controller)
        controllersMap[clientTypes[j]] = controller
      }
    }
    try {
      super.bind(port)
    } catch (e: BindException) {
      throw IllegalStateException(e)
    }
    logger.atInfo().log("Ready to accept connections on port $port")
  }
}

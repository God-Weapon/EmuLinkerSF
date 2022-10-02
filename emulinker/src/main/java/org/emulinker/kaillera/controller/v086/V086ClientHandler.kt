package org.emulinker.kaillera.controller.v086

import com.codahale.metrics.MetricRegistry
import com.google.common.flogger.FluentLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit.MINUTES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.action.*
import org.emulinker.kaillera.controller.v086.protocol.V086Bundle
import org.emulinker.kaillera.controller.v086.protocol.V086Bundle.Companion.parse
import org.emulinker.kaillera.controller.v086.protocol.V086BundleFormatException
import org.emulinker.kaillera.controller.v086.protocol.V086Message
import org.emulinker.kaillera.model.KailleraUser
import org.emulinker.kaillera.model.event.*
import org.emulinker.net.UDPServer
import org.emulinker.net.UdpSocketProvider
import org.emulinker.util.ClientGameDataCache
import org.emulinker.util.EmuUtil.dumpBuffer
import org.emulinker.util.EmuUtil.dumpBufferFromBeginning
import org.emulinker.util.EmuUtil.formatSocketAddress
import org.emulinker.util.GameDataCache

private val logger = FluentLogger.forEnclosingClass()

/** A private UDP server allocated for communication with a single client. */
class V086ClientHandler
    @AssistedInject
    constructor(
        metrics: MetricRegistry,
        private val flags: RuntimeFlags,
        @Assisted remoteSocketAddress: InetSocketAddress,
        /** The V086Controller that started this client handler. */
        @param:Assisted val controller: V086Controller
    ) : UDPServer(), KailleraEventListener {
  lateinit var user: KailleraUser
    private set

  private val mutex = Mutex()

  private var messageNumberCounter = 0

  // TODO(nue): Add this to RuntimeFlags and increase to at least 5.
  val numAcksForSpeedTest = 3

  private var prevMessageNumber = -1
    private set
  var lastMessageNumber = -1
    private set
  var clientGameDataCache: GameDataCache = ClientGameDataCache(256)
    private set
  var serverGameDataCache: GameDataCache = ClientGameDataCache(256)
    private set

  private val lastMessageBuffer = LastMessageBuffer(V086Controller.MAX_BUNDLE_SIZE)
  private val outMessages = arrayOfNulls<V086Message>(V086Controller.MAX_BUNDLE_SIZE)
  private val inBuffer: ByteBuffer = ByteBuffer.allocateDirect(flags.v086BufferSize)
  private val outBuffer: ByteBuffer = ByteBuffer.allocateDirect(flags.v086BufferSize)
  private val inMutex = Mutex()
  private val outMutex = Mutex()
  private var testStart: Long = 0
  private var lastMeasurement: Long = 0
  var speedMeasurementCount = 0
    private set
  var bestNetworkSpeed = Int.MAX_VALUE
    private set
  private var clientRetryCount = 0
  private var lastResend: Long = 0

  private val clientRequestTimer =
      metrics.timer(MetricRegistry.name(this.javaClass, "clientRequests"))

  lateinit var remoteSocketAddress: InetSocketAddress
    private set

  val remoteInetAddress: InetAddress = remoteSocketAddress.address

  @AssistedFactory
  interface Factory {
    fun create(
        remoteSocketAddress: InetSocketAddress?, v086Controller: V086Controller?
    ): V086ClientHandler
  }

  override suspend fun handleReceived(
      buffer: ByteBuffer, remoteSocketAddress: InetSocketAddress, requestScope: CoroutineScope
  ) {
    if (!this::remoteSocketAddress.isInitialized) {
      this.remoteSocketAddress = remoteSocketAddress
    } else if (remoteSocketAddress != this.remoteSocketAddress) {
      logger
          .atWarning()
          .log(
              "Rejecting packet received from wrong address. Expected=%s but was %s",
              formatSocketAddress(this.remoteSocketAddress),
              formatSocketAddress(remoteSocketAddress))

      return
    }
    clientRequestTimer.time().use {
      try {
        withTimeout(flags.requestTimeout) { handleReceived(buffer) }
      } catch (e: TimeoutCancellationException) {
        logger.atSevere().withCause(e).log("Request timed out")
      }
    }
  }

  private suspend fun send(buffer: ByteBuffer) {
    super.send(buffer, remoteSocketAddress)
  }

  override fun toString(): String {
    return if (bindPort > 0) "V086Controller($bindPort)" else "V086Controller(unbound)"
  }

  @get:Synchronized
  val nextMessageNumber: Int
    get() {
      if (messageNumberCounter > 0xFFFF) messageNumberCounter = 0
      return messageNumberCounter++
    }

  fun resetGameDataCache() {
    clientGameDataCache = ClientGameDataCache(256)
    /*SF MOD - Button Ghosting Patch
    serverCache = new ServerGameDataCache(256);
    */
    serverGameDataCache = ClientGameDataCache(256)
  }

  fun startSpeedTest() {
    lastMeasurement = System.currentTimeMillis()
    testStart = lastMeasurement
    speedMeasurementCount = 0
  }

  fun addSpeedMeasurement() {
    val et = (System.currentTimeMillis() - lastMeasurement).toInt()
    if (et < bestNetworkSpeed) bestNetworkSpeed = et
    speedMeasurementCount++
    lastMeasurement = System.currentTimeMillis()
  }

  val averageNetworkSpeed: Int
    get() = ((lastMeasurement - testStart) / speedMeasurementCount).toInt()

  public override fun bind(udpSocketProvider: UdpSocketProvider, port: Int) {
    super.bind(udpSocketProvider, port)
  }

  fun start(user: KailleraUser) {
    this.user = user

    //    controller.threadPool.execute(this) // NUEFIXME

    /*
    long s = System.currentTimeMillis();
    while (!isBound() && (System.currentTimeMillis() - s) < 1000)
    {
    try
    {
    delay(100.milliseconds);
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
    controller.clientHandlers[user.id] = this
  }

  override suspend fun stop() {
    mutex.withLock {
      logger.atFine().log("Stopping ClientHandler for %d", user.id)
      if (stopFlag) return
      var port = -1
      if (isBound) port = bindPort
      super.stop()
      if (port > 0) {
        logger
            .atFine()
            .log(
                "%s returning port %d to available port queue: %d available",
                this,
                port,
                controller.portRangeQueue.size + 1)
        controller.portRangeQueue.add(port)
      }
    }
    controller.clientHandlers.remove(user.id)
    user.stop()
  }

  override fun allocateBuffer(): ByteBuffer {
    // return ByteBufferMessage.getBuffer(bufferSize);
    // Cast to avoid issue with java version mismatch:
    // https://stackoverflow.com/a/61267496/2875073
    (inBuffer as Buffer).clear()
    return inBuffer
  }

  private suspend fun handleReceived(buffer: ByteBuffer) {
    inMutex.withLock {
      val lastMessageNumberUsed = lastMessageNumber
      val inBundle =
          try {
            parse(buffer, lastMessageNumber)
          } catch (e: ParseException) {
            buffer.rewind()
            logger.atWarning().withCause(e).log("$this failed to parse: ${dumpBuffer(buffer)}")
            null
          } catch (e: V086BundleFormatException) {
            buffer.rewind()
            logger
                .atWarning()
                .withCause(e)
                .log("$this received invalid message bundle: ${dumpBuffer(buffer)}")
            null
          } catch (e: MessageFormatException) {
            buffer.rewind()
            logger
                .atWarning()
                .withCause(e)
                .log("$this received invalid message: ${dumpBuffer(buffer)}")
            null
          } ?: return

      if (inBundle.messages.firstOrNull() == null) {
        logger
            .atWarning()
            .atMostEvery(1, MINUTES)
            .log(
                "Received request from User %d containing no messages. inBundle.messages.size = %d. numMessages: %d, buffer dump: %s, lastMessageNumberUsed: $lastMessageNumberUsed",
                user.id,
                inBundle.messages.size,
                inBundle.numMessages,
                lazy { dumpBufferFromBeginning(buffer) })
      }

      logger.atFinest().log("-> FROM user %d: %s", user.id, inBundle.messages.firstOrNull())
      clientRetryCount =
          if (inBundle.numMessages == 0) {
            logger
                .atFine()
                .log("%s received bundle of %d messages from %s", this, inBundle.numMessages, user)
            clientRetryCount++
            resend(clientRetryCount)
            return
          } else {
            0
          }
      try {
        val messages = inBundle.messages
        if (inBundle.numMessages == 1) {
          lastMessageNumber = messages.single()!!.messageNumber
          val action = controller.actions[messages[0]!!.messageId.toInt()]
          if (action == null) {
            logger.atSevere().log("No action defined to handle client message: " + messages[0])
          }
          (action as V086Action<V086Message>).performAction(messages[0]!!, this)
        } else {
          // read the bundle from back to front to process the oldest messages first
          for (i in inBundle.numMessages - 1 downTo 0) {
            /**
             * already extracts messages with higher numbers when parsing, it does not need to be
             * checked and this causes an error if messageNumber is 0 and lastMessageNumber is
             * 0xFFFF if (messages [i].getNumber() > lastMessageNumber)
             */
            prevMessageNumber = lastMessageNumber
            lastMessageNumber = messages[i]!!.messageNumber
            if (prevMessageNumber + 1 != lastMessageNumber) {
              if (prevMessageNumber == 0xFFFF && lastMessageNumber == 0) {
                // exception; do nothing
              } else {
                logger
                    .atWarning()
                    .log("$user dropped a packet! ($prevMessageNumber to $lastMessageNumber)")
                user.droppedPacket()
              }
            }
            val action = controller.actions[messages[i]!!.messageId.toInt()]
            if (action == null) {
              logger.atSevere().log("No action defined to handle client message: " + messages[i])
            } else {
              // logger.atFine().log(user + " -> " + message);
              (action as V086Action<V086Message>).performAction(messages[i]!!, this)
            }
          }
        }
      } catch (e: FatalActionException) {
        logger.atWarning().withCause(e).log(toString() + " fatal action, closing connection")
        stop()
      }
    }
  }

  override val bufferSize = flags.v086BufferSize

  override suspend fun actionPerformed(event: KailleraEvent) {
    when (event) {
      is GameEvent -> {
        val eventHandler = controller.gameEventHandlers[event::class]
        if (eventHandler == null) {
          logger
              .atSevere()
              .log("%s found no GameEventHandler registered to handle game event: %s", this, event)
          return
        }
        (eventHandler as V086GameEventHandler<GameEvent>).handleEvent(event, this)
      }
      is ServerEvent -> {
        val eventHandler = controller.serverEventHandlers[event::class]
        if (eventHandler == null) {
          logger
              .atSevere()
              .log(
                  "%s found no ServerEventHandler registered to handle server event: %s",
                  this,
                  event)
          return
        }
        (eventHandler as V086ServerEventHandler<ServerEvent>).handleEvent(event, this)
      }
      is UserEvent -> {
        val eventHandler = controller.userEventHandlers[event::class]
        if (eventHandler == null) {
          logger
              .atSevere()
              .log("%s found no UserEventHandler registered to handle user event: ", this, event)
          return
        }
        (eventHandler as V086UserEventHandler<UserEvent>).handleEvent(event, this)
      }
      is StopFlagEvent -> {}
    }
  }

  suspend fun resend(timeoutCounter: Int) {
    // TODO(nue): Confirm it's safe to remove this.
    //    outMutex.withLock {
    // if ((System.currentTimeMillis() - lastResend) > (user.getPing()*3))
    if (System.currentTimeMillis() - lastResend > controller.server.maxPing) {
      // int numToSend = (3+timeoutCounter);
      var numToSend = 3 * timeoutCounter
      if (numToSend > V086Controller.MAX_BUNDLE_SIZE) numToSend = V086Controller.MAX_BUNDLE_SIZE
      logger.atFine().log("$this: resending last $numToSend messages")
      send(null, numToSend)
      lastResend = System.currentTimeMillis()
    } else {
      logger.atFine().log("Skipping resend...")
    }
    //    }
  }

  suspend fun send(outMessage: V086Message?, numToSend: Int = 5) {
    var numToSend = numToSend
    outMutex.withLock {
      if (outMessage != null) {
        lastMessageBuffer.add(outMessage)
      }
      numToSend = lastMessageBuffer.fill(outMessages, numToSend)
      // System.out.println("Server -> " + numToSend);
      val outBundle = V086Bundle(outMessages, numToSend)
      logger.atFinest().log("<- TO P%d: %s", user.playerNumber, outMessage)
      outBundle.writeTo(outBuffer)
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      (outBuffer as Buffer).flip()
      send(outBuffer)
      (outBuffer as Buffer).clear()
    }
  }

  init {
    inBuffer.order(ByteOrder.LITTLE_ENDIAN)
    outBuffer.order(ByteOrder.LITTLE_ENDIAN)
    resetGameDataCache()
  }
}

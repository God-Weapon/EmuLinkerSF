package org.emulinker.kaillera.controller.v086

import com.codahale.metrics.MetricRegistry
import com.google.common.flogger.FluentLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.net.InetSocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
import org.emulinker.net.BindException
import org.emulinker.net.PrivateUDPServer
import org.emulinker.util.ClientGameDataCache
import org.emulinker.util.EmuUtil.dumpBuffer
import org.emulinker.util.GameDataCache

private val logger = FluentLogger.forEnclosingClass()

class V086ClientHandler
    @AssistedInject
    constructor(
        metrics: MetricRegistry?,
        flags: RuntimeFlags,
        @Assisted remoteSocketAddress: InetSocketAddress,
        @param:Assisted val controller: V086Controller
    ) : PrivateUDPServer(false, remoteSocketAddress.address, metrics!!), KailleraEventListener {
  var user: KailleraUser? = null
    private set

  private var messageNumberCounter = 0

  /*
  public List<V086Message> getLastMessage()
  {
  return lastMessages;
  }
  */ var prevMessageNumber = -1
    private set
  var lastMessageNumber = -1
    private set
  var clientGameDataCache: GameDataCache = ClientGameDataCache(256)
    private set
  var serverGameDataCache: GameDataCache = ClientGameDataCache(256)
    private set

  // private LinkedList<V086Message>	lastMessages			= new LinkedList<V086Message>();
  private val lastMessageBuffer = LastMessageBuffer(V086Controller.MAX_BUNDLE_SIZE)
  private val outMessages = arrayOfNulls<V086Message>(V086Controller.MAX_BUNDLE_SIZE)
  private val inBuffer: ByteBuffer = ByteBuffer.allocateDirect(flags.v086BufferSize)
  private val outBuffer: ByteBuffer = ByteBuffer.allocateDirect(flags.v086BufferSize)
  private val inSynch = Any()
  private val outSynch = Any()
  private var testStart: Long = 0
  private var lastMeasurement: Long = 0
  var speedMeasurementCount = 0
    private set
  var bestNetworkSpeed = Int.MAX_VALUE
    private set
  private var clientRetryCount = 0
  private var lastResend: Long = 0

  @AssistedFactory
  interface Factory {
    fun create(
        remoteSocketAddress: InetSocketAddress?, v086Controller: V086Controller?
    ): V086ClientHandler
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

  @Throws(BindException::class)
  public override fun bind(port: Int) {
    super.bind(port)
  }

  fun start(user: KailleraUser) {
    this.user = user
    logger
        .atFine()
        .log(
            toString() +
                " thread starting (ThreadPool:" +
                controller.threadPool.activeCount +
                "/" +
                controller.threadPool.poolSize +
                ")")
    controller.threadPool.execute(this)
    Thread.yield()

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
    */ logger
        .atFine()
        .log(
            toString() +
                " thread started (ThreadPool:" +
                controller.threadPool.activeCount +
                "/" +
                controller.threadPool.poolSize +
                ")")
    controller.clientHandlers[user.id] = this
  }

  override fun stop() {
    synchronized(this) {
      if (stopFlag) return
      var port = -1
      if (isBound) port = bindPort
      logger.atFine().log("$this Stopping!")
      super.stop()
      if (port > 0) {
        logger
            .atFine()
            .log(
                toString() +
                    " returning port " +
                    port +
                    " to available port queue: " +
                    (controller.portRangeQueue.size + 1) +
                    " available")
        controller.portRangeQueue.add(port)
      }
    }
    if (user != null) {
      controller.clientHandlers.remove(user!!.id)
      user!!.stop()
      user = null
    }
  }

  // return ByteBufferMessage.getBuffer(bufferSize);
  // Cast to avoid issue with java version mismatch:
  // https://stackoverflow.com/a/61267496/2875073
  override val buffer: ByteBuffer
    protected get() {
      // return ByteBufferMessage.getBuffer(bufferSize);
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      (inBuffer as Buffer).clear()
      return inBuffer
    }

  override fun releaseBuffer(buffer: ByteBuffer) {
    // ByteBufferMessage.releaseBuffer(buffer);
    // buffer.clear();
  }

  override fun handleReceived(buffer: ByteBuffer) {
    var inBundle: V086Bundle? = null
    inBundle =
        try {
          parse(buffer, lastMessageNumber)
          // inBundle = V086Bundle.parse(buffer, -1);
        } catch (e: ParseException) {
          buffer.rewind()
          logger
              .atWarning()
              .withCause(e)
              .log(toString() + " failed to parse: " + dumpBuffer(buffer))
          return
        } catch (e: V086BundleFormatException) {
          buffer.rewind()
          logger
              .atWarning()
              .withCause(e)
              .log(toString() + " received invalid message bundle: " + dumpBuffer(buffer))
          return
        } catch (e: MessageFormatException) {
          buffer.rewind()
          logger
              .atWarning()
              .withCause(e)
              .log(toString() + " received invalid message: " + dumpBuffer(buffer))
          return
        }

    // logger.atFine().log("-> " + inBundle.getNumMessages());
    clientRetryCount =
        if (inBundle!!.numMessages == 0) {
          logger
              .atFine()
              .log(
                  toString() +
                      " received bundle of " +
                      inBundle.numMessages +
                      " messages from " +
                      user)
          clientRetryCount++
          resend(clientRetryCount)
          return
        } else {
          0
        }
    try {
      synchronized(inSynch) {
        val messages = inBundle.messages
        if (inBundle.numMessages == 1) {
          lastMessageNumber = messages[0]!!.messageNumber
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
            run {
              prevMessageNumber = lastMessageNumber
              lastMessageNumber = messages[i]!!.messageNumber
              if (prevMessageNumber + 1 != lastMessageNumber) {
                if (prevMessageNumber == 0xFFFF && lastMessageNumber == 0) {
                  // exception; do nothing
                } else {
                  logger
                      .atWarning()
                      .log(
                          user.toString() +
                              " dropped a packet! (" +
                              prevMessageNumber +
                              " to " +
                              lastMessageNumber +
                              ")")
                  user!!.droppedPacket()
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
        }
      }
    } catch (e: FatalActionException) {
      logger.atWarning().withCause(e).log(toString() + " fatal action, closing connection")
      Thread.yield()
      stop()
    }
  }

  override fun actionPerformed(event: KailleraEvent?) {
    if (event is GameEvent) {
      val eventHandler = controller.gameEventHandlers[event.javaClass]
      if (eventHandler == null) {
        logger
            .atSevere()
            .log(
                toString() + " found no GameEventHandler registered to handle game event: " + event)
        return
      }
      (eventHandler as V086GameEventHandler<GameEvent>).handleEvent(event as GameEvent, this)
    } else if (event is ServerEvent) {
      val eventHandler = controller.serverEventHandlers[event.javaClass]
      if (eventHandler == null) {
        logger
            .atSevere()
            .log(
                toString() +
                    " found no ServerEventHandler registered to handle server event: " +
                    event)
        return
      }
      (eventHandler as V086ServerEventHandler<ServerEvent>).handleEvent(event, this)
    } else if (event is UserEvent) {
      val eventHandler = controller.userEventHandlers[event.javaClass]
      if (eventHandler == null) {
        logger
            .atSevere()
            .log(
                toString() + " found no UserEventHandler registered to handle user event: " + event)
        return
      }
      (eventHandler as V086UserEventHandler<UserEvent>).handleEvent(event as UserEvent, this)
    }
  }

  fun resend(timeoutCounter: Int) {
    synchronized(outSynch) {
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
    }
  }

  @JvmOverloads
  fun send(outMessage: V086Message?, numToSend: Int = 5) {
    var numToSend = numToSend
    synchronized(outSynch) {
      if (outMessage != null) {
        lastMessageBuffer.add(outMessage)
      }
      numToSend = lastMessageBuffer.fill(outMessages, numToSend)
      // System.out.println("Server -> " + numToSend);
      val outBundle = V086Bundle(outMessages, numToSend)
      //				logger.atFine().log("<- " + outBundle);
      outBundle.writeTo(outBuffer)
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      (outBuffer as Buffer).flip()
      super.send(outBuffer)
      (outBuffer as Buffer).clear()
    }
  }

  init {
    inBuffer.order(ByteOrder.LITTLE_ENDIAN)
    outBuffer.order(ByteOrder.LITTLE_ENDIAN)
    resetGameDataCache()
  }
}

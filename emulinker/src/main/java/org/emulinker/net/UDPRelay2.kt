package org.emulinker.net

import com.google.common.flogger.FluentLogger
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.*
import kotlin.Throws
import kotlin.jvm.JvmOverloads
import org.emulinker.util.EmuUtil.formatSocketAddress

private val logger = FluentLogger.forEnclosingClass()

abstract class UDPRelay2
    @JvmOverloads
    constructor(
        var serverSocketAddress: InetSocketAddress,
        var listenPort: Int,
        var bufferSize: Int = DEFAULT_BUFFER_SIZE
    ) {
  var isStarted = false
    protected set
  protected var stopFlag = false
  var exception: Exception? = null
    protected set
  protected var relayThreads = Hashtable<InetSocketAddress, RelayThread>()
  protected var channels = Hashtable<Int, DatagramChannel?>()
  protected abstract fun processClientToServer(
      receiveBuffer: ByteBuffer?, fromAddress: InetSocketAddress?, toAddress: InetSocketAddress?
  ): ByteBuffer?

  protected abstract fun processServerToClient(
      receiveBuffer: ByteBuffer?, fromAddress: InetSocketAddress?, toAddress: InetSocketAddress?
  ): ByteBuffer?

  @Synchronized
  @Throws(IOException::class)
  fun start() {
    if (!isStarted) {
      stopFlag = false
      relayThreads[serverSocketAddress] = RelayThread(listenPort, serverSocketAddress)
      isStarted = true
    } else {
      logger.atWarning().log("Already started")
    }
  }

  @Synchronized
  fun stop() {
    if (isStarted) {
      logger.atFine().log("Stopping...")
      stopFlag = true
      val e = relayThreads.elements()
      while (e.hasMoreElements()) {
        e.nextElement().close()
      }
      isStarted = false
    } else {
      logger.atWarning().log("Not running")
    }
  }

  // TODO: Make Relay work when server is locahost
  protected inner class RelayThread(port: Int, forwardAddress: InetSocketAddress?) : Thread() {
    protected var port = 0
    var channel: DatagramChannel? = null
      protected set
    var forwardAddress: InetSocketAddress?
      protected set

    protected var toStringRepresentation: String

    var lastActivity: Long
      protected set
    protected var running = false

    protected constructor(forwardAddress: InetSocketAddress?) : this(-1, forwardAddress) {}

    override fun toString(): String {
      return toStringRepresentation
    }

    @Throws(IOException::class)
    fun send(buffer: ByteBuffer?, target: InetSocketAddress?) {
      // logger.atFine().log("Port " + channel.socket().getLocalPort() + " sending
      // to " + EmuUtil.formatSocketAddress(target) + ": " +
      // EmuUtil.INSTANCE.dumpBuffer(buffer));
      channel!!.send(buffer, target)
      lastActivity = System.currentTimeMillis()
    }

    fun close() {
      try {
        channel!!.close()
      } catch (e: Exception) {}
    }

    override fun run() {
      logger.atFine().log("$toStringRepresentation Running")
      try {
        val receiveBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
        while (!stopFlag) {
          running = true

          // Cast to avoid issue with java version mismatch:
          // https://stackoverflow.com/a/61267496/2875073
          (receiveBuffer as Buffer).clear()
          val fromAddress = channel!!.receive(receiveBuffer) as InetSocketAddress
          // Cast to avoid issue with java version mismatch:
          // https://stackoverflow.com/a/61267496/2875073
          (receiveBuffer as Buffer).flip()
          lastActivity = System.currentTimeMillis()
          var sendBuffer: ByteBuffer? = null
          sendBuffer =
              if (fromAddress == serverSocketAddress) {
                // logger.atFine().log("Server at " +
                // EmuUtil.formatSocketAddress(fromAddress) + " sent " +
                // receiveBuffer.limit() + " bytes to relay port " +
                // channel.socket().getLocalPort() + " which it will
                // forward to " +
                // EmuUtil.formatSocketAddress(getForwardAddress()));
                // logger.atFine().log("Buffer Dump: " +
                // EmuUtil.INSTANCE.dumpBuffer(receiveBuffer));
                processServerToClient(receiveBuffer, fromAddress, forwardAddress)
              } else {
                // logger.atFine().log("Client at " +
                // EmuUtil.formatSocketAddress(fromAddress) + " sent " +
                // receiveBuffer.limit() + " bytes to relay port " +
                // channel.socket().getLocalPort() + " which it will
                // forward to " +
                // EmuUtil.formatSocketAddress(getForwardAddress()));
                // logger.atFine().log("Buffer Dump: " +
                // EmuUtil.INSTANCE.dumpBuffer(receiveBuffer));
                processClientToServer(receiveBuffer, fromAddress, forwardAddress)
              }
          if (sendBuffer == null || sendBuffer.limit() <= 0) continue
          var responseThread = relayThreads[fromAddress]
          if (responseThread == null) {
            logger
                .atFine()
                .log(
                    "No RelayThread is registered to forward to " +
                        formatSocketAddress(fromAddress) +
                        "... creating new RelayThread")
            responseThread = RelayThread(fromAddress)
            relayThreads[fromAddress] = responseThread
          }
          responseThread.send(sendBuffer, forwardAddress)
        }
      } catch (e: IOException) {
        logger.atWarning().withCause(e).log("$toStringRepresentation caught IOException")
        if (exception != null) exception = e
      } catch (e: Exception) {
        logger.atSevere().withCause(e).log("$toStringRepresentation caught unexpected exception")
        if (exception != null) exception = e
      } finally {
        this@UDPRelay2.stop()
        running = false
      }
      logger.atFine().log("$toStringRepresentation Exiting")
    }

    init {
      if (port > 0) {
        channel = channels[port]
        if (channel == null) {
          channel = DatagramChannel.open()
          channel!!.socket().bind(InetSocketAddress(port))
          channels[port] = channel
          logger
              .atFine()
              .log(
                  "Created new DatagramChannel bound to specific port " +
                      channel!!.socket().localPort +
                      " that will forward to " +
                      formatSocketAddress(forwardAddress!!))
        } else {
          logger
              .atFine()
              .log(
                  "Using previously created DatagramChannel bound to port " +
                      channel!!.socket().localPort +
                      " that will forward to " +
                      formatSocketAddress(forwardAddress!!))
        }
      } else {
        channel = DatagramChannel.open()
        channel!!.socket().bind(null)
        logger
            .atFine()
            .log(
                "Creating new DatagramChannel bound to arbitrary port " +
                    channel!!.socket().localPort +
                    " that will forward to " +
                    formatSocketAddress(forwardAddress!!))
      }
      lastActivity = System.currentTimeMillis()
      this.forwardAddress = forwardAddress
      this.toStringRepresentation =
          ("RelayThread." +
              threadCounter++ +
              ": " +
              channel!!.socket().localPort +
              "->" +
              formatSocketAddress(forwardAddress))
      this.start()
      while (!running) {
        try {
          sleep(100)
        } catch (e: Exception) {
          logger.atSevere().withCause(e).log("Sleep Interrupted!")
        }
      }
    }
  }

  companion object {
    private const val DEFAULT_BUFFER_SIZE = 4096
    private var threadCounter = 0
  }
}

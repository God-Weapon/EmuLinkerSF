package org.emulinker.net

import com.google.common.flogger.FluentLogger
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.controller.v086.V086Utils
import org.emulinker.util.EmuUtil.dumpBufferFromBeginning
import org.emulinker.util.EmuUtil.formatSocketAddress
import org.emulinker.util.Executable

private val logger = FluentLogger.forEnclosingClass()

abstract class UDPServer(private val flags: RuntimeFlags) : Executable {
  abstract val bufferSize: Int

  /*
  	private static int		artificalPacketLossPercentage = 0;
  	private static int		artificalDelay = 0;
  	private static Random	random = new Random();

  	static
  	{
  		try
  		{
  			artificalPacketLossPercentage = Integer.parseInt(System.getProperty("artificalPacketLossPercentage"));
  			artificalDelay = Integer.parseInt(System.getProperty("artificalDelay"));
  		}
  		catch(Exception e) {}

  		if(artificalPacketLossPercentage > 0)
  			logger.atWarning().log("Introducing " + artificalPacketLossPercentage + "% artifical packet loss!");

  		if(artificalDelay > 0)
  			logger.atWarning().log("Introducing " + artificalDelay + "ms artifical delay!");
  	}
  */
  var bindPort = 0
    private set

  private lateinit var serverSocket: BoundDatagramSocket

  protected lateinit var globalContext: CoroutineContext

  final override var threadIsActive = false
    private set

  protected var stopFlag = false
    private set

  @get:Synchronized
  val isBound: Boolean
    get() {
      return !serverSocket.isClosed
    }
  val isConnected: Boolean
    get() = !serverSocket.isClosed

  @Synchronized
  open suspend fun start(udpSocketProvider: UdpSocketProvider, globalContext: CoroutineContext) {
    this.globalContext = globalContext
    logger.atFine().log(toString() + " received start request!")
    if (threadIsActive) {
      logger.atFine().log(toString() + " start request ignored: already running!")
      return
    }
    stopFlag = false
  }

  @Synchronized
  override suspend fun stop() {
    stopFlag = true
    serverSocket.close()
  }

  @Synchronized
  protected fun bind(udpSocketProvider: UdpSocketProvider) {
    bind(udpSocketProvider, -1)
  }

  @Synchronized
  protected open fun bind(udpSocketProvider: UdpSocketProvider, port: Int) {
    serverSocket =
        udpSocketProvider.bindSocket(
            io.ktor.network.sockets.InetSocketAddress("0.0.0.0", port), bufferSize)

    logger.atInfo().log("Accepting messages at ${serverSocket.localAddress}")
  }

  protected abstract fun allocateBuffer(): ByteBuffer

  protected abstract suspend fun handleReceived(
      buffer: ByteBuffer, remoteSocketAddress: InetSocketAddress, requestScope: CoroutineScope
  )

  protected suspend fun send(buffer: ByteBuffer, toSocketAddress: InetSocketAddress) {
    if (!isBound) {
      logger
          .atWarning()
          .log(
              "Failed to send to " +
                  formatSocketAddress(toSocketAddress) +
                  ": UDPServer is not bound!")
      return
    }
    /*
    if(artificalPacketLossPercentage > 0 && Math.abs(random.nextInt()%100) < artificalPacketLossPercentage)
    {
    	return;
    }
    */ try {
      //			logger.atFine().log("send("+EmuUtil.INSTANCE.dumpBuffer(buffer, false)+")");
      //      channel!!.send(buffer, toSocketAddress)
      serverSocket.send(Datagram(ByteReadPacket(buffer), V086Utils.toKtorAddress(toSocketAddress)))
    } catch (e: Exception) {
      logger.atSevere().withCause(e).log("Failed to send on port $bindPort")
    }
  }

  override suspend fun run(globalContext: CoroutineContext) {
    this.globalContext = globalContext
    threadIsActive = true

    supervisorScope {
      while (!stopFlag) {
        val datagram = serverSocket.incoming.receive()

        require(datagram.address is io.ktor.network.sockets.InetSocketAddress) {
          "address was an incompatable type!"
        }

        val buffer = datagram.packet.readByteBuffer()

        // Launch the request handler asynchronously.
        val requestContext =
            CoroutineScope(coroutineContext) // TODO(nue): Can we just pass coroutineContext?
        val errorHandler =
            CoroutineExceptionHandler { _, e ->
              logger
                  .atSevere()
                  .withCause(e)
                  .log("Error while handling request: %s", dumpBufferFromBeginning(buffer))
            }
        launch(errorHandler) {
          handleReceived(
              buffer,
              V086Utils.toJavaAddress(
                  datagram.address as io.ktor.network.sockets.InetSocketAddress),
              requestScope = requestContext)
        }
      }
    }

    threadIsActive = false
  }

  // TODO(nue): Investigate this.
  //  private inner class ShutdownThread : Thread() {
  //    override fun run() {
  //      this@UDPServer.stop()
  //    }
  //  }
  //  init {
  //    if (shutdownOnExit) Runtime.getRuntime().addShutdownHook(ShutdownThread())
  //  }
}

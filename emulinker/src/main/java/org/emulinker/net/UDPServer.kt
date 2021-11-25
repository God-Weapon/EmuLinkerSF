package org.emulinker.net

import com.codahale.metrics.MetricRegistry
import com.google.common.flogger.FluentLogger
import java.io.IOException
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.Throws
import org.emulinker.net.UDPServer.ShutdownThread
import org.emulinker.util.EmuUtil.formatSocketAddress
import org.emulinker.util.Executable

abstract class UDPServer(shutdownOnExit: Boolean, metrics: MetricRegistry?) : Executable {
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
  private var channel: DatagramChannel? = null

  final override var running = false
    private set

  protected var stopFlag = false
    private set

  @get:Synchronized
  val isBound: Boolean
    get() {
      if (channel == null) return false
      return if (channel!!.socket() == null) false else !channel!!.socket().isClosed
    }
  val isConnected: Boolean
    get() = channel!!.isConnected

  @Synchronized
  open fun start() {
    logger.atFine().log(toString() + " received start request!")
    if (running) {
      logger.atFine().log(toString() + " start request ignored: already running!")
      return
    }
    stopFlag = false
  }

  @Synchronized
  override fun stop() {
    stopFlag = true
    if (channel != null) {
      try {
        channel!!.close()
      } catch (e: IOException) {
        logger.atSevere().withCause(e).log("Failed to close DatagramChannel")
      }
    }
  }

  @Synchronized
  @Throws(BindException::class)
  protected fun bind() {
    bind(-1)
  }

  @Synchronized
  @Throws(BindException::class)
  protected open fun bind(port: Int) {
    try {
      channel = DatagramChannel.open()
      if (port > 0) channel!!.socket().bind(InetSocketAddress(port))
      else channel!!.socket().bind(null)
      bindPort = channel!!.socket().localPort
      val tempBuffer = buffer
      val bufferSize = tempBuffer.capacity() * 2
      releaseBuffer(tempBuffer)
      channel!!.socket().receiveBufferSize = bufferSize
      channel!!.socket().sendBufferSize = bufferSize
    } catch (e: IOException) {
      throw BindException("Failed to bind to port $port", port, e)
    }
    start()
  }

  protected abstract val buffer: ByteBuffer

  protected abstract fun releaseBuffer(buffer: ByteBuffer)

  protected abstract fun handleReceived(buffer: ByteBuffer, remoteSocketAddress: InetSocketAddress)

  protected fun send(buffer: ByteBuffer?, toSocketAddress: InetSocketAddress?) {
    if (!isBound) {
      logger
          .atWarning()
          .log(
              "Failed to send to " +
                  formatSocketAddress(toSocketAddress!!) +
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
      channel!!.send(buffer, toSocketAddress)
    } catch (e: Exception) {
      logger.atSevere().withCause(e).log("Failed to send on port $bindPort")
    }
  }

  override fun run() {
    running = true
    logger.atFine().log(toString() + ": thread running...")
    try {
      while (!stopFlag) {
        try {
          val buffer = buffer
          val fromSocketAddress = channel!!.receive(buffer) as InetSocketAddress
          if (stopFlag) break
          if (fromSocketAddress == null)
              throw IOException("Failed to receive from DatagramChannel: fromSocketAddress == null")
          /*
          if(artificalPacketLossPercentage > 0 && Math.abs(random.nextInt()%100) < artificalPacketLossPercentage)
          {
          	releaseBuffer(buffer);
          	continue;
          }

          if(artificalDelay > 0)
          {
          	try
          	{
          		Thread.sleep(artificalDelay);
          	}
          	catch(Exception e) {}
          }
          */
          // Cast to avoid issue with java version mismatch:
          // https://stackoverflow.com/a/61267496/2875073
          (buffer as Buffer).flip()
          //					logger.atFine().log("receive("+EmuUtil.INSTANCE.dumpBuffer(buffer, false)+")");
          // TODO(nue): time this
          handleReceived(buffer, fromSocketAddress)
          releaseBuffer(buffer)
        } catch (e: SocketException) {
          if (stopFlag) break
          logger.atSevere().withCause(e).log("Failed to receive on port %d", bindPort)
        } catch (e: IOException) {
          if (stopFlag) break
          logger.atSevere().withCause(e).log("Failed to receive on port %d", bindPort)
        }
      }
    } catch (e: Throwable) {
      logger
          .atSevere()
          .withCause(e)
          .log("UDPServer on port %d caught unexpected exception!", bindPort)
      stop()
    } finally {
      running = false
      logger.atFine().log(toString() + ": thread exiting...")
    }
  }

  private inner class ShutdownThread : Thread() {
    override fun run() {
      this@UDPServer.stop()
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }

  init {
    if (shutdownOnExit) Runtime.getRuntime().addShutdownHook(ShutdownThread())
  }
}

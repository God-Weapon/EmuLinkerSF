package org.emulinker.net

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.google.common.flogger.FluentLogger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import org.emulinker.util.EmuUtil.formatSocketAddress

private val logger = FluentLogger.forEnclosingClass()

abstract class PrivateUDPServer(
    shutdownOnExit: Boolean, val remoteInetAddress: InetAddress, metrics: MetricRegistry
) : UDPServer(shutdownOnExit, metrics) {

  private val clientRequestTimer: Timer

  var remoteSocketAddress: InetSocketAddress? = null
    private set

  override fun handleReceived(buffer: ByteBuffer, inboundSocketAddress: InetSocketAddress) {
    if (remoteSocketAddress == null) remoteSocketAddress = inboundSocketAddress
    else if (inboundSocketAddress != remoteSocketAddress) {
      logger
          .atWarning()
          .log(
              "Rejecting packet received from wrong address: " +
                  formatSocketAddress(inboundSocketAddress) +
                  " != " +
                  formatSocketAddress(remoteSocketAddress!!))
      return
    }
    clientRequestTimer.time().use { context -> handleReceived(buffer) }
  }

  protected abstract fun handleReceived(buffer: ByteBuffer)

  protected fun send(buffer: ByteBuffer?) {
    super.send(buffer, remoteSocketAddress)
  }

  init {
    clientRequestTimer = metrics.timer(MetricRegistry.name(this.javaClass, "clientRequests"))
  }
}

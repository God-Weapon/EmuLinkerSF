package org.emulinker.kaillera.controller

import java.net.InetSocketAddress
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.exception.NewConnectionException
import org.emulinker.kaillera.model.exception.ServerFullException
import org.emulinker.net.UdpSocketProvider

interface KailleraServerController {
  val server: KailleraServer
  val bufferSize: Int
  val version: String
  val numClients: Int
  val clientTypes: Array<String>

  @Throws(ServerFullException::class, NewConnectionException::class)
  suspend fun newConnection(
      udpSocketProvider: UdpSocketProvider, clientSocketAddress: InetSocketAddress, protocol: String
  ): Int

  fun start()

  suspend fun stop()
  val clientHandlers: MutableMap<Int, V086ClientHandler>
}

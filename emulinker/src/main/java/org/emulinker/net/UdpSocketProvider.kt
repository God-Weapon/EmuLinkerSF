package org.emulinker.net

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

class UdpSocketProvider @Inject constructor() {
  fun bindSocket(inetSocketAddress: InetSocketAddress, bufferSize: Int): BoundDatagramSocket {
    // TODO(nue): Should this be IO?
    val selectorManager = SelectorManager(Dispatchers.IO)
    return aSocket(selectorManager)
        .udp()
        .configure {
          receiveBufferSize = bufferSize
          sendBufferSize = bufferSize
          typeOfService = TypeOfService.IPTOS_LOWDELAY
        }
        .bind(inetSocketAddress)
  }
}

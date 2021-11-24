package org.emulinker.kaillera.master

import org.emulinker.config.RuntimeFlags

class PublicServerInformation(flags: RuntimeFlags) {
  val serverName: String = flags.serverName
  val location: String = flags.serverLocation
  val website: String = flags.serverWebsite
  val connectAddress: String = flags.serverAddress
}

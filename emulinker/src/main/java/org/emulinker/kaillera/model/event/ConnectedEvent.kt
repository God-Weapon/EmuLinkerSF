package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.KailleraUser

data class ConnectedEvent(val server: KailleraServer, override val user: KailleraUser) : UserEvent {
  override fun toString(): String {
    return "ConnectedEvent"
  }
}

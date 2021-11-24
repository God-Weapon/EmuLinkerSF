package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.KailleraUser

class UserJoinedEvent(override val server: KailleraServer, val user: KailleraUser) : ServerEvent {
  override fun toString(): String {
    return "UserJoinedEvent"
  }
}

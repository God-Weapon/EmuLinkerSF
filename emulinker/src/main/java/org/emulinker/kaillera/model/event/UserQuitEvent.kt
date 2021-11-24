package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.KailleraUser

class UserQuitEvent(
    override val server: KailleraServer, val user: KailleraUser, val message: String
) : ServerEvent {
  override fun toString(): String {
    return "UserQuitEvent"
  }
}

package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraUser

class InfoMessageEvent(override val user: KailleraUser, val message: String) : UserEvent {
  override fun toString(): String {
    return "InfoMessageEvent"
  }
}

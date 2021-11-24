package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraUser

interface UserEvent : KailleraEvent {
  val user: KailleraUser?
}

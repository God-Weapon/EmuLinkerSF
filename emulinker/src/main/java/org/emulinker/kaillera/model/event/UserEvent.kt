package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.KailleraUser

sealed interface UserEvent : KailleraEvent {
  val user: KailleraUser?
}

data class ConnectedEvent(val server: KailleraServer, override val user: KailleraUser) : UserEvent

data class InfoMessageEvent(override val user: KailleraUser, val message: String) : UserEvent

package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraServer

class GameStatusChangedEvent(override val server: KailleraServer, val game: KailleraGame) :
    ServerEvent {
  override fun toString(): String {
    return "GameStatusChangedEvent"
  }
}

package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame

class AllReadyEvent(override val game: KailleraGame) : GameEvent {
  override fun toString(): String {
    return "AllReadyEvent"
  }
}

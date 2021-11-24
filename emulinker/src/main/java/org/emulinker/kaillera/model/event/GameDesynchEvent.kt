package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame

class GameDesynchEvent(override val game: KailleraGame, val message: String) : GameEvent {
  override fun toString(): String {
    return "GameDesynchEvent"
  }
}

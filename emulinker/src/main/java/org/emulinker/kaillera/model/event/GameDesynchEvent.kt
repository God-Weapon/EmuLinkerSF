package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame

data class GameDesynchEvent(override val game: KailleraGame, val message: String) : GameEvent {
  override fun toString(): String {
    return "GameDesynchEvent"
  }
}

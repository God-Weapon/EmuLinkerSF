package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame

data class GameDataEvent(override val game: KailleraGame, val data: ByteArray) : GameEvent {
  override fun toString(): String {
    return "GameDataEvent"
  }
}

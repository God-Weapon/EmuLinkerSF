package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

class PlayerDesynchEvent(
    override val game: KailleraGame, val user: KailleraUser, val message: String
) : GameEvent {
  override fun toString(): String {
    return "GameDesynchEvent"
  }
}

package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

class GameInfoEvent(override val game: KailleraGame, val message: String, val user: KailleraUser?) :
    GameEvent {
  override fun toString(): String {
    return "GameInfoEvent"
  }
}

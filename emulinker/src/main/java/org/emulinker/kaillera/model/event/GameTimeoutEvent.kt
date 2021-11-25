package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

data class GameTimeoutEvent(
    override val game: KailleraGame, val user: KailleraUser, val timeoutNumber: Int
) : GameEvent {
  override fun toString(): String {
    return "GameTimeoutEvent"
  }
}

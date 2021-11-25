package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

data class UserDroppedGameEvent(
    override val game: KailleraGame, val user: KailleraUser, val playerNumber: Int
) : GameEvent {
  override fun toString(): String {
    return "UserDroppedGameEvent"
  }
}

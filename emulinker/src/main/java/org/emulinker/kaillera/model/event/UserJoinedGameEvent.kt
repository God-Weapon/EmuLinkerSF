package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

data class UserJoinedGameEvent(override val game: KailleraGame, val user: KailleraUser) :
    GameEvent {
  override fun toString(): String {
    return "UserJoinedGameEvent"
  }
}

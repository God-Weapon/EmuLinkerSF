package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

// If game is null, I think that means the user quit the whole server.
class UserQuitGameEvent(override val game: KailleraGame?, val user: KailleraUser) : GameEvent {
  override fun toString(): String {
    return "UserQuitGameEvent"
  }
}

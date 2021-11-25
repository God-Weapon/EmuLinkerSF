package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

data class GameChatEvent(
    override val game: KailleraGame, val user: KailleraUser, val message: String
) : GameEvent {
  override fun toString(): String {
    return "GameChatEvent"
  }
}

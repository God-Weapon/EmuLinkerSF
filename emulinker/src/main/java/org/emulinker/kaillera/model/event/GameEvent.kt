package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser

sealed interface GameEvent : KailleraEvent {
  val game: KailleraGame?
}

data class GameInfoEvent(
    override val game: KailleraGame, val message: String, val toUser: KailleraUser?
) : GameEvent

data class GameChatEvent(
    override val game: KailleraGame, val user: KailleraUser, val message: String
) : GameEvent

data class AllReadyEvent(override val game: KailleraGame) : GameEvent

data class GameDesynchEvent(override val game: KailleraGame, val message: String) : GameEvent

data class GameDataEvent(override val game: KailleraGame, val data: ByteArray) : GameEvent

data class GameStartedEvent(override val game: KailleraGame) : GameEvent

data class GameTimeoutEvent(
    override val game: KailleraGame, val user: KailleraUser, val timeoutNumber: Int
) : GameEvent

data class UserJoinedGameEvent(override val game: KailleraGame, val user: KailleraUser) : GameEvent

// If game is null, I think that means the user quit the whole server.
data class UserQuitGameEvent(override val game: KailleraGame?, val user: KailleraUser) : GameEvent

data class PlayerDesynchEvent(
    override val game: KailleraGame, val user: KailleraUser, val message: String
) : GameEvent

data class UserDroppedGameEvent(
    override val game: KailleraGame, val user: KailleraUser, val playerNumber: Int
) : GameEvent

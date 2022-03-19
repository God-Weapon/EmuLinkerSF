package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.model.KailleraUser

sealed interface ServerEvent : KailleraEvent {
  val server: KailleraServer?
}

data class GameStatusChangedEvent(override val server: KailleraServer, val game: KailleraGame) :
    ServerEvent

data class ChatEvent(
    override val server: KailleraServer, val user: KailleraUser, val message: String
) : ServerEvent

data class GameClosedEvent(override val server: KailleraServer, val game: KailleraGame) :
    ServerEvent

data class GameCreatedEvent(override val server: KailleraServer, val game: KailleraGame) :
    ServerEvent

data class UserJoinedEvent(override val server: KailleraServer, val user: KailleraUser) :
    ServerEvent

data class UserQuitEvent(
    override val server: KailleraServer, val user: KailleraUser, val message: String
) : ServerEvent

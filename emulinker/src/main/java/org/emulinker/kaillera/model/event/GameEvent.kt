package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraGame

interface GameEvent : KailleraEvent {
  val game: KailleraGame?
}

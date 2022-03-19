package org.emulinker.kaillera.model.event

sealed interface KailleraEvent {
  override fun toString(): String
}

class StopFlagEvent : KailleraEvent {
  override fun toString(): String {
    return "StopFlagEvent"
  }
}

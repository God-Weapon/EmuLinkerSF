package org.emulinker.kaillera.model.event

sealed interface KailleraEvent {
  override fun toString(): String
}

@Deprecated("This seems unnecessary", level = DeprecationLevel.WARNING)
class StopFlagEvent : KailleraEvent {
  override fun toString(): String {
    return "StopFlagEvent"
  }
}

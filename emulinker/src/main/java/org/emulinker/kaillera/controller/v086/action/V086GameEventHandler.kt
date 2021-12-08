package org.emulinker.kaillera.controller.v086.action

import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.model.event.GameEvent

interface V086GameEventHandler<in T : GameEvent?> {
  @Deprecated("Structure this in a different way")
  override fun toString(): String

  fun handleEvent(event: T, clientHandler: V086ClientHandler)
  val handledEventCount: Int
}

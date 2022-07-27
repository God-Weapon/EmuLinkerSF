package org.emulinker.kaillera.controller.v086.action

import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.model.event.ServerEvent

interface V086ServerEventHandler<in T : ServerEvent> {
  @Deprecated("Structure this in a different way")
  override fun toString(): String

  suspend fun handleEvent(event: T, clientHandler: V086ClientHandler)
  val handledEventCount: Int
}

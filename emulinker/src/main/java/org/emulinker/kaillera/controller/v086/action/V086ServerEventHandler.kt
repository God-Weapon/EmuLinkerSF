package org.emulinker.kaillera.controller.v086.action

import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.model.event.ServerEvent

interface V086ServerEventHandler<T : ServerEvent> {
  override fun toString(): String
  fun handleEvent(event: T, clientHandler: V086ClientHandler)
  val handledEventCount: Int
}

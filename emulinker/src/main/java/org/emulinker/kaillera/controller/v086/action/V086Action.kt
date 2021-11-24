package org.emulinker.kaillera.controller.v086.action

import kotlin.Throws
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.V086Message

interface V086Action<T : V086Message> {
  override fun toString(): String

  @Throws(FatalActionException::class)
  fun performAction(message: T, clientHandler: V086ClientHandler?)
  val actionPerformedCount: Int
}

package org.emulinker.kaillera.model.event

import org.emulinker.kaillera.model.KailleraServer

interface ServerEvent : KailleraEvent {
  val server: KailleraServer?
}

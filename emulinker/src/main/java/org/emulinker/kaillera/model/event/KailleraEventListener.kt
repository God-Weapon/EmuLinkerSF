package org.emulinker.kaillera.model.event

interface KailleraEventListener {
  fun actionPerformed(event: KailleraEvent?)
  fun stop()
}

package org.emulinker.kaillera.model.event

interface KailleraEventListener {
  suspend fun actionPerformed(event: KailleraEvent)
  suspend fun stop()
}

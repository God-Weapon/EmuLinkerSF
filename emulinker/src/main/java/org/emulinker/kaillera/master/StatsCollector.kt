package org.emulinker.kaillera.master

import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraServer

interface StatsCollector {
  fun markGameAsStarted(server: KailleraServer, game: KailleraGame)

  fun getStartedGamesList(): MutableList<String>

  fun clearStartedGamesList()
}

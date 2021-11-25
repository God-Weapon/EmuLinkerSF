package org.emulinker.kaillera.master

import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraServer

@Singleton
class MasterListStatsCollector @Inject internal constructor() : StatsCollector {
  private val startedGamesList = mutableListOf<String>()

  @Synchronized
  override fun markGameAsStarted(server: KailleraServer, game: KailleraGame) {
    startedGamesList.add(game.romName!!)
  }

  @Synchronized
  override fun getStartedGamesList(): MutableList<String> {
    return startedGamesList
  }

  @Synchronized
  override fun clearStartedGamesList() {
    startedGamesList.clear()
  }
}

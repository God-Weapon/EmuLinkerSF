package org.emulinker.kaillera.master;

import java.util.List;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;

public interface StatsCollector {
  public void gameStarted(KailleraServer server, KailleraGame game);

  public List<String> getStartedGamesList();

  public void clearStartedGamesList();
}

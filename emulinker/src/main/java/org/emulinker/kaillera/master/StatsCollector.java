package org.emulinker.kaillera.master;

import java.util.List;

import org.emulinker.kaillera.model.*;

public interface StatsCollector
{
	public void gameStarted(KailleraServer server, KailleraGame game);

	public List getStartedGamesList();

	public void clearStartedGamesList();
}

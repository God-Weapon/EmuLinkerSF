package org.emulinker.kaillera.master;

import java.util.*;

import org.emulinker.kaillera.model.*;

public class MasterListStatsCollector implements StatsCollector
{
	private ArrayList<String>	startedGamesList	= new ArrayList<String>();

	public synchronized void gameStarted(KailleraServer server, KailleraGame game)
	{
		startedGamesList.add(game.getRomName());
	}

	public synchronized List<String> getStartedGamesList()
	{
		return startedGamesList;
	}

	public synchronized void clearStartedGamesList()
	{
		startedGamesList.clear();
	}
}

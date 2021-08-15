package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraGame;

public interface GameEvent extends KailleraEvent
{
	public KailleraGame getGame();
}

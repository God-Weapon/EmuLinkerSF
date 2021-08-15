package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class UserJoinedGameEvent implements GameEvent
{
	private KailleraGame	game;
	private KailleraUser	user;

	public UserJoinedGameEvent(KailleraGame game, KailleraUser user)
	{
		this.game = game;
		this.user = user;
	}

	public String toString()
	{
		return "UserJoinedGameEvent";
	}

	public KailleraGame getGame()
	{
		return game;
	}

	public KailleraUser getUser()
	{
		return user;
	}
}

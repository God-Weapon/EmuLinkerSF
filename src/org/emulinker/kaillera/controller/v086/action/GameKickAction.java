package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.exception.GameKickException;

public class GameKickAction implements V086Action
{
	private static Log				log			= LogFactory.getLog(GameKickAction.class);
	private static final String		desc		= "GameKickAction";
	private static GameKickAction	singleton	= new GameKickAction();

	public static GameKickAction getInstance()
	{
		return singleton;
	}

	private int	actionCount	= 0;

	private GameKickAction()
	{

	}

	public int getActionPerformedCount()
	{
		return actionCount;
	}

	public String toString()
	{
		return desc;
	}

	public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException
	{
		actionCount++;

		GameKick kickRequest = (GameKick) message;

		try
		{
			clientHandler.getUser().gameKick(kickRequest.getUserID());
		}
		catch (GameKickException e)
		{
			log.debug("Failed to kick: " + e.getMessage());
		}
	}
}

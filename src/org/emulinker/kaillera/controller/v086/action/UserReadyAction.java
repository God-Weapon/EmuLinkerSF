package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.UserReadyException;

public class UserReadyAction implements V086Action, V086GameEventHandler
{
	private static Log				log			= LogFactory.getLog(UserReadyAction.class);
	private static final String		desc		= "UserReadyAction";
	private static UserReadyAction	singleton	= new UserReadyAction();

	public static UserReadyAction getInstance()
	{
		return singleton;
	}

	private int	actionCount		= 0;
	private int	handledCount	= 0;

	private UserReadyAction()
	{

	}

	public int getActionPerformedCount()
	{
		return actionCount;
	}

	public int getHandledEventCount()
	{
		return handledCount;
	}

	public String toString()
	{
		return desc;
	}

	public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException
	{
		actionCount++;

		try
		{
			clientHandler.getUser().playerReady();
		}
		catch (UserReadyException e)
		{
			log.debug("Ready signal failed: " + e.getMessage());
		}
	}

	public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler)
	{
		handledCount++;

		clientHandler.resetGameDataCache();

		try
		{
			clientHandler.send(new AllReady(clientHandler.getNextMessageNumber()));
		}
		catch (MessageFormatException e)
		{
			log.error("Failed to contruct AllReady message: " + e.getMessage(), e);
		}
	}
}

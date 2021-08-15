package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.CloseGame;
import org.emulinker.kaillera.model.event.*;

public class CloseGameAction implements V086ServerEventHandler
{
	private static Log				log			= LogFactory.getLog(CloseGameAction.class);
	private static final String		desc		= "CloseGameAction";
	private static CloseGameAction	singleton	= new CloseGameAction();

	public static CloseGameAction getInstance()
	{
		return singleton;
	}

	private int	handledCount;

	private CloseGameAction()
	{

	}

	public int getHandledEventCount()
	{
		return handledCount;
	}

	public String toString()
	{
		return desc;
	}

	public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler)
	{
		handledCount++;

		GameClosedEvent gameClosedEvent = (GameClosedEvent) event;

		try
		{
			clientHandler.send(new CloseGame(clientHandler.getNextMessageNumber(), gameClosedEvent.getGame().getID(), (short) 0));
		}
		catch (MessageFormatException e)
		{
			log.error("Failed to contruct CloseGame_Notification message: " + e.getMessage(), e);
		}
	}
}

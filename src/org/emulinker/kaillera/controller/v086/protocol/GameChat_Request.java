package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class GameChat_Request extends GameChat
{
	public static final String	DESC	= "In-Game Chat Request";

	public GameChat_Request(int messageNumber, String message) throws MessageFormatException
	{
		super(messageNumber, "", message);
	}

	public String getDescription()
	{
		return DESC;
	}

	public String toString()
	{
		return getInfoString() + "[message=" + getMessage() + "]";
	}
}

package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class Chat_Request extends Chat
{
	public static final String	DESC	= "Chat Request";

	public Chat_Request(int messageNumber, String message) throws MessageFormatException
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

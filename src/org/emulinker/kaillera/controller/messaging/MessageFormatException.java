package org.emulinker.kaillera.controller.messaging;

public class MessageFormatException extends Exception
{
	public MessageFormatException()
	{
		super();
	}

	public MessageFormatException(String msg)
	{
		super(msg);
	}

	public MessageFormatException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}

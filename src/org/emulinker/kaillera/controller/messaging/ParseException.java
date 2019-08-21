package org.emulinker.kaillera.controller.messaging;

public class ParseException extends Exception
{
	public ParseException()
	{
		super();
	}

	public ParseException(String msg)
	{
		super(msg);
	}

	public ParseException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}

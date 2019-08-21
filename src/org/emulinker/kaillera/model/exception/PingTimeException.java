package org.emulinker.kaillera.model.exception;

public class PingTimeException extends LoginException
{
	public PingTimeException(String message)
	{
		super(message);
	}

	public PingTimeException(String message, Exception source)
	{
		super(message, source);
	}
}

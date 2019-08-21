package org.emulinker.kaillera.model.exception;

public class LoginException extends ActionException
{
	public LoginException(String message)
	{
		super(message);
	}

	public LoginException(String message, Exception source)
	{
		super(message, source);
	}
}

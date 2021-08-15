package org.emulinker.kaillera.model.exception;

public class DropGameException extends ActionException
{
	public DropGameException(String message)
	{
		super(message);
	}

	public DropGameException(String message, Exception source)
	{
		super(message, source);
	}
}

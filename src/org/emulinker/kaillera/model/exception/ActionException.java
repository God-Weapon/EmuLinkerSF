package org.emulinker.kaillera.model.exception;

public class ActionException extends Exception
{
	public ActionException()
	{

	}

	public ActionException(String message)
	{
		super(message);
	}

	public ActionException(String message, Exception source)
	{
		super(message, source);
	}
}

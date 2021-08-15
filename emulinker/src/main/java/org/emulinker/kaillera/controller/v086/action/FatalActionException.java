package org.emulinker.kaillera.controller.v086.action;

public class FatalActionException extends Exception
{
	public FatalActionException(String message)
	{
		super(message);
	}

	public FatalActionException(String message, Exception source)
	{
		super(message, source);
	}
}

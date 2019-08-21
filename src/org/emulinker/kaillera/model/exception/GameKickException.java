package org.emulinker.kaillera.model.exception;

public class GameKickException extends ActionException
{
	public GameKickException(String message)
	{
		super(message);
	}

	public GameKickException(String message, Exception source)
	{
		super(message, source);
	}
}

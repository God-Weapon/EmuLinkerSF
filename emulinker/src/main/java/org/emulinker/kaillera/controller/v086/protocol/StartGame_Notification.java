package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class StartGame_Notification extends StartGame
{
	public static final String	DESC	= "Start Game Notification";

	public StartGame_Notification(int messageNumber, int val1, short playerNumber, short numPlayers) throws MessageFormatException
	{
		super(messageNumber, val1, playerNumber, numPlayers);
	}

	public String getDescription()
	{
		return DESC;
	}

	public String toString()
	{
		return getInfoString() + "[val1=" + getVal1() + " playerNumber=" + getPlayerNumber() + " numPlayers=" + getNumPlayers() + "]";
	}
}

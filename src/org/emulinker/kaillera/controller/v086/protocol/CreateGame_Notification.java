package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class CreateGame_Notification extends CreateGame
{
	public static final String	DESC	= "Create Game Notification";

	public CreateGame_Notification(int messageNumber, String userName, String romName, String clientType, int gameID, int val1) throws MessageFormatException
	{
		super(messageNumber, userName, romName, clientType, gameID, val1);
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public String toString()
	{
		return getInfoString() + "[userName=" + getUserName() + " romName=" + getRomName() + " clientType=" + getClientType() + " gameID=" + getGameID() + " val1=" + getVal1() + "]";
	}
}

package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public abstract class QuitGame extends V086Message
{
	public static final byte	ID	= 0x0B;

	private String				userName;
	private int					userID;

	public QuitGame(int messageNumber, String userName, int userID) throws MessageFormatException
	{
		super(messageNumber);

		if (userID < 0 || userID > 0xFFFF)
			throw new MessageFormatException("Invalid " + getDescription() + " format: userID out of acceptable range: " + userID);

		this.userName = userName; // check userName length?
		this.userID = userID;
	}

	public byte getID()
	{
		return ID;
	}

	public abstract String getDescription();

	public String getUserName()
	{
		return userName;
	}

	public int getUserID()
	{
		return userID;
	}

	public int getBodyLength()
	{
		//return (charset.encode(userName).remaining() + 3);
		return (getNumBytes(userName) + 3);
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, userName, 0x00, charset);
		UnsignedUtil.putUnsignedShort(buffer, userID);
	}

	public static QuitGame parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 3)
			throw new ParseException("Failed byte count validation!");

		String userName = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 2)
			throw new ParseException("Failed byte count validation!");
		
		int userID = UnsignedUtil.getUnsignedShort(buffer);

		if (userName.length() == 0 && userID == 0xFFFF)
			return new QuitGame_Request(messageNumber);
		else
			return new QuitGame_Notification(messageNumber, userName, userID);
	}
}

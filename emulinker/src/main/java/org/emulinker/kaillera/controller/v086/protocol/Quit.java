package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public abstract class Quit extends V086Message
{
	public static final byte	ID	= 0x01;

	private String				userName;
	private int					userID;
	private String				message;

	public Quit(int messageNumber, String userName, int userID, String message) throws MessageFormatException
	{
		super(messageNumber);

		if (userID < 0 || userID > 0xFFFF)
			throw new MessageFormatException("Invalid " + getDescription() + " format: userID out of acceptable range: " + userID);

		if (message == null)
			throw new MessageFormatException("Invalid " + getDescription() + " format: message == null!");

		this.userName = userName; // check userName length?
		this.userID = userID;
		this.message = message;
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

	public String getMessage()
	{
		return message;
	}

	public abstract String toString();

	public int getBodyLength()
	{
		return getNumBytes(userName) + getNumBytes(message) + 4;
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, userName, 0x00, charset);
		UnsignedUtil.putUnsignedShort(buffer, userID);
		EmuUtil.writeString(buffer, message, 0x00, charset);
	}

	public static Quit parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 5)
			throw new ParseException("Failed byte count validation!");

		String userName = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 3)
			throw new ParseException("Failed byte count validation!");
		
		int userID = UnsignedUtil.getUnsignedShort(buffer);

		String message = EmuUtil.readString(buffer, 0x00, charset);
			
		if (userName.length() == 0 && userID == 0xFFFF)
			return new Quit_Request(messageNumber, message);
		else
			return new Quit_Notification(messageNumber, userName, userID, message);
	}
}

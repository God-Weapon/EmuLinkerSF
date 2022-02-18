package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public abstract class CreateGame extends V086Message
{
	public static final byte	ID	= 0x0A;

	private String				userName;
	private String				romName;
	private String				clientType;
	private int					gameID;
	private int					val1;

	public CreateGame(int messageNumber, String userName, String romName, String clientType, int gameID, int val1) throws MessageFormatException
	{
		super(messageNumber);

		if (romName.length() == 0)
			throw new MessageFormatException("Invalid " + getDescription() + " format: romName.length == 0");

		if (gameID < 0 || gameID > 0xFFFF)
			throw new MessageFormatException("Invalid " + getDescription() + " format: gameID out of acceptable range: " + gameID);

		if (val1 != 0x0000 && val1 != 0xFFFF)
			throw new MessageFormatException("Invalid " + getDescription() + " format: val1 out of acceptable range: " + val1);

		this.userName = userName;
		this.romName = romName;
		this.clientType = clientType;
		this.gameID = gameID;
		this.val1 = val1;
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

	public String getRomName()
	{
		return romName;
	}

	public String getClientType()
	{
		return clientType;
	}

	public int getGameID()
	{
		return gameID;
	}

	public int getVal1()
	{
		return val1;
	}

	public abstract String toString();

	public int getBodyLength()
	{
		//return (charset.encode(userName).remaining() + charset.encode(romName).remaining() + charset.encode(clientType).remaining() + 7);
		return (getNumBytes(userName) + getNumBytes(romName) + getNumBytes(clientType) + 7);
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, userName, 0x00, charset);
		EmuUtil.writeString(buffer, romName, 0x00, charset);
		EmuUtil.writeString(buffer, clientType, 0x00, charset);
		UnsignedUtil.putUnsignedShort(buffer, gameID);
		UnsignedUtil.putUnsignedShort(buffer, val1);
	}

	public static CreateGame parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 8)
			throw new ParseException("Failed byte count validation!");

		String userName = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 6)
			throw new ParseException("Failed byte count validation!");
		
		
		String romName = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 5)
			throw new ParseException("Failed byte count validation!");
		
		String clientType = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 4)
			throw new ParseException("Failed byte count validation!");
		
		
		int gameID = UnsignedUtil.getUnsignedShort(buffer);
		int val1 = UnsignedUtil.getUnsignedShort(buffer);

		if (userName.length() == 0 && gameID == 0xFFFF && val1 == 0xFFFF)
			return new CreateGame_Request(messageNumber, romName);
		else
			return new CreateGame_Notification(messageNumber, userName, romName, clientType, gameID, val1);
	}
}

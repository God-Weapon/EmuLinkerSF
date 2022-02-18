package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.EmuUtil;

public abstract class PlayerDrop extends V086Message
{
	public static final byte	ID	= 0x14;
	private String				userName;

	private byte				playerNumber;

	public PlayerDrop(int messageNumber, String userName, byte playerNumber) throws MessageFormatException
	{
		super(messageNumber);

		if (playerNumber < 0 || playerNumber > 255)
			throw new MessageFormatException("Invalid " + getDescription() + " format: playerNumber out of acceptable range: " + playerNumber);

		this.userName = userName;
		this.playerNumber = playerNumber;
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

	public byte getPlayerNumber()
	{
		return playerNumber;
	}

	public String toString()
	{
		return getInfoString() + "[userName=" + userName + " playerNumber=" + playerNumber + "]";
	}

	public int getBodyLength()
	{
		//return (charset.encode(userName).remaining() + 2);
		return (getNumBytes(userName) + 2);
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, userName, 0x00, charset);
		buffer.put(playerNumber);
	}

	public static PlayerDrop parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 2)
			throw new ParseException("Failed byte count validation!");

		String userName = EmuUtil.readString(buffer, 0x00, charset);
		byte playerNumber = buffer.get();

		if (userName.length() == 0 && playerNumber == 0)
			return new PlayerDrop_Request(messageNumber);
		else
			return new PlayerDrop_Notification(messageNumber, userName, playerNumber);
	}
}

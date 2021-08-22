package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class UserJoined extends V086Message
{
	public static final byte	ID		= 0x02;
	public static final String	DESC	= "User Joined";

	private String				userName;
	private int					userID;
	private long				ping;
	private byte				connectionType;

	public UserJoined(int messageNumber, String userName, int userID, long ping, byte connectionType) throws MessageFormatException
	{
		super(messageNumber);

		if (userName.length() == 0)
			throw new MessageFormatException("Invalid " + getDescription() + " format: userName.length == 0, (userID = " + userID + ")");

		if (userID < 0 || userID > 65535)
			throw new MessageFormatException("Invalid " + getDescription() + " format: userID out of acceptable range: " + userID);

		if (ping < 0 || ping > 2048) // what should max ping be?
			throw new MessageFormatException("Invalid " + getDescription() + " format: ping out of acceptable range: " + ping);

		if (connectionType < 1 || connectionType > 6)
			throw new MessageFormatException("Invalid " + getDescription() + " format: connectionType out of acceptable range: " + connectionType);

		this.userName = userName; // check userName length?
		this.userID = userID;
		this.ping = ping;
		this.connectionType = connectionType;
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public String getUserName()
	{
		return userName;
	}

	public int getUserID()
	{
		return userID;
	}

	public long getPing()
	{
		return ping;
	}

	public byte getConnectionType()
	{
		return connectionType;
	}

	public String toString()
	{
		return getInfoString() + "[userName=" + userName + " userID=" + userID + " ping=" + ping + " connectionType=" + org.emulinker.kaillera.model.KailleraUser.CONNECTION_TYPE_NAMES[connectionType] + "]";
	}

	public int getBodyLength()
	{
		//return (charset.encode(userName).remaining() + 8);
		return (getNumBytes(userName) + 8);
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, userName, 0x00, charset);
		UnsignedUtil.putUnsignedShort(buffer, userID);
		UnsignedUtil.putUnsignedInt(buffer, ping);
		buffer.put(connectionType);
	}

	public static UserJoined parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 9)
			throw new ParseException("Failed byte count validation!");

		String userName = EmuUtil.readString(buffer, 0x00, charset);
		
		
		if (buffer.remaining() < 7)
			throw new ParseException("Failed byte count validation!");
		
		int userID = UnsignedUtil.getUnsignedShort(buffer);
		long ping = UnsignedUtil.getUnsignedInt(buffer);
		byte connectionType = buffer.get();

		return new UserJoined(messageNumber, userName, userID, ping, connectionType);
	}
}

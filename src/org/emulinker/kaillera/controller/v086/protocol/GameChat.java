package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.EmuUtil;

public abstract class GameChat extends V086Message
{
	public static final byte	ID	= 0x08;

	private String				userName;
	private String				message;

	public GameChat(int messageNumber, String userName, String message) throws MessageFormatException
	{
		super(messageNumber);

		this.userName = userName;
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

	public String getMessage()
	{
		return message;
	}

	public abstract String toString();

	public int getBodyLength()
	{
		//return (charset.encode(userName).remaining() + charset.encode(message).remaining() + 2);
		return (userName.length() + message.length() + 2);
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, userName, 0x00, charset);
		EmuUtil.writeString(buffer, message, 0x00, charset);
	}

	public static GameChat parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 3)
			throw new ParseException("Failed byte count validation!");

		String userName = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 2)
			throw new ParseException("Failed byte count validation!");
		
		
		String message = EmuUtil.readString(buffer, 0x00, charset);

		if (userName.length() == 0)
			return new GameChat_Request(messageNumber, message);
		else
			return new GameChat_Notification(messageNumber, userName, message);
	}
}

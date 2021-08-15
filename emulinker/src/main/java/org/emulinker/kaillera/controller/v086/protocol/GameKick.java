package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class GameKick extends V086Message
{
	public static final byte	ID		= 0x0F;
	public static final String	DESC	= "Game Kick Request";

	private int					userID;

	public GameKick(int messageNumber, int userID) throws MessageFormatException
	{
		super(messageNumber);

		if (userID < 0 || userID > 0xFFFF)
			throw new MessageFormatException("Invalid " + getDescription() + " format: userID out of acceptable range: " + userID);

		this.userID = userID;
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public int getUserID()
	{
		return userID;
	}

	public String toString()
	{
		return getInfoString() + "[userID=" + userID + "]";
	}

	public int getBodyLength()
	{
		return 3;
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		buffer.put((byte) 0x00);
		UnsignedUtil.putUnsignedShort(buffer, userID);
	}

	public static GameKick parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 3)
			throw new ParseException("Failed byte count validation!");

		
		byte b = buffer.get();
		/*SF MOD
		if (b != 0x00)
			throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));
		*/
		return new GameKick(messageNumber, UnsignedUtil.getUnsignedShort(buffer));
	}
}

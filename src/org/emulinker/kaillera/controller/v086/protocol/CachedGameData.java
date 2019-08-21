package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.UnsignedUtil;

public class CachedGameData extends V086Message
{
	public static final byte	ID		= 0x13;
	public static final String	DESC	= "Cached Game Data";

	private int					key;

	public CachedGameData(int messageNumber, int key) throws MessageFormatException
	{
		super(messageNumber);
		this.key = key;
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public int getKey()
	{
		return key;
	}

	public String toString()
	{
		return getInfoString() + "[key=" + key + "]";
	}

	public int getBodyLength()
	{
		return 2;
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		buffer.put((byte) 0x00);
		UnsignedUtil.putUnsignedByte(buffer, key);
	}

	public static CachedGameData parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 2)
			throw new ParseException("Failed byte count validation!");

		byte b = buffer.get();
		// removed to increase speed
		//		if (b != 0x00)
		//			throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

		return new CachedGameData(messageNumber, UnsignedUtil.getUnsignedByte(buffer));
	}
}

package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class ServerACK extends ACK
{
	public static final byte	ID		= 0x05;
	public static final String	DESC	= "Server to Client ACK";

	public ServerACK(int messageNumber) throws MessageFormatException
	{
		super(messageNumber, 0, 1, 2, 3);
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public static ServerACK parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 17)
			throw new ParseException("Failed byte count validation!");

		byte b = buffer.get();
		
		if (b != 0x00)
			throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

		long val1 = UnsignedUtil.getUnsignedInt(buffer);
		long val2 = UnsignedUtil.getUnsignedInt(buffer);
		long val3 = UnsignedUtil.getUnsignedInt(buffer);
		long val4 = UnsignedUtil.getUnsignedInt(buffer);

		if (val1 != 0 || val2 != 1 || val3 != 2 || val4 != 3)
			throw new MessageFormatException("Invalid " + DESC + " format: bytes do not match acceptable format!");
		
		return new ServerACK(messageNumber);
	}
}

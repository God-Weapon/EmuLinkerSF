package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.EmuUtil;

public class InformationMessage extends V086Message
{
	public static final byte	ID		= 0x17;
	public static final String	DESC	= "Information Message";

	private String				source;
	private String				message;

	public InformationMessage(int messageNumber, String source, String message) throws MessageFormatException
	{
		super(messageNumber);

		if (source.length() == 0)
			throw new MessageFormatException("Invalid " + getDescription() + " format: source.length == 0");

		if (message.length() == 0)
			throw new MessageFormatException("Invalid " + getDescription() + " format: message.length == 0");

		this.source = source;
		this.message = message;
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public String getSource()
	{
		return source;
	}

	public String getMessage()
	{
		return message;
	}

	public String toString()
	{
		return getInfoString() + "[source: " + source + " message: " + message + "]";
	}

	public int getBodyLength()
	{
		return getNumBytes(source) + getNumBytes(message) + 2;
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		EmuUtil.writeString(buffer, source, 0x00, charset);
		EmuUtil.writeString(buffer, message, 0x00, charset);
	}

	public static InformationMessage parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 4)
			throw new ParseException("Failed byte count validation!");

		String source = EmuUtil.readString(buffer, 0x00, charset);

		
		if (buffer.remaining() < 2)
			throw new ParseException("Failed byte count validation!");
		
		String message = EmuUtil.readString(buffer, 0x00, charset);

		return new InformationMessage(messageNumber, source, message);
	}
}

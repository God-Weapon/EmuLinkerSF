package org.emulinker.kaillera.controller.connectcontroller.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.*;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.EmuUtil;

public abstract class ConnectMessage extends ByteBufferMessage
{
	public static Charset	charset	= Charset.forName("ISO-8859-1");

	protected abstract String getID();

	public static ConnectMessage parse(ByteBuffer buffer) throws MessageFormatException
	{
		String messageStr = null;

		try
		{
			CharsetDecoder stringDecoder = charset.newDecoder();
			messageStr = stringDecoder.decode(buffer).toString();
		}
		catch (CharacterCodingException e)
		{
			throw new MessageFormatException("Invalid bytes received: failed to decode to a string!", e);
		}

		if (messageStr.startsWith(ConnectMessage_TOO.ID))
		{
			return ConnectMessage_TOO.parse(messageStr);
		}
		else if (messageStr.startsWith(ConnectMessage_HELLOD00D.ID))
		{
			return ConnectMessage_HELLOD00D.parse(messageStr);
		}
		else if (messageStr.startsWith(ConnectMessage_HELLO.ID))
		{
			return ConnectMessage_HELLO.parse(messageStr);
		}
		else if (messageStr.startsWith(ConnectMessage_PING.ID))
		{
			return ConnectMessage_PING.parse(messageStr);
		}
		else if (messageStr.startsWith(ConnectMessage_PONG.ID))
		{
			return ConnectMessage_PONG.parse(messageStr);
		}

		buffer.rewind();
		throw new MessageFormatException("Unrecognized connect message: " + EmuUtil.dumpBuffer(buffer));
	}
}

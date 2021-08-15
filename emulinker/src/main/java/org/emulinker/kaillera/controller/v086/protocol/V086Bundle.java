package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class V086Bundle extends ByteBufferMessage
{
	public static final String	DESC	= "Kaillera v.086 Message Bundle";

	//protected List<V086Message>	messages;
	protected V086Message[]		messages;
	protected int				numToWrite;
	protected int				length	= -1;

	public V086Bundle(V086Message[] messages)
	{
		this(messages, Integer.MAX_VALUE);
	}

	public V086Bundle(V086Message[] messages, int numToWrite)
	{
		this.numToWrite = messages.length;
		if (numToWrite < this.numToWrite)
			this.numToWrite = numToWrite;

		this.messages = messages;
	}

	public String getDescription()
	{
		return DESC;
	}

	public int getNumMessages()
	{
		return numToWrite;
	}

	public V086Message[] getMessages()
	{
		return messages;
	}

	public int getLength()
	{
		if (length == -1)
		{
			for (int i = 0; i < numToWrite; i++)
			{
				if (messages[i] == null)
					break;

				length += messages[i].getLength();
			}
		}
		return length;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(DESC + " (" + numToWrite + " messages) (" + getLength() + " bytes)");
		sb.append(EmuUtil.LB);
		for (int i = 0; i < numToWrite; i++)
		{
			if (messages[i] == null)
				break;

			sb.append("\tMessage " + (i + 1) + ": " + messages[i].toString() + EmuUtil.LB);
		}
		return sb.toString();
	}

	public void writeTo(ByteBuffer buffer)
	{
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// no real need for unsigned
		//UnsignedUtil.putUnsignedByte(buffer, numToWrite);
		buffer.put((byte) numToWrite);
		for (int i = 0; i < numToWrite; i++)
		{
			if (messages[i] == null)
				break;

			messages[i].writeTo(buffer);
		}
	}

	public static V086Bundle parse(ByteBuffer buffer) throws ParseException, V086BundleFormatException, MessageFormatException
	{
		return parse(buffer, -1);
	}

	public static V086Bundle parse(ByteBuffer buffer, int lastMessageID) throws ParseException, V086BundleFormatException, MessageFormatException
	{
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		if (buffer.limit() < 5)
			throw new V086BundleFormatException("Invalid buffer length: " + buffer.limit());

		// again no real need for unsigned
		//int messageCount = UnsignedUtil.getUnsignedByte(buffer);
		int messageCount = buffer.get();

		if (messageCount <= 0 || messageCount > 32) // what should the max be?
			throw new V086BundleFormatException("Invalid message count: " + messageCount);

		if (buffer.limit() < (1 + (messageCount * 6)))
			throw new V086BundleFormatException("Invalid bundle length: " + buffer.limit());


		int parsedCount = 0;
		V086Message[] messages;
		
		int msgNum = buffer.getChar(1);//buffer.getShort(1); - mistake. max value of short is 0x7FFF but we need 0xFFFF
		if((msgNum - 1) == lastMessageID || msgNum == 0 && lastMessageID == 0xFFFF){// exception for 0 and 0xFFFF
			messageCount = 1;
			messages = new V086Message[messageCount];
			int messageNumber = UnsignedUtil.getUnsignedShort(buffer);
			
			short messageLength = buffer.getShort();
			if (messageLength < 2 || messageLength > buffer.remaining())// || messageLength > buffer.limit())
				throw new ParseException("Invalid message length: " + messageLength);
			
			messages[parsedCount] = V086Message.parse(messageNumber, messageLength, buffer);			
			parsedCount++;
		}
		else{
			messages = new V086Message[messageCount];
			for(parsedCount = 0; parsedCount < messageCount; parsedCount++){
				int messageNumber = UnsignedUtil.getUnsignedShort(buffer);
				
				if (messageNumber <= lastMessageID){
					if (messageNumber < 0x20 && lastMessageID > 0xFFDF) {
						// exception when messageNumber with lower value is greater
						// do nothing
					}
					else {
						break;
					}
				}
				else if (messageNumber > 0xFFBF && lastMessageID < 0x40) {
					// exception when disorder messageNumber greater that lastMessageID
					break;
				}
	
				short messageLength = buffer.getShort();
				if (messageLength < 2 || messageLength > buffer.remaining())// || messageLength > buffer.limit())
					throw new ParseException("Invalid message length: " + messageLength);
				
				messages[parsedCount] = V086Message.parse(messageNumber, messageLength, buffer);
			}
		}
		
		return new V086Bundle(messages, parsedCount);
	}
}

package org.emulinker.kaillera.controller.messaging;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.apache.commons.logging.*;

public abstract class ByteBufferMessage
{
	protected static Log		log		= LogFactory.getLog(ByteBufferMessage.class);
//	public static Charset	charset	= Charset.forName("US-ASCII");
//	public static Charset	charset	= Charset.forName("ISO-8859-1");
//	public static Charset	charset	= Charset.forName("UTF-8");
	public static Charset	charset = Charset.defaultCharset();

	static
	{
		String charsetName = System.getProperty("emulinker.charset");
		if (charsetName != null)
		{
			try
			{
				if (Charset.isSupported(charsetName))
					charset = Charset.forName(charsetName);
				else
					log.fatal("Charset " + charsetName + " is not supported!");
			}
			catch (Exception e)
			{
				log.fatal("Failed to load charset " + charsetName + ": " + e.getMessage(), e);
			}
		}
		
		log.info("Using character set: " + charset.displayName());
	}

	private ByteBuffer		buffer;

	public abstract int getLength();

	public abstract String getDescription();

	public abstract String toString();

	protected void initBuffer()
	{
		initBuffer(getLength());
	}

	private void initBuffer(int size)
	{
		buffer = getBuffer(size);
	}

	public void releaseBuffer()
	{

	}

	public ByteBuffer toBuffer()
	{
		initBuffer();
		writeTo(buffer);
		buffer.flip();
		return buffer;
	}

	public abstract void writeTo(ByteBuffer buffer);

	public static ByteBuffer getBuffer(int size)
	{
		return ByteBuffer.allocateDirect(size);
	}

	public static void releaseBuffer(ByteBuffer buffer)
	{
		// nothing to do since we aren't caching buffers anymore
	}
}

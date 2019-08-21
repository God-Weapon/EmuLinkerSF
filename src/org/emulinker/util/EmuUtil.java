package org.emulinker.util;

import java.io.*;
import java.text.*;
import java.lang.reflect.Constructor;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Properties;

public class EmuUtil
{
	private static final char[]	HEX_CHARS	= { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static final String	LB			= System.getProperty("line.separator");
	
	public static DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

	public static boolean systemIsWindows()
	{
		if (File.separatorChar == '\\')
			return true;
		return false;
	}

	public static Properties loadProperties(String filename)
	{
		try
		{
			File file = new File(filename);
			return loadProperties(file);
		}
		catch (Exception e)
		{
			// log some kind of error here
			return null;
		}
	}

	public static Properties loadProperties(File file)
	{
		Properties p = null;
		try
		{
			FileInputStream in = new FileInputStream(file);
			p = new Properties();
			p.load(in);
			in.close();
		}
		catch (Throwable e)
		{
			// log the error
		}
		return p;
	}

	public static String formatBytes(byte[] data)
	{
		return formatBytes(data, false);
	}

	public static String formatBytes(byte[] data, boolean allHex)
	{
		if (data == null)
			return "null";

		if (data.length == 0)
			return "";

		if (allHex)
			return bytesToHex(data, ',');

		int len = data.length;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++)
		{
			if (Character.isLetterOrDigit((char) data[i]) || (data[i] >= 32 && data[i] <= 126))
				sb.append((char) data[i]);
			else
				sb.append(byteToHex(data[i]));

			if (i < (len - 1))
				sb.append(',');
		}

		return sb.toString();
	}

	public static String bytesToHex(byte[] data, char sep)
	{
		int len = data.length;
		StringBuilder sb = new StringBuilder((len * 3));
		for (int i = 0; i < len; i++)
		{
			sb.append(byteToHex(data[i]));
			if (i < (len - 1))
				sb.append(sep);
		}

		return sb.toString();
	}

	public static String bytesToHex(byte[] data)
	{
		if(data == null)
			return "null";
		
		int len = data.length;
		StringBuilder sb = new StringBuilder((len * 3));
		for (int i = 0; i < len; i++)
		{
			sb.append(byteToHex(data[i]));
		}

		return sb.toString();
	}

	public static String bytesToHex(byte[] data, int pos, int len)
	{
		if(data == null)
			return "null";
		
		StringBuilder sb = new StringBuilder((len * 2));
		for (int i = pos; i < (pos+len); i++)
		{
			sb.append(byteToHex(data[i]));
		}

		return sb.toString();
	}

	public static String byteToHex(byte b)
	{
		return Character.toString(HEX_CHARS[((b & 0xf0) >> 4)]) + Character.toString(HEX_CHARS[(b & 0xf)]);
	}

	public static byte[] hexToByteArray(String hex) throws NumberFormatException
	{
		if ((hex.length() % 2) != 0)
			throw new NumberFormatException("The string has the wrong length, not pairs of hex representations.");

		int len = (hex.length() / 2);
		byte[] ba = new byte[len];
		int pos = 0;
		for (int i = 0; i < len; i++)
		{
			ba[i] = hexToByte(hex.substring(pos, pos + 2).toCharArray());
			pos += 2;
		}

		return ba;
	}

	public static byte hexToByte(char[] hex) throws NumberFormatException
	{
		if (hex.length != 2)
			throw new NumberFormatException("Invalid number of digits in " + new String(hex));

		int i = 0;
		byte nibble;

		if ((hex[i] >= '0') && (hex[i] <= '9'))
		{
			nibble = (byte) ((hex[i] - '0') << 4);
		}
		else if ((hex[i] >= 'A') && (hex[i] <= 'F'))
		{
			nibble = (byte) ((hex[i] - ('A' - 0x0A)) << 4);
		}
		else if ((hex[i] >= 'a') && (hex[i] <= 'f'))
		{
			nibble = (byte) ((hex[i] - ('a' - 0x0A)) << 4);
		}
		else
		{
			throw new NumberFormatException(hex[i] + " is not a hexadecimal string.");
		}

		i++;

		if ((hex[i] >= '0') && (hex[i] <= '9'))
		{
			nibble = (byte) (nibble | (hex[i] - '0'));
		}
		else if ((hex[i] >= 'A') && (hex[i] <= 'F'))
		{
			nibble = (byte) (nibble | (hex[i] - ('A' - 0x0A)));
		}
		else if ((hex[i] >= 'a') && (hex[i] <= 'f'))
		{
			nibble = (byte) (nibble | (hex[i] - ('a' - 0x0A)));
		}
		else
		{
			throw new NumberFormatException(hex[i] + " is not a hexadecimal string.");
		}

		return nibble;
	}

	public static String arrayToString(int[] array, char sep)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++)
		{
			sb.append(array[i]);
			if (i < (array.length - 1))
				sb.append(sep);
		}
		return sb.toString();
	}

	public static String arrayToString(byte[] array, char sep)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++)
		{
			sb.append(array[i]);
			if (i < (array.length - 1))
				sb.append(sep);
		}
		return sb.toString();
	}

	public static String formatSocketAddress(SocketAddress sa)
	{
		return ((InetSocketAddress) sa).getAddress().getHostAddress() + ":" + ((InetSocketAddress) sa).getPort();
	}

	public static String dumpBuffer(ByteBuffer buffer)
	{
		return dumpBuffer(buffer, false);
	}

	public static String dumpBuffer(ByteBuffer buffer, boolean allHex)
	{
		StringBuilder sb = new StringBuilder();
		buffer.mark();
		while (buffer.hasRemaining())
		{
			byte b = buffer.get();
			if (!allHex && Character.isLetterOrDigit((char) b))
				sb.append((char) b);
			else
				sb.append(byteToHex(b));
			if (buffer.hasRemaining())
				sb.append(",");
		}
		buffer.reset();
		return sb.toString();
	}

	public static String readString(ByteBuffer buffer, int stopByte, Charset charset)
	{
		//ByteBuffer tempBuffer = ByteBuffer.allocate(buffer.remaining());
		char[] tempArray = new char[buffer.remaining()];
		byte b;
		int  i;
//		while (buffer.hasRemaining())
		for(i=0; i<tempArray.length; i++)
		{
			if ((b = buffer.get()) == stopByte)
				break;
//			tempBuffer.put(b);
			tempArray[i] = (char)b;
		}
//		return charset.decode((ByteBuffer) tempBuffer.flip()).toString();
		return new String(tempArray, 0, i);
	}

	public static void writeString(ByteBuffer buffer, String s, int stopByte, Charset charset)
	{
//		buffer.put(charset.encode(s));
		char[] tempArray = s.toCharArray();
		for(int i=0; i<tempArray.length; i++)
			buffer.put((byte) tempArray[i]);
		buffer.put((byte) stopByte);
	}

	public static Object construct(String className, Object args[]) throws InstantiationException
	{
		try
		{
			Class c = Class.forName(className);
			Class[] contructorArgs = new Class[args.length];
			for (int i = 0; i < args.length; i++)
				contructorArgs[i] = args[i].getClass();
			Constructor constructor = c.getConstructor(contructorArgs);
			return constructor.newInstance(args);
		}
		catch (Exception e)
		{
			throw new InstantiationException("Problem constructing new " + className + ": " + e.getMessage());
		}
	}
}

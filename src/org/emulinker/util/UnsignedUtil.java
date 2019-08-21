package org.emulinker.util;

import java.nio.ByteBuffer;

public class UnsignedUtil
{
	public static short getUnsignedByte(ByteBuffer bb)
	{
		return ((short) (bb.get() & 0xff));
	}

	public static void putUnsignedByte(ByteBuffer bb, int value)
	{
		bb.put((byte) (value & 0xff));
	}

	public static short getUnsignedByte(ByteBuffer bb, int position)
	{
		return ((short) (bb.get(position) & (short) 0xff));
	}

	public static void putUnsignedByte(ByteBuffer bb, int position, int value)
	{
		bb.put(position, (byte) (value & 0xff));
	}

	// ---------------------------------------------------------------

	public static int getUnsignedShort(ByteBuffer bb)
	{
		return (bb.getShort() & 0xffff);
	}

	public static void putUnsignedShort(ByteBuffer bb, int value)
	{
		bb.putShort((short) (value & 0xffff));
	}

	public static int getUnsignedShort(ByteBuffer bb, int position)
	{
		return (bb.getShort(position) & 0xffff);
	}

	public static void putUnsignedShort(ByteBuffer bb, int position, int value)
	{
		bb.putShort(position, (short) (value & 0xffff));
	}

	// ---------------------------------------------------------------

	public static long getUnsignedInt(ByteBuffer bb)
	{
		return ((long) bb.getInt() & 0xffffffffL);
	}

	public static void putUnsignedInt(ByteBuffer bb, long value)
	{
		bb.putInt((int) (value & 0xffffffffL));
	}

	public static long getUnsignedInt(ByteBuffer bb, int position)
	{
		return ((long) bb.getInt(position) & 0xffffffffL);
	}

	public static void putUnsignedInt(ByteBuffer bb, int position, long value)
	{
		bb.putInt(position, (int) (value & 0xffffffffL));
	}

	// -----------------

	public static short readUnsignedByte(byte[] bytes, int offset)
	{
		return (short) (((int) bytes[offset]) & 0xFF);
	}

	public static void writeUnsignedByte(short s, byte[] bytes, int offset)
	{
		bytes[offset] = (byte) (((int) s) & 0xFF);
	}

	public static int readUnsignedShort(byte[] bytes, int offset)
	{
		return readUnsignedShort(bytes, offset, false);
	}

	public static int readUnsignedShort(byte[] bytes, int offset, boolean littleEndian)
	{
		return (littleEndian) ? ((bytes[offset + 1] & 0xFF) << 8) + (bytes[offset] & 0xFF) : ((bytes[offset] & 0xFF) << 8) + (bytes[offset + 1] & 0xFF);
	}

	public static void writeUnsignedShort(int s, byte[] bytes, int offset)
	{
		writeUnsignedShort(s, bytes, offset);
	}

	public static void writeUnsignedShort(int s, byte[] bytes, int offset, boolean littleEndian)
	{
		if (littleEndian)
		{
			bytes[offset] = (byte) (s & 0xFF);
			bytes[offset + 1] = (byte) ((s >>> 8) & 0xFF);
		}
		else
		{
			bytes[offset] = (byte) ((s >>> 8) & 0xFF);
			bytes[offset + 1] = (byte) (s & 0xFF);
		}
	}

	public static long readUnsignedInt(byte[] bytes, int offset)
	{
		return readUnsignedInt(bytes, offset, false);
	}

	public static long readUnsignedInt(byte[] bytes, int offset, boolean littleEndian)
	{
		int i1 = bytes[offset + 0] & 0xFF;
		int i2 = bytes[offset + 1] & 0xFF;
		int i3 = bytes[offset + 2] & 0xFF;
		int i4 = bytes[offset + 3] & 0xFF;
		return (littleEndian) ? (i4 << 24) + (i3 << 16) + (i2 << 8) + i1 : (i1 << 24) + (i2 << 16) + (i3 << 8) + i4;
	}

	public static void writeUnsignedInt(long i, byte[] bytes, int offset)
	{
		writeUnsignedInt(i, bytes, offset, false);
	}

	public static void writeUnsignedInt(long i, byte[] bytes, int offset, boolean littleEndian)
	{
		if (littleEndian)
		{
			bytes[offset + 0] = (byte) (i & 0xFF);
			bytes[offset + 1] = (byte) ((i >>> 8) & 0xFF);
			bytes[offset + 2] = (byte) ((i >>> 16) & 0xFF);
			bytes[offset + 3] = (byte) ((i >>> 24) & 0xFF);
		}
		else
		{
			bytes[offset + 0] = (byte) ((i >>> 24) & 0xFF);
			bytes[offset + 1] = (byte) ((i >>> 16) & 0xFF);
			bytes[offset + 2] = (byte) ((i >>> 8) & 0xFF);
			bytes[offset + 3] = (byte) (i & 0xFF);
		}
	}
}

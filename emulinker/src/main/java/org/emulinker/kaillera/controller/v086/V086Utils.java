package org.emulinker.kaillera.controller.v086;

import java.nio.ByteBuffer;

/** Util methods mostly for dealing ByteBuffers. */
public final class V086Utils
{
    private final static String HEX_STRING = "0123456789abcdef";

    public static ByteBuffer hexStringToByteBuffer(String hex)
    {
        hex = hex.replace(" ", "");
        byte[] bytes = hexStringToByteArray2(hex);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.position(0);
        return buffer;
    }

    public static byte[] hexStringToByteArray2(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes)
    {
        char[] hex_array = HEX_STRING.toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hex_array[v >>> 4];
            hexChars[j * 2 + 1] = hex_array[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String toHex(ByteBuffer bb)
    {
        StringBuilder sb = new StringBuilder();
        while (bb.hasRemaining())
        {
            sb.append(String.format("%02X", bb.get()));
        }
        return sb.toString();
    }

    public static ByteBuffer clone(ByteBuffer original)
    {
        ByteBuffer clone = ByteBuffer.allocate(original.capacity());
        original.rewind();
        clone.put(original);
        original.rewind();
        clone.flip();
        return clone;
    }
}

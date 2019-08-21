package org.emulinker.util;

public interface GameDataCache
{
	public byte[] get(int index);

	public int add(byte[] data);

	public int indexOf(byte[] data);

	public int size();

	public boolean isEmpty();

	public void clear();

	public boolean contains(byte[] data);

	public byte[] set(int index, byte[] data);

	public byte[] remove(int index);
}

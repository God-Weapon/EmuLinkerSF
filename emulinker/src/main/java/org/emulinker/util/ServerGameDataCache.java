package org.emulinker.util;

import java.util.*;

// Adapted from http://www.smotricz.com/kabutz/Issue027.html

public class ServerGameDataCache implements GameDataCache
{
	// array holds the elements
	protected byte[][]					array;

	// hashmap for quicker indexOf access, but slows down inserts
	//	protected HashMap<byte[], Integer>	map;
	protected HashMap<Integer, Integer>	map;

	// head points to the first logical element in the array, and
	// tail points to the element following the last. This means
	// that the list is empty when head == tail. It also means
	// that the array array has to have an extra space in it.
	protected int						head	= 0, tail = 0;

	// Strictly speaking, we don't need to keep a handle to size,
	// as it can be calculated programmatically, but keeping it
	// makes the algorithms faster.
	protected int						size	= 0;

	// fixed size, goes not grow
	public ServerGameDataCache(int size)
	{
		array = new byte[size][];
		map = new HashMap<Integer, Integer>(size, .05f);
	}

	public String toString()
	{
		return "ServerGameDataCache[size=" + size + " head=" + head + " tail=" + tail + "]";
	}

	public boolean isEmpty()
	{
		return (head == tail); // or size == 0
	}

	public int size()
	{
		// the size can also be worked out each time as: (tail + array.length -
		// head) % array.length
		return size;
	}

	public boolean contains(byte[] data)
	{
		return indexOf(data) >= 0;
	}

	public int indexOf(byte[] data)
	{
		//		Integer i = map.get(Arrays.toString(data));
		Integer i = map.get(Arrays.hashCode(data));
		return (i == null ? -1 : unconvert(i));
	}

	public byte[] get(int index)
	{
		rangeCheck(index);
		return array[convert(index)];
	}

	public byte[] set(int index, byte[] data)
	{
		rangeCheck(index);
		int convertedIndex = convert(index);
		byte[] oldValue = array[convertedIndex];
		array[convertedIndex] = data;
		//		map.put(Arrays.toString(data), convertedIndex);
		map.put(Arrays.hashCode(data), convertedIndex);
		return oldValue;
	}

	// This method is the main reason we re-wrote the class.
	// It is optimized for removing first and last elements
	// but also allows you to remove in the middle of the list.
	public byte[] remove(int index)
	{
		rangeCheck(index);

		int pos = convert(index);

		try
		{
			//			map.remove(array[pos]);
			map.remove(Arrays.hashCode(array[pos]));
			//			map.remove(Arrays.toString(array[pos]));
			return array[pos];
		}
		finally
		{
			array[pos] = null; // Let gc do its work

			// optimized for FIFO access, i.e. adding to back and
			// removing from front
			if (pos == head)
				head = (head + 1) % array.length;
			else if (pos == tail)
				tail = (tail - 1 + array.length) % array.length;
			else
			{
				if (pos > head && pos > tail)
				{ // tail/head/pos
					System.arraycopy(array, head, array, head + 1, pos - head);
					head = (head + 1) % array.length;
				}
				else
				{
					System.arraycopy(array, pos + 1, array, pos, tail - pos - 1);
					tail = (tail - 1 + array.length) % array.length;
				}
			}
			size--;
		}
	}

	public void clear()
	{
		for (int i = 0; i < size; i++)
			array[convert(i)] = null;

		head = tail = size = 0;
		map.clear();
	}

	public int add(byte[] data)
	{
		if (size == array.length)
			remove(0);

		int pos = tail;
		array[tail] = data;
		//		map.put(Arrays.toString(data), tail);
		//		map.put(data, tail);
		map.put(Arrays.hashCode(data), tail);
		tail = ((tail + 1) % array.length);
		size++;

		return unconvert(pos);
	}

	// The convert() method takes a logical index (as if head was always 0) and
	// calculates the index within array
	protected int convert(int index)
	{
		return ((index + head) % array.length);
	}

	// there gotta be a better way to do this but I can't figure it out
	protected int unconvert(int index)
	{
		if (index >= head)
			return (index - head);
		else
			return ((array.length - head) + index);
	}

	protected void rangeCheck(int index)
	{
		if (index >= size || index < 0)
			throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
	}
}

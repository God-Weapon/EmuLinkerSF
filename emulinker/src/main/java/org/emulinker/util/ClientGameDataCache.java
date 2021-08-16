package org.emulinker.util;

import java.util.Arrays;

// This is a very specialized structure designed for quickly caching int arrays during game play
// Adapted from http://www.smotricz.com/kabutz/Issue027.html

public class ClientGameDataCache implements GameDataCache {
  // array holds the elements
  protected byte[][] array;

  // head points to the first logical element in the array, and
  // tail points to the element following the last. This means
  // that the list is empty when head == tail. It also means
  // that the array array has to have an extra space in it.
  protected int head = 0, tail = 0;

  // Strictly speaking, we don't need to keep a handle to size,
  // as it can be calculated programmatically, but keeping it
  // makes the algorithms faster.
  protected int size = 0;

  // fixed size, goes not grow
  public ClientGameDataCache(int size) {
    array = new byte[size][];
  }

  public String toString() {
    return "ClientGameDataCache[size=" + size + " head=" + head + " tail=" + tail + "]";
  }

  public boolean isEmpty() {
    return (head == tail); // or size == 0
  }

  public int size() {
    // the size can also be worked out each time as: (tail + array.length -
    // head) % array.length
    return size;
  }

  public boolean contains(byte[] data) {
    return indexOf(data) >= 0;
  }

  public int indexOf(byte[] data) {
    for (int i = 0; i < size; i++) if (Arrays.equals(data, array[convert(i)])) return i;
    return -1;
  }

  public byte[] get(int index) {
    rangeCheck(index);
    return array[convert(index)];
  }

  public byte[] set(int index, byte[] data) {
    rangeCheck(index);
    int convertedIndex = convert(index);
    byte[] oldValue = array[convertedIndex];
    array[convertedIndex] = data;
    return oldValue;
  }

  // This method is the main reason we re-wrote the class.
  // It is optimized for removing first and last elements
  // but also allows you to remove in the middle of the list.
  public byte[] remove(int index) {
    rangeCheck(index);

    int pos = convert(index);

    try {
      return array[pos];
    } finally {
      array[pos] = null; // Let gc do its work

      // optimized for FIFO access, i.e. adding to back and
      // removing from front
      if (pos == head) {
        // head = (head + 1) % array.length;
        head++;
        if (head == array.length) head = 0;
      } else if (pos == tail) {
        tail = (tail - 1 + array.length) % array.length;
      } else {
        if (pos > head && pos > tail) { // tail/head/pos
          System.arraycopy(array, head, array, head + 1, pos - head);
          // head = (head + 1) % array.length;
          head++;
          if (head == array.length) head = 0;
        } else {
          System.arraycopy(array, pos + 1, array, pos, tail - pos - 1);
          tail = (tail - 1 + array.length) % array.length;
        }
      }
      size--;
    }
  }

  public void clear() {
    for (int i = 0; i < size; i++) array[convert(i)] = null;

    head = tail = size = 0;
  }

  public int add(byte[] data) {
    if (size == array.length) remove(0);

    int pos = tail;
    array[tail] = data;

    // tail = ((tail + 1) % array.length);
    tail++;
    if (tail == array.length) tail = 0;

    size++;

    return unconvert(pos);
  }

  // The convert() method takes a logical index (as if head was always 0) and
  // calculates the index within array
  protected int convert(int index) {

    return ((index + head) % array.length);
  }

  // there gotta be a better way to do this but I can't figure it out
  protected int unconvert(int index) {
    if (index >= head) return (index - head);
    else return ((array.length - head) + index);
  }

  protected void rangeCheck(int index) {
    if (index >= size || index < 0)
      throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
  }
}

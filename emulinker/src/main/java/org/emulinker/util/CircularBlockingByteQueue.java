package org.emulinker.util;

import java.util.concurrent.*;
import org.apache.commons.logging.*;

// This is a very specialized structure designed for queueing ints during gameplay
// Adapted from http://www.smotricz.com/kabutz/Issue027.html

public final class CircularBlockingByteQueue {
  private static Log log = LogFactory.getLog(CircularBlockingByteQueue.class);

  // array holds the elements
  private byte[] array;

  // head points to the first logical element in the array, and
  // tail points to the element following the last. This means
  // that the list is empty when head == tail. It also means
  // that the array array has to have an extra space in it.
  private int head = 0, tail = 0;

  // Strictly speaking, we don't need to keep a handle to size,
  // as it can be calculated programmatically, but keeping it
  // makes the algorithms faster.
  private int size = 0;

  private Semaphore semaphore = new Semaphore(0);
  private Object lock = new Object();

  // fixed size, goes not grow
  public CircularBlockingByteQueue(int maxSize) {
    array = new byte[maxSize];
  }

  public String toString() {
    synchronized (lock) {
      return "CircularBlockingIntArray[size=" + size + " head=" + head + " tail=" + tail + "]";
    }
  }

  public boolean isEmpty() {
    synchronized (lock) {
      return (head == tail); // or size == 0
    }
  }

  public int size() {
    // the size can also be worked out each time as: (tail + array.length - head) % array.length
    synchronized (lock) {
      return size;
    }
  }

  public byte get(long timeout, TimeUnit unit) throws TimeoutException {
    try {
      if (!semaphore.tryAcquire(timeout, unit)) throw new TimeoutException();
    } catch (InterruptedException e) {
      return -1;
    }

    synchronized (lock) {
      return remove(0);
    }
  }

  public void put(byte data) {
    //		log.debug(this + " put("+data+")");
    synchronized (lock) {
      if (size == array.length) grow();

      array[tail] = data;
      tail = ((tail + 1) % array.length);

      size++;
    }

    //		log.debug("semaphore.release()");
    semaphore.release();
  }

  public byte remove(int index) {
    //		log.debug(this + " remove("+index+")");
    int pos = convert(index);

    try {
      return array[pos];
    } finally {
      array[pos] = -1; // Let gc do its work

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

  public void grow() {
    //		log.debug(this + " grow()");
    int oldCapacity = array.length;
    int newCapacity = (oldCapacity * 3) / 2 + 1;
    log.debug("CircularBlockingByteQueue growing from " + oldCapacity + " to " + newCapacity);
    byte newData[] = new byte[newCapacity];
    toArray(newData);
    tail = size;
    head = 0;
    array = newData;
  }

  public byte[] toArray() {
    //		log.debug(this + " toArray()");

    return toArray(new byte[size]);
  }

  public byte[] toArray(byte a[]) {
    //		log.debug(this + " toArray("+Arrays.toString(a)+")");

    if (size == 0) return a;

    if (a.length < size)
      a = (byte[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

    if (head < tail) {
      System.arraycopy(array, head, a, 0, tail - head);
    } else {
      System.arraycopy(array, head, a, 0, array.length - head);
      System.arraycopy(array, 0, a, array.length - head, tail);
    }

    if (a.length > size) {
      a[size] = 0;
    }

    return a;
  }

  // The convert() method takes a logical index (as if head was always 0) and
  // calculates the index within array
  protected int convert(int index) {

    return ((index + head) % array.length);
  }
}

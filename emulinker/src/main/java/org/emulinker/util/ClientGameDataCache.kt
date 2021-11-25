package org.emulinker.util

import java.lang.IndexOutOfBoundsException
import java.util.Arrays

// This is a very specialized structure designed for quickly caching int arrays during game play
// Adapted from http://www.smotricz.com/kabutz/Issue027.html
class ClientGameDataCache(size: Int) : GameDataCache {
  // array holds the elements
  private var array: Array<ByteArray?> = arrayOfNulls(size)

  // head points to the first logical element in the array, and
  // tail points to the element following the last. This means
  // that the list is empty when head == tail. It also means
  // that the array array has to have an extra space in it.
  private var head = 0
  private var tail = 0

  // Strictly speaking, we don't need to keep a handle to size,
  // as it can be calculated programmatically, but keeping it
  // makes the algorithms faster.
  // the size can also be worked out each time as: (tail + array.length -
  // head) % array.length
  override var size = 0
    private set

  override fun toString(): String {
    return "ClientGameDataCache[size=$size head=$head tail=$tail]"
  }

  // or size == 0
  override val isEmpty = head == tail // or size == 0

  override fun contains(data: ByteArray?): Boolean {
    return indexOf(data) >= 0
  }

  override fun indexOf(data: ByteArray?): Int {
    for (i in 0 until size) if (Arrays.equals(data, array[convert(i)])) return i
    return -1
  }

  override fun get(index: Int): ByteArray? {
    rangeCheck(index)
    return array[convert(index)]
  }

  override fun set(index: Int, data: ByteArray?): ByteArray? {
    rangeCheck(index)
    val convertedIndex = convert(index)
    val oldValue = array[convertedIndex]
    array[convertedIndex] = data
    return oldValue
  }

  // This method is the main reason we re-wrote the class.
  // It is optimized for removing first and last elements
  // but also allows you to remove in the middle of the list.
  override fun remove(index: Int): ByteArray? {
    rangeCheck(index)
    val pos = convert(index)
    return try {
      array[pos]
    } finally {
      array[pos] = null // Let gc do its work

      // optimized for FIFO access, i.e. adding to back and
      // removing from front
      if (pos == head) {
        // head = (head + 1) % array.length;
        head++
        if (head == array.size) head = 0
      } else if (pos == tail) {
        tail = (tail - 1 + array.size) % array.size
      } else {
        if (pos > head && pos > tail) { // tail/head/pos
          System.arraycopy(array, head, array, head + 1, pos - head)
          // head = (head + 1) % array.length;
          head++
          if (head == array.size) head = 0
        } else {
          System.arraycopy(array, pos + 1, array, pos, tail - pos - 1)
          tail = (tail - 1 + array.size) % array.size
        }
      }
      size--
    }
  }

  override fun clear() {
    for (i in 0 until size) array[convert(i)] = null
    size = 0
    tail = size
    head = tail
  }

  override fun add(data: ByteArray?): Int {
    if (size == array.size) remove(0)
    val pos = tail
    array[tail] = data

    // tail = ((tail + 1) % array.length);
    tail++
    if (tail == array.size) tail = 0
    size++
    return unconvert(pos)
  }

  // The convert() method takes a logical index (as if head was always 0) and
  // calculates the index within array
  private fun convert(index: Int): Int {
    return (index + head) % array.size
  }

  // there gotta be a better way to do this but I can't figure it out
  private fun unconvert(index: Int): Int {
    return if (index >= head) index - head else array.size - head + index
  }

  private fun rangeCheck(index: Int) {
    if (index >= size || index < 0) throw IndexOutOfBoundsException("index=$index, size=$size")
  }
}

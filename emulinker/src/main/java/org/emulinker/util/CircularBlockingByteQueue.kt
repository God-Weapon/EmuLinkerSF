package org.emulinker.util

import com.google.common.flogger.FluentLogger
import java.lang.InterruptedException
import java.lang.reflect.Array
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.Throws

private val logger = FluentLogger.forEnclosingClass()

// This is a very specialized structure designed for queueing ints during gameplay
// Adapted from http://www.smotricz.com/kabutz/Issue027.html
class CircularBlockingByteQueue(maxSize: Int) {
  // array holds the elements
  // fixed size, goes not grow
  private var array: ByteArray = ByteArray(maxSize)

  // head points to the first logical element in the array, and
  // tail points to the element following the last. This means
  // that the list is empty when head == tail. It also means
  // that the array array has to have an extra space in it.
  private var head = 0
  private var tail = 0

  // Strictly speaking, we don't need to keep a handle to size,
  // as it can be calculated programmatically, but keeping it
  // makes the algorithms faster.
  private var size = 0
  private val semaphore = Semaphore(0)
  private val lock = Any()

  override fun toString(): String {
    synchronized(lock) {
      return "CircularBlockingIntArray[size=$size head=$head tail=$tail]"
    }
  }

  // or size == 0
  val isEmpty: Boolean
    get() {
      synchronized(lock) {
        return head == tail // or size == 0
      }
    }

  fun size(): Int {
    // the size can also be worked out each time as: (tail + array.length - head) % array.length
    synchronized(lock) {
      return size
    }
  }

  @Throws(TimeoutException::class)
  operator fun get(timeout: Long, unit: TimeUnit?): Byte {
    try {
      if (!semaphore.tryAcquire(timeout, unit)) throw TimeoutException()
    } catch (e: InterruptedException) {
      return -1
    }
    synchronized(lock) {
      return remove(0)
    }
  }

  fun put(data: Byte) {
    //		logger.atFine().log(this + " put("+data+")");
    synchronized(lock) {
      if (size == array.size) grow()
      array[tail] = data
      tail = (tail + 1) % array.size
      size++
    }

    //		logger.atFine().log("semaphore.release()");
    semaphore.release()
  }

  fun remove(index: Int): Byte {
    //		logger.atFine().log(this + " remove("+index+")");
    val pos = convert(index)
    return try {
      array[pos]
    } finally {
      array[pos] = -1 // Let gc do its work

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

  private fun grow() {
    //		logger.atFine().log(this + " grow()");
    val oldCapacity = array.size
    val newCapacity = oldCapacity * 3 / 2 + 1
    logger.atFine().log("CircularBlockingByteQueue growing from $oldCapacity to $newCapacity")
    val newData = ByteArray(newCapacity)
    toArray(newData)
    tail = size
    head = 0
    array = newData
  }

  private fun toArray(a: ByteArray = ByteArray(size)): ByteArray {
    //		logger.atFine().log(this + " toArray("+Arrays.toString(a)+")");
    var a = a
    if (size == 0) return a
    if (a.size < size) a = Array.newInstance(a.javaClass.componentType, size) as ByteArray
    if (head < tail) {
      System.arraycopy(array, head, a, 0, tail - head)
    } else {
      System.arraycopy(array, head, a, 0, array.size - head)
      System.arraycopy(array, 0, a, array.size - head, tail)
    }
    if (a.size > size) {
      a[size] = 0
    }
    return a
  }

  // The convert() method takes a logical index (as if head was always 0) and
  // calculates the index within array
  private fun convert(index: Int): Int {
    return (index + head) % array.size
  }
}

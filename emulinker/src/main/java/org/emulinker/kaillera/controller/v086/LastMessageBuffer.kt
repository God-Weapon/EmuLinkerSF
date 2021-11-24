package org.emulinker.kaillera.controller.v086

import org.emulinker.kaillera.controller.v086.protocol.V086Message

/**
 * This is a specialized data structure designed to efficiently store and retrieve the last outbound
 * messages sent to a client. It would be much easier to use a simple LinkedList, but that means we
 * have to use iterators and create node objects, which causes large amounts of garbage collection
 * considering the rate at which messages flow through the server.<br></br> <br></br> This class
 * operates like a circular buffer, but adds messages from back to front. By doing this, we can use
 * System.aray copy to efficiently copy messages out, start with the newest first.
 */
class LastMessageBuffer(max: Int) {
  private val max: Int
  private var next: Int
  private var size = 0
  private val array: Array<V086Message?>
  fun add(o: V086Message?) {
    array[next] = o
    if (--next < 0) next = max - 1
    if (size < max) size++
  }

  fun fill(o: Array<V086Message?>?, num: Int): Int {
    // int startRead = (next+1);
    // int endRead = ((next+1)+size);
    if (size < num) {
      System.arraycopy(array, next + 1, o, 0, size)
      return size
    } else if (next + 1 + num <= max) {
      System.arraycopy(array, next + 1, o, 0, num)
    } else {
      // int firstPartSize = (max-(next+1));
      // int secondPartSize = (size-(max-(next+1)));
      System.arraycopy(array, next + 1, o, 0, max - (next + 1))
      System.arraycopy(array, 0, o, max - (next + 1), num - (max - (next + 1)))
    }
    return num
  }

  init {
    array = arrayOfNulls(max)
    this.max = max
    next = max - 1
  }
}

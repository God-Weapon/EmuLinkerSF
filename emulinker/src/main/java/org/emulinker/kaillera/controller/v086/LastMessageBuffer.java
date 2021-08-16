package org.emulinker.kaillera.controller.v086;

import org.emulinker.kaillera.controller.v086.protocol.V086Message;

/**
 * This is a specialized data structure designed to efficiently store and retrieve the last outbound
 * messages sent to a client. It would be much easier to use a simple LinkedList, but that means we
 * have to use iterators and create node objects, which causes large amounts of garbage collection
 * considering the rate at which messages flow through the server.<br>
 * <br>
 * This class operates like a circular buffer, but adds messages from back to front. By doing this,
 * we can use System.aray copy to efficiently copy messages out, start with the newest first.
 */
public class LastMessageBuffer {
  private int max;
  private int next;
  private int size;
  private V086Message[] array;

  public LastMessageBuffer(int max) {
    array = new V086Message[max];
    this.max = max;
    next = (max - 1);
  }

  public void add(V086Message o) {
    array[next] = o;
    if (--next < 0) next = (max - 1);
    if (size < max) size++;
  }

  public int fill(V086Message[] o, int num) {
    // int startRead = (next+1);
    // int endRead = ((next+1)+size);
    if (size < num) {
      System.arraycopy(array, (next + 1), o, 0, size);
      return size;
    } else if (((next + 1) + num) <= max) {
      System.arraycopy(array, (next + 1), o, 0, num);
    } else {
      // int firstPartSize = (max-(next+1));
      // int secondPartSize = (size-(max-(next+1)));
      System.arraycopy(array, (next + 1), o, 0, (max - (next + 1)));
      System.arraycopy(array, 0, o, (max - (next + 1)), (num - (max - (next + 1))));
    }
    return num;
  }
}

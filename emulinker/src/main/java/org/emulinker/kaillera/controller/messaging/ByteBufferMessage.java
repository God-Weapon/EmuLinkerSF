package org.emulinker.kaillera.controller.messaging;

import java.nio.ByteBuffer;
import org.apache.commons.logging.*;

public abstract class ByteBufferMessage {
  protected static Log log = LogFactory.getLog(ByteBufferMessage.class);

  private ByteBuffer buffer;

  public abstract int getLength();

  public abstract String getDescription();

  @Override
  public abstract String toString();

  protected void initBuffer() {
    initBuffer(getLength());
  }

  private void initBuffer(int size) {
    buffer = getBuffer(size);
  }

  public void releaseBuffer() {}

  public ByteBuffer toBuffer() {
    initBuffer();
    writeTo(buffer);
    buffer.flip();
    return buffer;
  }

  public abstract void writeTo(ByteBuffer buffer);

  public static ByteBuffer getBuffer(int size) {
    return ByteBuffer.allocateDirect(size);
  }

  public static void releaseBuffer(ByteBuffer buffer) {
    // nothing to do since we aren't caching buffers anymore
  }
}

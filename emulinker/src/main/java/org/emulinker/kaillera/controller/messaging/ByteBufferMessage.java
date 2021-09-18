package org.emulinker.kaillera.controller.messaging;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public abstract class ByteBufferMessage {
  private ByteBuffer buffer;

  public abstract int getLength();

  public abstract String description();

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
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    ((Buffer) buffer).flip();
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

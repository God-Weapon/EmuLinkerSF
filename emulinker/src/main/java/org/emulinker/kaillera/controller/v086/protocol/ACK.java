package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.util.UnsignedUtil;

public abstract class ACK extends V086Message {
  public abstract long val1();

  public abstract long val2();

  public abstract long val3();

  public abstract long val4();

  @Override
  public int getBodyLength() {
    return 17;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedInt(buffer, val1());
    UnsignedUtil.putUnsignedInt(buffer, val2());
    UnsignedUtil.putUnsignedInt(buffer, val3());
    UnsignedUtil.putUnsignedInt(buffer, val4());
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.UnsignedUtil;

@AutoValue
public abstract class CachedGameData extends V086Message {
  public static final byte ID = 0x13;

  private static final String DESC = "Cached Game Data";

  public abstract int key();

  public static AutoValue_CachedGameData create(int messageNumber, int key)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);
    return new AutoValue_CachedGameData(messageNumber, ID, DESC, key);
  }

  @Override
  public int getBodyLength() {
    return 2;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedByte(buffer, key());
  }

  public static CachedGameData parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();
    // removed to increase speed
    // if (b != 0x00)
    // throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " +
    // EmuUtil.byteToHex(b));

    return CachedGameData.create(messageNumber, UnsignedUtil.getUnsignedByte(buffer));
  }
}

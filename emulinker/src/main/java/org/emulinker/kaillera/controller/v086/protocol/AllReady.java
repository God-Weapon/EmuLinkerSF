package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.EmuUtil;

@AutoValue
public abstract class AllReady extends V086Message {
  public static final byte ID = 0x15;

  private static final String DESC = "All Ready Signal";

  public static AutoValue_AllReady create(int messageNumber) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);
    return new AutoValue_AllReady(messageNumber, ID, DESC);
  }

  @Override
  public int getBodyLength() {
    return 1;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
  }

  public static AllReady parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

    return AllReady.create(messageNumber);
  }
}

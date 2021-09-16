package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.UnsignedUtil;

@AutoValue
public abstract class KeepAlive extends V086Message {
  public static final byte ID = 0x09;
  private static final String DESC = "KeepAlive";

  public abstract short val();

  // TODO(nue): Is anybody using this?
  // public KeepAlive(int messageNumber) throws MessageFormatException {
  //   this(messageNumber, (short) 0x00);
  // }

  public static AutoValue_KeepAlive create(int messageNumber, short val)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (val < 0 || val > 0xFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: val out of acceptable range: " + val);
    }

    return new AutoValue_KeepAlive(messageNumber, ID, DESC, val);
  }

  @Override
  public int getBodyLength() {
    return 1;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    UnsignedUtil.putUnsignedByte(buffer, val());
  }

  public static KeepAlive parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

    return KeepAlive.create(messageNumber, UnsignedUtil.getUnsignedByte(buffer));
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.UnsignedUtil;

public class KeepAlive extends V086Message {
  public static final byte ID = 0x09;
  public static final String DESC = "KeepAlive";

  private short val;

  public KeepAlive(int messageNumber) throws MessageFormatException {
    this(messageNumber, (short) 0x00);
  }

  public KeepAlive(int messageNumber, short val) throws MessageFormatException {
    super(messageNumber);

    if (val < 0 || val > 0xFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: val out of acceptable range: " + val);

    this.val = val;
  }

  @Override
  public byte getID() {
    return ID;
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  public short getVal() {
    return val;
  }

  @Override
  public String toString() {
    return getInfoString() + "[val=" + val + "]";
  }

  @Override
  public int getBodyLength() {
    return 1;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    UnsignedUtil.putUnsignedByte(buffer, val);
  }

  public static KeepAlive parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

    return new KeepAlive(messageNumber, UnsignedUtil.getUnsignedByte(buffer));
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.EmuUtil;

public class AllReady extends V086Message {
  public static final byte ID = 0x15;
  public static final String DESC = "All Ready Signal";

  public AllReady(int messageNumber) throws MessageFormatException {
    super(messageNumber);
  }

  public byte getID() {
    return ID;
  }

  public String getDescription() {
    return DESC;
  }

  public String toString() {
    return getInfoString();
  }

  public int getBodyLength() {
    return 1;
  }

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

    return new AllReady(messageNumber);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.UnsignedUtil;

public abstract class StartGame extends V086Message {
  public static final byte ID = 0x11;

  public abstract int val1();

  public abstract short playerNumber();

  public abstract short numPlayers();

  @Override
  public int getBodyLength() {
    return 5;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, val1());
    UnsignedUtil.putUnsignedByte(buffer, playerNumber());
    UnsignedUtil.putUnsignedByte(buffer, numPlayers());
  }

  public static StartGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00) throw new ParseException("Failed byte count validation!");

    int val1 = UnsignedUtil.getUnsignedShort(buffer);
    short playerNumber = UnsignedUtil.getUnsignedByte(buffer);
    short numPlayers = UnsignedUtil.getUnsignedByte(buffer);

    if (val1 == 0xFFFF && playerNumber == 0xFF && numPlayers == 0xFF)
      return StartGame_Request.create(messageNumber);
    else return StartGame_Notification.create(messageNumber, val1, playerNumber, numPlayers);
  }
}

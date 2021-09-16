package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

@AutoValue
public abstract class CloseGame extends V086Message {
  public static final byte ID = 0x10;

  private static final String DESC = "Close Game";

  public abstract int gameId();

  public abstract int val1();

  public static AutoValue_CloseGame create(int messageNumber, int gameId, int val1)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (gameId < 0 || gameId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId);
    }

    if (val1 < 0 || val1 > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: val1 out of acceptable range: " + val1);
    }

    return new AutoValue_CloseGame(messageNumber, ID, DESC, gameId, val1);
  }

  @Override
  public int getBodyLength() {
    return 5;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameId());
    UnsignedUtil.putUnsignedShort(buffer, val1());
  }

  public static CloseGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

    int gameID = UnsignedUtil.getUnsignedShort(buffer);
    int val1 = UnsignedUtil.getUnsignedShort(buffer);

    return CloseGame.create(messageNumber, gameID, val1);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

@AutoValue
public abstract class GameKick extends V086Message {
  public static final byte ID = 0x0F;
  private static final String DESC = "Game Kick Request";

  public abstract int userId();

  public static AutoValue_GameKick create(int messageNumber, int userId)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (userId < 0 || userId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId);
    }

    return new AutoValue_GameKick(messageNumber, ID, DESC, userId);
  }

  @Override
  public int getBodyLength() {
    return 3;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, userId());
  }

  public static GameKick parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();
    /*SF MOD
    if (b != 0x00)
    	throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));
    */
    return GameKick.create(messageNumber, UnsignedUtil.getUnsignedShort(buffer));
  }
}

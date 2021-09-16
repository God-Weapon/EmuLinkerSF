package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

@AutoValue
public abstract class ClientACK extends ACK {
  public static final byte ID = 0x06;
  private static final String DESC = "Client to Server ACK";

  public static ClientACK create(int messageNumber) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);
    return new AutoValue_ClientACK(messageNumber, ID, DESC, 0, 1, 2, 3);
  }

  public static ClientACK parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 17) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));
    }

    // long val1 = UnsignedUtil.getUnsignedInt(buffer);
    // long val2 = UnsignedUtil.getUnsignedInt(buffer);
    // long val3 = UnsignedUtil.getUnsignedInt(buffer);
    // long val4 = UnsignedUtil.getUnsignedInt(buffer);

    // if (val1 != 0 || val2 != 1 || val3 != 2 || val4 != 3)
    // throw new MessageFormatException("Invalid " + DESC + " format: bytes do not match acceptable
    // format!");

    return ClientACK.create(messageNumber);
  }
}

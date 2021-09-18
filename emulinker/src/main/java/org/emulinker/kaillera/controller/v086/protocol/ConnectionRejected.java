package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.pico.AppModule;
import org.emulinker.util.*;

@AutoValue
public abstract class ConnectionRejected extends V086Message {
  public static final byte ID = 0x16;
  private static final String DESC = "Connection Rejected";

  public abstract String username();

  public abstract int userId();

  public abstract String message();

  public static AutoValue_ConnectionRejected create(
      int messageNumber, String username, int userId, String message)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (Strings.isNullOrEmpty(username)) {
      throw new MessageFormatException("Invalid " + DESC + " format: userName.length == 0");
    }

    if (userId < 0 || userId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId);
    }

    if (Strings.isNullOrEmpty(message)) {
      throw new MessageFormatException("Invalid " + DESC + " format: message.length == 0");
    }

    return new AutoValue_ConnectionRejected(messageNumber, ID, DESC, username, userId, message);
  }

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + getNumBytes(message()) + 4;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, AppModule.charsetDoNotUse);
    UnsignedUtil.putUnsignedShort(buffer, userId());
    EmuUtil.writeString(buffer, message(), 0x00, AppModule.charsetDoNotUse);
  }

  public static ConnectionRejected parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 6) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (buffer.remaining() < 4) throw new ParseException("Failed byte count validation!");

    int userID = UnsignedUtil.getUnsignedShort(buffer);

    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    String message = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    return ConnectionRejected.create(messageNumber, userName, userID, message);
  }
}

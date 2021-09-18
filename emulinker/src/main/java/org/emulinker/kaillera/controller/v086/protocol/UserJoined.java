package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.pico.AppModule;
import org.emulinker.util.*;

@AutoValue
public abstract class UserJoined extends V086Message {
  public static final byte ID = 0x02;
  private static final String DESC = "User Joined";

  public abstract String username();

  public abstract int userId();

  public abstract long ping();

  public abstract byte connectionType();

  public static AutoValue_UserJoined create(
      int messageNumber, String username, int userId, long ping, byte connectionType)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (Strings.isNullOrEmpty(username))
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userName.length == 0, (userID = " + userId + ")");

    if (userId < 0 || userId > 65535)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId);

    if (ping < 0 || ping > 2048) // what should max ping be?
    throw new MessageFormatException(
          "Invalid " + DESC + " format: ping out of acceptable range: " + ping);

    if (connectionType < 1 || connectionType > 6)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: connectionType out of acceptable range: " + connectionType);

    return new AutoValue_UserJoined(
        messageNumber, ID, DESC, username, userId, ping, connectionType);
  }

  // TODO(nue): Get rid of this.
  @Override
  public String toString() {
    return getInfoString()
        + "[userName="
        + username()
        + " userID="
        + userId()
        + " ping="
        + ping()
        + " connectionType="
        + KailleraUser.CONNECTION_TYPE_NAMES[connectionType()]
        + "]";
  }

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + 8;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, AppModule.charsetDoNotUse);
    UnsignedUtil.putUnsignedShort(buffer, userId());
    UnsignedUtil.putUnsignedInt(buffer, ping());
    buffer.put(connectionType());
  }

  public static UserJoined parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 9) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (buffer.remaining() < 7) throw new ParseException("Failed byte count validation!");

    int userID = UnsignedUtil.getUnsignedShort(buffer);
    long ping = UnsignedUtil.getUnsignedInt(buffer);
    byte connectionType = buffer.get();

    return UserJoined.create(messageNumber, userName, userID, ping, connectionType);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.EmuUtil;

@AutoValue
public abstract class UserInformation extends V086Message {
  public static final byte ID = 0x03;
  private static final String DESC = "User Information";

  public abstract String username();

  public abstract String clientType();

  public abstract byte connectionType();

  public static AutoValue_UserInformation create(
      int messageNumber, String username, String clientType, byte connectionType)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (connectionType < 1 || connectionType > 6) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: connectionType out of acceptable range: " + connectionType);
    }

    return new AutoValue_UserInformation(
        messageNumber, ID, DESC, username, clientType, connectionType);
  }

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + getNumBytes(clientType()) + 3;
  }

  // TODO(nue): Get rid of this.
  @Override
  public final String toString() {
    return getInfoString()
        + "[userName="
        + username()
        + " clientType="
        + clientType()
        + " connectionType="
        + KailleraUser.CONNECTION_TYPE_NAMES[connectionType()]
        + "]";
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, KailleraRelay.config.charset());
    EmuUtil.writeString(buffer, clientType(), 0x00, KailleraRelay.config.charset());
    buffer.put(connectionType());
  }

  public static UserInformation parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    String clientType = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

    byte connectionType = buffer.get();

    return UserInformation.create(messageNumber, userName, clientType, connectionType);
  }
}

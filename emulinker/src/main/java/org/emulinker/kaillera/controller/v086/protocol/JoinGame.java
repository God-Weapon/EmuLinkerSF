package org.emulinker.kaillera.controller.v086.protocol;

import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.*;

public abstract class JoinGame extends V086Message {
  public static final byte ID = 0x0C;

  public abstract int gameId();

  public abstract int val1();

  public abstract String username();

  public abstract long ping();

  public abstract int userId();

  public abstract byte connectionType();

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + 13;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameId());
    UnsignedUtil.putUnsignedShort(buffer, val1());
    EmuUtil.writeString(buffer, username(), 0x00, KailleraRelay.config.charset());
    UnsignedUtil.putUnsignedInt(buffer, ping());
    UnsignedUtil.putUnsignedShort(buffer, userId());
    buffer.put(connectionType());
  }

  public static JoinGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 13) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException("Invalid format: byte 0 = " + EmuUtil.byteToHex(b));

    int gameID = UnsignedUtil.getUnsignedShort(buffer);
    int val1 = UnsignedUtil.getUnsignedShort(buffer);
    String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 7) throw new ParseException("Failed byte count validation!");

    long ping = UnsignedUtil.getUnsignedInt(buffer);
    int userID = UnsignedUtil.getUnsignedShort(buffer);
    byte connectionType = buffer.get();

    if (Strings.isNullOrEmpty(userName) && ping == 0 && userID == 0xFFFF)
      return JoinGame_Request.create(messageNumber, gameID, connectionType);
    else
      return JoinGame_Notification.create(
          messageNumber, gameID, val1, userName, ping, userID, connectionType);
  }
}

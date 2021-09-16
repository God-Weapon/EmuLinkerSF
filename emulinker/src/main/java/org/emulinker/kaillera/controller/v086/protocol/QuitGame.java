package org.emulinker.kaillera.controller.v086.protocol;

import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.*;

public abstract class QuitGame extends V086Message {
  public static final byte ID = 0x0B;

  public abstract String username();

  public abstract int userId();

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + 3;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, KailleraRelay.config.charset());
    UnsignedUtil.putUnsignedShort(buffer, userId());
  }

  public static QuitGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    int userID = UnsignedUtil.getUnsignedShort(buffer);

    if (Strings.isNullOrEmpty(userName) && userID == 0xFFFF) {
      return QuitGame_Request.create(messageNumber);
    }
    return QuitGame_Notification.create(messageNumber, userName, userID);
  }
}

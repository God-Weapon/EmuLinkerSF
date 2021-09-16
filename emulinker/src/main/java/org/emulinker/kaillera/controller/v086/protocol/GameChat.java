package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.EmuUtil;

public abstract class GameChat extends V086Message {
  public static final byte ID = 0x08;

  public abstract String username();

  public abstract String message();

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + getNumBytes(message()) + 2;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, KailleraRelay.config.charset());
    EmuUtil.writeString(buffer, message(), 0x00, KailleraRelay.config.charset());
  }

  public static GameChat parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    String message = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (userName.length() == 0) {
      return GameChat_Request.create(messageNumber, message);
    }
    return GameChat_Notification.create(messageNumber, userName, message);
  }
}

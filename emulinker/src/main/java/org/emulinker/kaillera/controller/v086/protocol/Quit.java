package org.emulinker.kaillera.controller.v086.protocol;

import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.pico.AppModule;
import org.emulinker.util.*;

public abstract class Quit extends V086Message {
  public static final byte ID = 0x01;

  public abstract String username();

  public abstract int userId();

  public abstract String message();

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

  public static Quit parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    int userID = UnsignedUtil.getUnsignedShort(buffer);

    String message = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (Strings.isNullOrEmpty(userName) && userID == 0xFFFF) {
      return Quit_Request.create(messageNumber, message);
    }
    return Quit_Notification.create(messageNumber, userName, userID, message);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.pico.AppModule;
import org.emulinker.util.*;

public abstract class CreateGame extends V086Message {
  public static final byte ID = 0x0A;

  public abstract String username();

  public abstract String romName();

  public abstract String clientType();

  public abstract int gameId();

  public abstract int val1();

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + getNumBytes(romName()) + getNumBytes(clientType()) + 7;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, AppModule.charsetDoNotUse);
    EmuUtil.writeString(buffer, romName(), 0x00, AppModule.charsetDoNotUse);
    EmuUtil.writeString(buffer, clientType(), 0x00, AppModule.charsetDoNotUse);
    UnsignedUtil.putUnsignedShort(buffer, gameId());
    UnsignedUtil.putUnsignedShort(buffer, val1());
  }

  public static CreateGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 8) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (buffer.remaining() < 6) throw new ParseException("Failed byte count validation!");

    String romName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    String clientType = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

    if (buffer.remaining() < 4) throw new ParseException("Failed byte count validation!");

    int gameID = UnsignedUtil.getUnsignedShort(buffer);
    int val1 = UnsignedUtil.getUnsignedShort(buffer);

    if (Strings.isNullOrEmpty(userName) && gameID == 0xFFFF && val1 == 0xFFFF)
      return CreateGame_Request.create(messageNumber, romName);
    else
      return CreateGame_Notification.create(
          messageNumber, userName, romName, clientType, gameID, val1);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.pico.AppModule;
import org.emulinker.util.EmuUtil;

public abstract class PlayerDrop extends V086Message {
  public static final byte ID = 0x14;

  public abstract String username();

  public abstract byte playerNumber();

  // public PlayerDrop(int messageNumber, String userName, byte playerNumber)
  //     throws MessageFormatException {
  //   super(messageNumber);

  //   if (playerNumber < 0 || playerNumber > 255)
  //     throw new MessageFormatException(
  //         "Invalid "
  //             + getDescription()
  //             + " format: playerNumber out of acceptable range: "
  //             + playerNumber);

  //   this.userName = userName;
  //   this.playerNumber = playerNumber;
  // }

  @Override
  public int getBodyLength() {
    return getNumBytes(username()) + 2;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, username(), 0x00, AppModule.charsetDoNotUse);
    buffer.put(playerNumber());
  }

  public static PlayerDrop parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);
    byte playerNumber = buffer.get();

    if (Strings.isNullOrEmpty(userName) && playerNumber == 0) {
      return PlayerDrop_Request.create(messageNumber);
    }
    return PlayerDrop_Notification.create(messageNumber, userName, playerNumber);
  }
}

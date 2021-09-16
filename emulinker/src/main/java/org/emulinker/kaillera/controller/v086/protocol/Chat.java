package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.EmuUtil;

public abstract class Chat extends V086Message {
  public static final byte ID = 0x07;

  private String userName;
  private String message;

  public Chat(int messageNumber, String userName, String message) throws MessageFormatException {
    super(messageNumber);

    this.userName = userName;
    this.message = message;
  }

  @Override
  public byte getID() {
    return ID;
  }

  @Override
  public abstract String getDescription();

  public String getUserName() {
    return userName;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public abstract String toString();

  @Override
  public int getBodyLength() {
    return getNumBytes(userName) + getNumBytes(message) + 2;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, userName, 0x00, KailleraRelay.config.charset());
    EmuUtil.writeString(buffer, message, 0x00, KailleraRelay.config.charset());
  }

  public static Chat parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    String message = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (userName.length() == 0) {
      return new Chat_Request(messageNumber, message);
    } else {
      return new Chat_Notification(messageNumber, userName, message);
    }
  }
}

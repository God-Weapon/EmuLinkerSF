package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.*;

public class ConnectionRejected extends V086Message {
  public static final byte ID = 0x16;
  public static final String DESC = "Connection Rejected";

  private String userName;
  private int userID;
  private String message;

  public ConnectionRejected(int messageNumber, String userName, int userID, String message)
      throws MessageFormatException {
    super(messageNumber);

    if (userName.length() == 0)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: userName.length == 0");

    if (userID < 0 || userID > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: userID out of acceptable range: " + userID);

    if (message.length() == 0)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: message.length == 0");

    this.userName = userName;
    this.userID = userID;
    this.message = message;
  }

  @Override
  public byte getID() {
    return ID;
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  public String getUserName() {
    return userName;
  }

  public int getUserID() {
    return userID;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return getInfoString()
        + "[userName="
        + userName
        + " userID="
        + userID
        + " message="
        + message
        + "]";
  }

  @Override
  public int getBodyLength() {
    return getNumBytes(userName) + getNumBytes(message) + 4;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, userName, 0x00, KailleraRelay.config.charset());
    UnsignedUtil.putUnsignedShort(buffer, userID);
    EmuUtil.writeString(buffer, message, 0x00, KailleraRelay.config.charset());
  }

  public static ConnectionRejected parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 6) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 4) throw new ParseException("Failed byte count validation!");

    int userID = UnsignedUtil.getUnsignedShort(buffer);

    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    String message = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    return new ConnectionRejected(messageNumber, userName, userID, message);
  }
}

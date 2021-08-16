package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public abstract class JoinGame extends V086Message {
  public static final byte ID = 0x0C;
  private int gameID;

  private int val1;
  private String userName;
  private long ping;
  private int userID;
  private byte connectionType;

  public JoinGame(
      int messageNumber,
      int gameID,
      int val1,
      String userName,
      long ping,
      int userID,
      byte connectionType)
      throws MessageFormatException {
    super(messageNumber);

    if (gameID < 0 || gameID > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: gameID out of acceptable range: " + gameID);

    if (ping < 0 || ping > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: ping out of acceptable range: " + ping);

    if (userID < 0 || userID > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: userID out of acceptable range: " + userID);

    if (connectionType < 1 || connectionType > 6)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: connectionType out of acceptable range: "
              + connectionType);

    this.gameID = gameID;
    this.val1 = val1;
    this.userName = userName; // check userName length?
    this.ping = ping;
    this.userID = userID;
    this.connectionType = connectionType;
  }

  public byte getID() {
    return ID;
  }

  public abstract String getDescription();

  public int getGameID() {
    return gameID;
  }

  public int getVal1() {
    return val1;
  }

  public String getUserName() {
    return userName;
  }

  public long getPing() {
    return ping;
  }

  public int getUserID() {
    return userID;
  }

  public byte getConnectionType() {
    return connectionType;
  }

  public abstract String toString();

  public int getBodyLength() {
    return getNumBytes(userName) + 13;
  }

  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameID);
    UnsignedUtil.putUnsignedShort(buffer, val1);
    EmuUtil.writeString(buffer, userName, 0x00, charset);
    UnsignedUtil.putUnsignedInt(buffer, ping);
    UnsignedUtil.putUnsignedShort(buffer, userID);
    buffer.put(connectionType);
  }

  public static JoinGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 13) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException("Invalid format: byte 0 = " + EmuUtil.byteToHex(b));

    int gameID = UnsignedUtil.getUnsignedShort(buffer);
    int val1 = UnsignedUtil.getUnsignedShort(buffer);
    String userName = EmuUtil.readString(buffer, 0x00, charset);

    if (buffer.remaining() < 7) throw new ParseException("Failed byte count validation!");

    long ping = UnsignedUtil.getUnsignedInt(buffer);
    int userID = UnsignedUtil.getUnsignedShort(buffer);
    byte connectionType = buffer.get();

    if (userName.length() == 0 && ping == 0 && userID == 0xFFFF)
      return new JoinGame_Request(messageNumber, gameID, connectionType);
    else
      return new JoinGame_Notification(
          messageNumber, gameID, val1, userName, ping, userID, connectionType);
  }
}

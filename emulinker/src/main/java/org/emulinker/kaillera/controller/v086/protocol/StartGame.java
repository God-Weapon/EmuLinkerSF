package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.UnsignedUtil;

public abstract class StartGame extends V086Message {
  public static final byte ID = 0x11;

  private int val1;
  private short playerNumber;
  private short numPlayers;

  public StartGame(int messageNumber, short playerNumber, short numPlayers)
      throws MessageFormatException {
    this(messageNumber, 1, playerNumber, numPlayers);
  }

  public StartGame(int messageNumber, int val1, short playerNumber, short numPlayers)
      throws MessageFormatException {
    super(messageNumber);

    if (val1 < 0 || val1 > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: val1 out of acceptable range: " + val1);

    if (playerNumber < 0 || playerNumber > 0xFF)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: playerNumber out of acceptable range: "
              + playerNumber);

    if (numPlayers < 0 || numPlayers > 0xFF)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: numPlayers out of acceptable range: "
              + numPlayers);

    this.val1 = val1;
    this.playerNumber = playerNumber;
    this.numPlayers = numPlayers;
  }

  public byte getID() {
    return ID;
  }

  public abstract String getDescription();

  public int getVal1() {
    return val1;
  }

  public short getPlayerNumber() {
    return playerNumber;
  }

  public short getNumPlayers() {
    return numPlayers;
  }

  public abstract String toString();

  public int getBodyLength() {
    return 5;
  }

  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, val1);
    UnsignedUtil.putUnsignedByte(buffer, playerNumber);
    UnsignedUtil.putUnsignedByte(buffer, numPlayers);
  }

  public static StartGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00) throw new ParseException("Failed byte count validation!");

    int val1 = UnsignedUtil.getUnsignedShort(buffer);
    short playerNumber = UnsignedUtil.getUnsignedByte(buffer);
    short numPlayers = UnsignedUtil.getUnsignedByte(buffer);

    if (val1 == 0xFFFF && playerNumber == 0xFF && numPlayers == 0xFF)
      return new StartGame_Request(messageNumber);
    else return new StartGame_Notification(messageNumber, val1, playerNumber, numPlayers);
  }
}

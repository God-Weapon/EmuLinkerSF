package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class GameStatus extends V086Message {
  public static final byte ID = 0x0E;
  public static final String DESC = "Game Status";

  private int gameID;
  private int val1;
  private byte gameStatus;
  private byte numPlayers;
  private byte maxPlayers;

  public GameStatus(
      int messageNumber, int gameID, int val1, byte gameStatus, byte numPlayers, byte maxPlayers)
      throws MessageFormatException {
    super(messageNumber);

    if (gameID < 0 || gameID > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: gameID out of acceptable range: " + gameID);

    if (val1 < 0 || val1 > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: val1 out of acceptable range: " + val1);

    if (gameStatus < 0 || gameStatus > 2)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: gameStatus out of acceptable range: "
              + gameStatus);

    if (numPlayers < 0 || numPlayers > 0xFF)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: numPlayers out of acceptable range: "
              + numPlayers);

    if (maxPlayers < 0 || maxPlayers > 0xFF)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: maxPlayers out of acceptable range: "
              + maxPlayers);

    this.gameID = gameID;
    this.val1 = val1;
    this.gameStatus = gameStatus;
    this.numPlayers = numPlayers;
    this.maxPlayers = maxPlayers;
  }

  public byte getID() {
    return ID;
  }

  public String getDescription() {
    return DESC;
  }

  public int getGameID() {
    return gameID;
  }

  public int getVal1() {
    return val1;
  }

  public byte getGameStatus() {
    return gameStatus;
  }

  public byte getNumPlayers() {
    return numPlayers;
  }

  public byte getMaxPlayers() {
    return maxPlayers;
  }

  public String toString() {
    return getInfoString()
        + "[gameID="
        + gameID
        + " gameStatus="
        + org.emulinker.kaillera.model.KailleraGame.STATUS_NAMES[gameStatus]
        + " numPlayers="
        + numPlayers
        + " maxPlayers="
        + maxPlayers
        + "]";
  }

  public int getBodyLength() {
    return 8;
  }

  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameID);
    UnsignedUtil.putUnsignedShort(buffer, val1);
    buffer.put(gameStatus);
    buffer.put(numPlayers);
    buffer.put(maxPlayers);
  }

  public static GameStatus parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 8) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

    int gameID = UnsignedUtil.getUnsignedShort(buffer);
    int val1 = UnsignedUtil.getUnsignedShort(buffer);
    byte gameStatus = buffer.get();
    byte numPlayers = buffer.get();
    byte maxPlayers = buffer.get();

    return new GameStatus(messageNumber, gameID, val1, gameStatus, numPlayers, maxPlayers);
  }
}

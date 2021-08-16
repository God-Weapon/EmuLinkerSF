package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class CloseGame extends V086Message {
  public static final byte ID = 0x10;
  public static final String DESC = "Close Game";

  private int gameID;
  private int val1;

  public CloseGame(int messageNumber, int gameID, int val1) throws MessageFormatException {
    super(messageNumber);

    if (gameID < 0 || gameID > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: gameID out of acceptable range: " + gameID);

    if (val1 < 0 || val1 > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: val1 out of acceptable range: " + val1);

    this.gameID = gameID;
    this.val1 = val1;
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

  public String toString() {
    return getInfoString() + "[gameID=" + gameID + " val1=" + val1 + "]";
  }

  public int getBodyLength() {
    return 5;
  }

  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameID);
    UnsignedUtil.putUnsignedShort(buffer, val1);
  }

  public static CloseGame parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

    int gameID = UnsignedUtil.getUnsignedShort(buffer);
    int val1 = UnsignedUtil.getUnsignedShort(buffer);

    return new CloseGame(messageNumber, gameID, val1);
  }
}

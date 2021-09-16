package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.util.*;

@AutoValue
public abstract class GameStatus extends V086Message {
  public static final byte ID = 0x0E;
  private static final String DESC = "Game Status";

  public abstract int gameId();

  public abstract int val1();

  public abstract byte gameStatus();

  public abstract byte numPlayers();

  public abstract byte maxPlayers();

  public static AutoValue_GameStatus create(
      int messageNumber, int gameId, int val1, byte gameStatus, byte numPlayers, byte maxPlayers)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (gameId < 0 || gameId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId);
    }

    if (val1 < 0 || val1 > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: val1 out of acceptable range: " + val1);
    }

    if (gameStatus < 0 || gameStatus > 2) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameStatus out of acceptable range: " + gameStatus);
    }

    if (numPlayers < 0 || numPlayers > 0xFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: numPlayers out of acceptable range: " + numPlayers);
    }

    if (maxPlayers < 0 || maxPlayers > 0xFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: maxPlayers out of acceptable range: " + maxPlayers);
    }

    return new AutoValue_GameStatus(
        messageNumber, ID, DESC, gameId, val1, gameStatus, numPlayers, maxPlayers);
  }

  // TODO(nue): See if we can remove this.
  @Override
  public String toString() {
    return getInfoString()
        + String.format(
            "[gameID=%d gameStatus=%s numPlayers=%d maxPlayers=%d]",
            gameId(), KailleraGame.STATUS_NAMES[gameStatus()], numPlayers(), maxPlayers());
  }

  @Override
  public int getBodyLength() {
    return 8;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameId());
    UnsignedUtil.putUnsignedShort(buffer, val1());
    buffer.put(gameStatus());
    buffer.put(numPlayers());
    buffer.put(maxPlayers());
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

    return GameStatus.create(messageNumber, gameID, val1, gameStatus, numPlayers, maxPlayers);
  }
}

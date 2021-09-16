package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

@AutoValue
public abstract class GameData extends V086Message {
  public static final byte ID = 0x12;
  public static final String DESC = "Game Data";

  public abstract byte[] gameData();

  public static void main(String args[]) throws Exception {
    byte[] data = new byte[9];
    long st = System.currentTimeMillis();
    GameData msg = GameData.create(0, data);
    ByteBuffer byteByffer = ByteBuffer.allocateDirect(4096);
    for (int i = 0; i < 0xFFFF; i++) {
      msg.writeTo(byteByffer);
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      ((Buffer) byteByffer).clear();
    }
    System.out.println("et=" + (System.currentTimeMillis() - st));
  }

  public static AutoValue_GameData create(int messageNumber, byte[] gameData)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (gameData.length <= 0 || gameData.length > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameData.remaining() = " + gameData.length);
    }

    return new AutoValue_GameData(
        messageNumber, ID, DESC, Arrays.copyOf(gameData, gameData.length));
  }

  @Override
  public int getBodyLength() {
    return gameData().length + 3;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    UnsignedUtil.putUnsignedShort(buffer, gameData().length);
    buffer.put(gameData());
  }

  public static GameData parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 4) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();
    // removed to increase speed
    //		if (b != 0x00)
    //			throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " +
    // EmuUtil.byteToHex(b));

    int dataSize = UnsignedUtil.getUnsignedShort(buffer);
    if (dataSize <= 0 || dataSize > buffer.remaining())
      throw new MessageFormatException("Invalid " + DESC + " format: dataSize = " + dataSize);

    byte[] gameData = new byte[dataSize];
    buffer.get(gameData);

    return GameData.create(messageNumber, gameData);
  }
}

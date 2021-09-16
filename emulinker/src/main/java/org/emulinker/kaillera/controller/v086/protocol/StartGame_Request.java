package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class StartGame_Request extends StartGame {
  private static final String DESC = "Start Game Request";
  private static final int VAL1 = 0xFFFF;
  private static final short PLAYER_NUMBER = (short) 0xFF;
  private static final short NUM_PLAYERS = (short) 0xFF;

  public static AutoValue_StartGame_Request create(int messageNumber)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_StartGame_Request(
        messageNumber, ID, DESC, VAL1, PLAYER_NUMBER, NUM_PLAYERS);
  }
}

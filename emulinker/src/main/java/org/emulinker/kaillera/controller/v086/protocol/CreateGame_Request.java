package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class CreateGame_Request extends CreateGame {
  private static final String DESC = "Create Game Request";

  private static final int GAME_ID = 0xFFFF;
  private static final int VAL1 = 0xFFFF;
  private static final String USERNAME = "";
  private static final String CLIENT_TYPE = "";

  public static AutoValue_CreateGame_Request create(int messageNumber, String romName)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_CreateGame_Request(
        messageNumber, ID, DESC, USERNAME, romName, CLIENT_TYPE, GAME_ID, VAL1);
  }
}

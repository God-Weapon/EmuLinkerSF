package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class CreateGame_Notification extends CreateGame {
  private static final String DESC = "Create Game Notification";

  public static AutoValue_CreateGame_Notification create(
      int messageNumber, String username, String romName, String clientType, int gameId, int val1)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (romName.length() == 0) {
      throw new MessageFormatException("Invalid " + DESC + " format: romName.length == 0");
    }

    if (gameId < 0 || gameId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId);
    }

    if (val1 != 0x0000 && val1 != 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: val1 out of acceptable range: " + val1);
    }

    return new AutoValue_CreateGame_Notification(
        messageNumber, ID, DESC, username, romName, clientType, gameId, val1);
  }
}

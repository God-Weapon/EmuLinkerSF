package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class QuitGame_Notification extends QuitGame {
  private static final String DESC = "Quit Game Notification";

  public static AutoValue_QuitGame_Notification create(
      int messageNumber, String username, int userId) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (userId < 0 || userId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId);
    }
    return new AutoValue_QuitGame_Notification(messageNumber, ID, DESC, username, userId);
  }
}

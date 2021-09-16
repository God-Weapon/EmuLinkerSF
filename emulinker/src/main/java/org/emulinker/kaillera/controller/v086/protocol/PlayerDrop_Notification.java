package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class PlayerDrop_Notification extends PlayerDrop {
  private static final String DESC = "Player Drop Notification";

  public static AutoValue_PlayerDrop_Notification create(
      int messageNumber, String username, byte playerNumber) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (playerNumber < 0 || playerNumber > 255) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: playerNumber out of acceptable range: " + playerNumber);
    }

    if (username.length() == 0) {
      throw new MessageFormatException("Invalid " + DESC + " format: userName.length == 0");
    }
    return new AutoValue_PlayerDrop_Notification(messageNumber, ID, DESC, username, playerNumber);
  }
}

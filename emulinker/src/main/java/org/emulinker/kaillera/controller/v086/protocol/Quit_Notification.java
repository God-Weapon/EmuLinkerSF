package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class Quit_Notification extends Quit {
  private static final String DESC = "User Quit Notification";

  public static AutoValue_Quit_Notification create(
      int messageNumber, String username, int userId, String message)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (userId < 0 || userId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId);
    }

    if (message == null)
      throw new MessageFormatException("Invalid " + DESC + " format: message == null!");

    if (Strings.isNullOrEmpty(username)) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userName.length == 0, (userID = " + userId + ")");
    }
    return new AutoValue_Quit_Notification(messageNumber, ID, DESC, username, userId, message);
  }
}

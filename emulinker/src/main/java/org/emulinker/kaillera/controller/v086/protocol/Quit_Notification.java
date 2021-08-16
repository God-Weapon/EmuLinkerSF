package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class Quit_Notification extends Quit {
  public static final String DESC = "User Quit Notification";

  public Quit_Notification(int messageNumber, String userName, int userID, String message)
      throws MessageFormatException {
    super(messageNumber, userName, userID, message);

    if (userName.length() == 0)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: userName.length == 0, (userID = "
              + userID
              + ")");
  }

  public String getDescription() {
    return DESC;
  }

  public String toString() {
    return getInfoString()
        + "[userName="
        + getUserName()
        + " userID="
        + getUserID()
        + " message="
        + getMessage()
        + "]";
  }
}

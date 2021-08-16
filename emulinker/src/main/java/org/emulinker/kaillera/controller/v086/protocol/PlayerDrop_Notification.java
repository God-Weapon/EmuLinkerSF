package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class PlayerDrop_Notification extends PlayerDrop {
  public static final String DESC = "Player Drop Notification";

  public PlayerDrop_Notification(int messageNumber, String userName, byte playerNumber)
      throws MessageFormatException {
    super(messageNumber, userName, playerNumber);

    if (userName.length() == 0)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: userName.length == 0");
  }

  public String getDescription() {
    return DESC;
  }

  public String toString() {
    return getInfoString()
        + "[userName="
        + getUserName()
        + " playerNumber="
        + getPlayerNumber()
        + "]";
  }
}

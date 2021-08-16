package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class QuitGame_Notification extends QuitGame {
  public static final String DESC = "Quit Game Notification";

  public QuitGame_Notification(int messageNumber, String userName, int userID)
      throws MessageFormatException {
    super(messageNumber, userName, userID);
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  @Override
  public String toString() {
    return getInfoString() + "[userName=" + getUserName() + " userID=" + getUserID() + "]";
  }
}

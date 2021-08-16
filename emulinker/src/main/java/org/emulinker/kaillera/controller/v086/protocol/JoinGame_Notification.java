package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class JoinGame_Notification extends JoinGame {
  public static final String DESC = "Join Game Notification";

  public JoinGame_Notification(
      int messageNumber,
      int gameID,
      int val1,
      String userName,
      long ping,
      int userID,
      byte connectionType)
      throws MessageFormatException {
    super(messageNumber, gameID, val1, userName, ping, userID, connectionType);

    if (userName.length() == 0)
      throw new MessageFormatException(
          "Invalid " + getDescription() + " format: userName.length() == 0");
  }

  @Override
  public byte getID() {
    return ID;
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  @Override
  public String toString() {
    return getInfoString()
        + "[gameID="
        + getGameID()
        + " val1="
        + getVal1()
        + " userName="
        + getUserName()
        + " ping="
        + getPing()
        + " userID="
        + getUserID()
        + " connectionType="
        + getConnectionType()
        + "]";
  }
}

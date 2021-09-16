package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class JoinGame_Notification extends JoinGame {
  private static final String DESC = "Join Game Notification";

  public static AutoValue_JoinGame_Notification create(
      int messageNumber,
      int gameId,
      int val1,
      String username,
      long ping,
      int userId,
      byte connectionType)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (gameId < 0 || gameId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId);
    }

    if (ping < 0 || ping > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: ping out of acceptable range: " + ping);
    }

    if (userId < 0 || userId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: userID out of acceptable range: " + userId);
    }

    if (connectionType < 1 || connectionType > 6) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: connectionType out of acceptable range: " + connectionType);
    }

    if (Strings.isNullOrEmpty(username)) {
      throw new MessageFormatException("Invalid " + DESC + " format: userName.length() == 0");
    }
    return new AutoValue_JoinGame_Notification(
        messageNumber, JoinGame.ID, gameId, val1, username, ping, userId, connectionType);
  }

  @Override
  public String description() {
    return DESC;
  }
}

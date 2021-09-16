package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class JoinGame_Request extends JoinGame {
  private static final String DESC = "Join Game Request";

  private static final int VAL1 = 0;
  private static final String USERNAME = "";
  private static final int PING = 0;
  private static final int USER_ID = 0xFFFF;

  public static AutoValue_JoinGame_Request create(
      int messageNumber, int gameId, byte connectionType) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (gameId < 0 || gameId > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId);
    }

    if (connectionType < 1 || connectionType > 6) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: connectionType out of acceptable range: " + connectionType);
    }

    return new AutoValue_JoinGame_Request(
        messageNumber, JoinGame.ID, DESC, gameId, VAL1, USERNAME, PING, USER_ID, connectionType);
  }
}

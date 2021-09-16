package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class StartGame_Notification extends StartGame {
  private static final String DESC = "Start Game Notification";

  public static AutoValue_StartGame_Notification create(
      int messageNumber, int val1, short playerNumber, short numPlayers)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (val1 < 0 || val1 > 0xFFFF)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: val1 out of acceptable range: " + val1);

    if (playerNumber < 0 || playerNumber > 0xFF)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: playerNumber out of acceptable range: " + playerNumber);

    if (numPlayers < 0 || numPlayers > 0xFF)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: numPlayers out of acceptable range: " + numPlayers);

    return new AutoValue_StartGame_Notification(
        messageNumber, ID, DESC, val1, playerNumber, numPlayers);
  }
}

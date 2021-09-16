package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class GameChat_Notification extends GameChat {
  private static final String DESC = "In-Game Chat Notification";

  public static AutoValue_GameChat_Notification create(
      int messageNumber, String username, String message) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_GameChat_Notification(messageNumber, ID, DESC, username, message);
  }
}

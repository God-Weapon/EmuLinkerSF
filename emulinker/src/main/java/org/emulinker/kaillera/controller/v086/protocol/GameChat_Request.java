package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class GameChat_Request extends GameChat {
  private static final String DESC = "In-Game Chat Request";
  private static final String USERNAME = "";

  public static AutoValue_GameChat_Request create(int messageNumber, String message)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_GameChat_Request(messageNumber, ID, DESC, USERNAME, message);
  }
}

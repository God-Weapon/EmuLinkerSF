package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class QuitGame_Request extends QuitGame {
  private static final String DESC = "Quit Game Request";
  private static final String USERNAME = "";
  private static final int USER_ID = 0xFFFF;

  public static AutoValue_QuitGame_Request create(int messageNumber) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_QuitGame_Request(messageNumber, ID, DESC, USERNAME, USER_ID);
  }
}

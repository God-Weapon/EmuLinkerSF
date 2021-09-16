package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class Quit_Request extends Quit {
  private static final String DESC = "User Quit Request";

  private static final int USER_ID = 0xFFFF;
  private static final String USERNAME = "";

  public static Quit_Request create(int messageNumber, String message)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_Quit_Request(messageNumber, ID, DESC, USERNAME, USER_ID, message);
  }
}

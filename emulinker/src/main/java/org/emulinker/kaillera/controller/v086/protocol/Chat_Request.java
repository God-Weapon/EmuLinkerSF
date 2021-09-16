package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class Chat_Request extends Chat {
  private static final String DESC = "Chat Request";
  private static final String USERNAME = "";

  public static AutoValue_Chat_Request create(int messageNumber, String message)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);
    return new AutoValue_Chat_Request(messageNumber, ID, DESC, USERNAME, message);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class Chat_Notification extends Chat {
  private static final String DESC = "Chat Notification";

  public static AutoValue_Chat_Notification create(
      int messageNumber, String username, String message) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);
    return new AutoValue_Chat_Notification(messageNumber, ID, DESC, username, message);
  }
}

package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class Chat_Notification extends Chat {
  public static final String DESC = "Chat Notification";

  public Chat_Notification(int messageNumber, String userName, String message)
      throws MessageFormatException {
    super(messageNumber, userName, message);
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  @Override
  public String toString() {
    return getInfoString() + "[userName=" + getUserName() + " message=" + getMessage() + "]";
  }
}

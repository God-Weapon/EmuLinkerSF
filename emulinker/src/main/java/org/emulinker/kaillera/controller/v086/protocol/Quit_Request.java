package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class Quit_Request extends Quit {
  public static final String DESC = "User Quit Request";

  public Quit_Request(int messageNumber, String message) throws MessageFormatException {
    super(messageNumber, "", 0xFFFF, message);
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  @Override
  public String toString() {
    return getInfoString() + "[message=" + getMessage() + "]";
  }
}

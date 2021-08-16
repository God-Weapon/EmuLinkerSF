package org.emulinker.kaillera.controller.v086.protocol;

import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class PlayerDrop_Request extends PlayerDrop {
  public static final String DESC = "Player Drop Request";

  public PlayerDrop_Request(int messageNumber) throws MessageFormatException {
    super(messageNumber, "", (byte) 0);
  }

  public String getDescription() {
    return DESC;
  }

  public String toString() {
    return getInfoString();
  }
}

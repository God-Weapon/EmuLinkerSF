package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

@AutoValue
public abstract class PlayerDrop_Request extends PlayerDrop {
  private static final String DESC = "Player Drop Request";

  private static final String USERNAME = "";
  private static final byte PLAYER_NUMBER = (byte) 0;

  public static AutoValue_PlayerDrop_Request create(int messageNumber)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_PlayerDrop_Request(messageNumber, ID, DESC, USERNAME, PLAYER_NUMBER);
  }
}

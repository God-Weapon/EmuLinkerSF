package org.emulinker.kaillera.controller.connectcontroller.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class ConnectMessage_TOO extends ConnectMessage {
  public static final String ID = "TOO";
  private static final String DESC = "Server Full Response";

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public String description() {
    return DESC;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public int getLength() {
    return (ID.length() + 1);
  }

  @Override
  public void writeTo(ByteBuffer buffer) {
    buffer.put(charset.encode(ID));
    buffer.put((byte) 0x00);
  }

  public static ConnectMessage parse(String msg) throws MessageFormatException {
    if (msg.length() != ID.length() + 1)
      throw new MessageFormatException("Invalid message length!");

    if (!msg.startsWith(ID)) throw new MessageFormatException("Invalid message identifier!");

    if (msg.charAt(msg.length() - 1) != 0x00)
      throw new MessageFormatException("Invalid message stop byte!");

    return new ConnectMessage_TOO();
  }
}

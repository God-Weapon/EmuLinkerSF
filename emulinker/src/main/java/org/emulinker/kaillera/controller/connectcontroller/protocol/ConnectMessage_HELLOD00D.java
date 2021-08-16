package org.emulinker.kaillera.controller.connectcontroller.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.util.EmuUtil;

public class ConnectMessage_HELLOD00D extends ConnectMessage {
  public static final String ID = "HELLOD00D";
  public static final String DESC = "Server Connection Response";

  private int port;

  public ConnectMessage_HELLOD00D(int port) {
    this.port = port;
  }

  public String getID() {
    return ID;
  }

  public String getDescription() {
    return DESC;
  }

  public int getPort() {
    return port;
  }

  public String toString() {
    return DESC + ": port: " + getPort();
  }

  public int getLength() {
    return (ID.length() + Integer.toString(port).length() + 1);
  }

  public void writeTo(ByteBuffer buffer) {
    buffer.put(charset.encode(ID));
    EmuUtil.writeString(buffer, Integer.toString(port), 0x00, charset);
  }

  public static ConnectMessage parse(String msg) throws MessageFormatException {
    if (msg.length() < (ID.length() + 2))
      throw new MessageFormatException("Invalid message length!");

    if (!msg.startsWith(ID)) throw new MessageFormatException("Invalid message identifier!");

    if (msg.charAt(msg.length() - 1) != 0x00)
      throw new MessageFormatException("Invalid message stop byte!");

    try {
      int port = Integer.parseInt(msg.substring(ID.length(), (msg.length() - 1)));
      return new ConnectMessage_HELLOD00D(port);
    } catch (NumberFormatException e) {
      throw new MessageFormatException("Invalid port number!");
    }
  }
}

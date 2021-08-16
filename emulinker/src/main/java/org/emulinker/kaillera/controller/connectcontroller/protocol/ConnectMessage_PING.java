package org.emulinker.kaillera.controller.connectcontroller.protocol;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;

public class ConnectMessage_PING extends ConnectMessage {
  public static final String ID = "PING";
  public static final String DESC = "Client Ping";

  private InetSocketAddress clientSocketAddress;

  public ConnectMessage_PING() {}

  public String getID() {
    return ID;
  }

  public String getDescription() {
    return DESC;
  }

  public void setClientSocketAddress(InetSocketAddress clientSocketAddress) {
    this.clientSocketAddress = clientSocketAddress;
  }

  public InetSocketAddress getClientSocketAddress() {
    return clientSocketAddress;
  }

  public String toString() {
    return DESC;
  }

  public int getLength() {
    return (ID.length() + 1);
  }

  public void writeTo(ByteBuffer buffer) {
    buffer.put(charset.encode(ID));
    buffer.put((byte) 0x00);
  }

  public static ConnectMessage parse(String msg) throws MessageFormatException {
    if (msg.length() != 5) throw new MessageFormatException("Invalid message length!");

    if (!msg.startsWith(ID)) throw new MessageFormatException("Invalid message identifier!");

    if (msg.charAt(msg.length() - 1) != 0x00)
      throw new MessageFormatException("Invalid message stop byte!");

    return new ConnectMessage_PING();
  }
}

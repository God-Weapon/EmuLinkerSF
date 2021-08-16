package org.emulinker.kaillera.controller.connectcontroller.protocol;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.util.EmuUtil;

public class ConnectMessage_HELLO extends ConnectMessage {
  public static final String ID = "HELLO";
  public static final String DESC = "Client Connection Request";

  private String protocol;
  private InetSocketAddress clientSocketAddress;

  public ConnectMessage_HELLO(String protocol) {
    this.protocol = protocol;
  }

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setClientSocketAddress(InetSocketAddress clientSocketAddress) {
    this.clientSocketAddress = clientSocketAddress;
  }

  public InetSocketAddress getClientSocketAddress() {
    return clientSocketAddress;
  }

  @Override
  public String toString() {
    return DESC + ": protocol: " + protocol;
  }

  @Override
  public int getLength() {
    return (ID.length() + protocol.length() + 1);
  }

  @Override
  public void writeTo(ByteBuffer buffer) {
    buffer.put(charset.encode(ID));
    EmuUtil.writeString(buffer, protocol, 0x00, charset);
  }

  public static ConnectMessage parse(String msg) throws MessageFormatException {
    if (msg.length() < (ID.length() + 2))
      throw new MessageFormatException("Invalid message length!");

    if (!msg.startsWith(ID)) throw new MessageFormatException("Invalid message identifier!");

    if (msg.charAt(msg.length() - 1) != 0x00)
      throw new MessageFormatException("Invalid message stop byte!");

    return new ConnectMessage_HELLO(msg.substring(ID.length(), (msg.length() - 1)));
  }
}

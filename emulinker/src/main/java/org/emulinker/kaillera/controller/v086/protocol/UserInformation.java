package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.util.EmuUtil;

public class UserInformation extends V086Message {
  public static final byte ID = 0x03;
  public static final String DESC = "User Information";

  private String userName;
  private String clientType;
  private byte connectionType;

  public UserInformation(int messageNumber, String userName, String clientType, byte connectionType)
      throws MessageFormatException {
    super(messageNumber);
    this.userName = userName;
    this.clientType = clientType;

    if (connectionType < 1 || connectionType > 6)
      throw new MessageFormatException(
          "Invalid "
              + getDescription()
              + " format: connectionType out of acceptable range: "
              + connectionType);

    this.connectionType = connectionType;
  }

  public byte getID() {
    return ID;
  }

  public String getDescription() {
    return DESC;
  }

  public int getBodyLength() {
    return getNumBytes(userName) + getNumBytes(clientType) + 3;
  }

  public String getUserName() {
    return userName;
  }

  public String getClientType() {
    return clientType;
  }

  public byte getConnectionType() {
    return connectionType;
  }

  public String toString() {
    return getInfoString()
        + "[userName="
        + userName
        + " clientType="
        + clientType
        + " connectionType="
        + KailleraUser.CONNECTION_TYPE_NAMES[connectionType]
        + "]";
  }

  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, userName, 0x00, charset);
    EmuUtil.writeString(buffer, clientType, 0x00, charset);
    buffer.put(connectionType);
  }

  public static UserInformation parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

    String userName = EmuUtil.readString(buffer, 0x00, charset);

    if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

    String clientType = EmuUtil.readString(buffer, 0x00, charset);

    if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

    byte connectionType = buffer.get();

    return new UserInformation(messageNumber, userName, clientType, connectionType);
  }
}

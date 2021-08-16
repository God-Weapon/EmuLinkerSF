package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import java.util.*;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class ServerStatus extends V086Message {
  public static final byte ID = 0x04;
  public static final String DESC = "Server Status";

  private List<User> users;
  private List<Game> games;

  public ServerStatus(int messageNumber, List<User> users, List<Game> games)
      throws MessageFormatException {
    super(messageNumber);

    this.users = users;
    this.games = games;
  }

  @Override
  public byte getID() {
    return ID;
  }

  @Override
  public String getDescription() {
    return DESC;
  }

  public List getUsers() {
    return users;
  }

  public List getGames() {
    return games;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getInfoString() + "[users=" + users.size() + " games=" + games.size() + "]");

    if (!users.isEmpty()) sb.append(EmuUtil.LB);

    for (User u : users) {
      sb.append("\t" + u);
      sb.append(EmuUtil.LB);
    }

    if (!games.isEmpty()) sb.append(EmuUtil.LB);

    for (Game g : games) {
      sb.append("\t" + g);
      sb.append(EmuUtil.LB);
    }

    return sb.toString();
  }

  @Override
  public int getBodyLength() {
    int len = 9;
    for (User u : users) len += u.getLength();
    for (Game g : games) len += g.getLength();
    return len;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    buffer.putInt(users.size());
    buffer.putInt(games.size());

    for (User u : users) u.writeTo(buffer);

    for (Game g : games) g.writeTo(buffer);
  }

  public static ServerStatus parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 9) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

    int numUsers = buffer.getInt();
    int numGames = buffer.getInt();

    int minLen = ((numUsers * 10) + (numGames * 13));
    if (buffer.remaining() < minLen) throw new ParseException("Failed byte count validation!");

    List<User> users = new ArrayList<User>(numUsers);
    for (int j = 0; j < numUsers; j++) {
      if (buffer.remaining() < 9) throw new ParseException("Failed byte count validation!");

      String userName = EmuUtil.readString(buffer, 0x00, charset);

      if (buffer.remaining() < 8) throw new ParseException("Failed byte count validation!");

      long ping = UnsignedUtil.getUnsignedInt(buffer);
      byte status = buffer.get();
      int userID = UnsignedUtil.getUnsignedShort(buffer);
      byte connectionType = buffer.get();

      users.add(new User(userName, ping, status, userID, connectionType));
    }

    List<Game> games = new ArrayList<Game>(numGames);
    for (int j = 0; j < numGames; j++) {
      if (buffer.remaining() < 13) throw new ParseException("Failed byte count validation!");

      String romName = EmuUtil.readString(buffer, 0x00, charset);

      if (buffer.remaining() < 10) throw new ParseException("Failed byte count validation!");

      int gameID = buffer.getInt();

      String clientType = EmuUtil.readString(buffer, 0x00, charset);

      if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

      String userName = EmuUtil.readString(buffer, 0x00, charset);

      if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

      String players = EmuUtil.readString(buffer, 0x00, charset);

      if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

      byte status = buffer.get();

      games.add(new Game(romName.toString(), gameID, clientType, userName, players, status));
    }

    return new ServerStatus(messageNumber, users, games);
  }

  public static class User {
    private String userName;
    private long ping;
    private byte status;
    private int userID;
    private byte connectionType;

    public User(String userName, long ping, byte status, int userID, byte connectionType)
        throws MessageFormatException {
      if (userName.length() == 0)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: userName.length == 0, (userID = " + userID + ")");

      if (ping < 0 || ping > 2048) // what should max ping be?
      throw new MessageFormatException(
            "Invalid " + DESC + " format: ping out of acceptable range: " + ping);

      if (status < 0 || status > 2)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: status out of acceptable range: " + status);

      if (userID < 0 || userID > 65535)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: userID out of acceptable range: " + userID);

      if (connectionType < 1 || connectionType > 6)
        throw new MessageFormatException(
            "Invalid "
                + DESC
                + " format: connectionType out of acceptable range: "
                + connectionType);

      this.userName = userName;
      this.ping = ping;
      this.status = status;
      this.userID = userID;
      this.connectionType = connectionType;
    }

    public String getUserName() {
      return userName;
    }

    public long getPing() {
      return ping;
    }

    public byte getStatus() {
      return status;
    }

    public int getUserID() {
      return userID;
    }

    public byte getConnectionType() {
      return connectionType;
    }

    @Override
    public String toString() {
      return "[userName="
          + userName
          + " ping="
          + ping
          + " status="
          + org.emulinker.kaillera.model.KailleraUser.STATUS_NAMES[status]
          + " userID="
          + userID
          + " connectionType="
          + org.emulinker.kaillera.model.KailleraUser.CONNECTION_TYPE_NAMES[connectionType]
          + "]";
    }

    public int getLength() {
      // return (charset.encode(userName).remaining() + 9);
      return (userName.length() + 9);
    }

    public void writeTo(ByteBuffer buffer) {
      EmuUtil.writeString(buffer, userName, 0x00, charset);
      UnsignedUtil.putUnsignedInt(buffer, ping);
      buffer.put(status);
      UnsignedUtil.putUnsignedShort(buffer, userID);
      buffer.put(connectionType);
    }
  }

  public static class Game {
    private String romName;
    private int gameID;
    private String clientType;
    private String userName;
    private String players;
    private byte status;

    public Game(
        String romName, int gameID, String clientType, String userName, String players, byte status)
        throws MessageFormatException {
      if (romName.length() == 0)
        throw new MessageFormatException("Invalid " + DESC + " format: romName.length == 0");

      if (gameID < 0 || gameID > 0xFFFF)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: gameID out of acceptable range: " + gameID);

      if (clientType.length() == 0)
        throw new MessageFormatException("Invalid " + DESC + " format: clientType.length == 0");

      if (userName.length() == 0)
        throw new MessageFormatException("Invalid " + DESC + " format: userName.length == 0");

      if (status < 0 || status > 2)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: gameStatus out of acceptable range: " + status);

      this.romName = romName;
      this.gameID = gameID;
      this.clientType = clientType;
      this.userName = userName;
      this.players = players;
      this.status = status;
    }

    public String getRomName() {
      return romName;
    }

    public int getGameID() {
      return gameID;
    }

    public String getClientType() {
      return clientType;
    }

    public String getUserName() {
      return userName;
    }

    public String getPlayers() {
      return players;
    }

    public byte getStatus() {
      return status;
    }

    @Override
    public String toString() {
      return "[romName="
          + romName
          + " gameID="
          + gameID
          + " clientType="
          + clientType
          + " userName="
          + userName
          + " players="
          + players
          + " status="
          + org.emulinker.kaillera.model.KailleraGame.STATUS_NAMES[status]
          + "]";
    }

    public int getLength() {
      // return (charset.encode(romName).remaining() + 1 + 4 +
      // charset.encode(clientType).remaining() + 1 + charset.encode(userName).remaining() + 1 +
      // charset.encode(players).remaining() + 1 + 1);
      return (romName.length()
          + 1
          + 4
          + clientType.length()
          + 1
          + userName.length()
          + 1
          + players.length()
          + 1
          + 1);
    }

    public void writeTo(ByteBuffer buffer) {
      EmuUtil.writeString(buffer, romName, 0x00, charset);
      buffer.putInt(gameID);
      EmuUtil.writeString(buffer, clientType, 0x00, charset);
      EmuUtil.writeString(buffer, userName, 0x00, charset);
      EmuUtil.writeString(buffer, players, 0x00, charset);
      buffer.put(status);
    }
  }
}

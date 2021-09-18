package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.*;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.controller.v086.V086Utils;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.pico.AppModule;
import org.emulinker.util.*;

@AutoValue
public abstract class ServerStatus extends V086Message {
  public static final byte ID = 0x04;

  private static final String DESC = "Server Status";

  public abstract ImmutableList<User> users();

  public abstract ImmutableList<Game> games();

  public static AutoValue_ServerStatus create(int messageNumber, List<User> users, List<Game> games)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_ServerStatus(
        messageNumber, ID, DESC, ImmutableList.copyOf(users), ImmutableList.copyOf(games));
  }

  // TODO(nue): Get rid of this.
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getInfoString() + "[users=" + users().size() + " games=" + games().size() + "]");

    if (!users().isEmpty()) {
      sb.append(EmuUtil.LB);
    }

    for (User u : users()) {
      sb.append("\t" + u);
      sb.append(EmuUtil.LB);
    }

    if (!games().isEmpty()) {
      sb.append(EmuUtil.LB);
    }

    for (Game g : games()) {
      sb.append("\t");
      sb.append(g);
      sb.append(EmuUtil.LB);
    }

    return sb.toString();
  }

  @Override
  public int getBodyLength() {
    int len = 9;
    len += users().stream().mapToInt(u -> u.getNumBytes()).sum();
    len += games().stream().mapToInt(g -> g.getNumBytes()).sum();
    return len;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    buffer.putInt(users().size());
    buffer.putInt(games().size());

    users().forEach(u -> u.writeTo(buffer));
    games().forEach(g -> g.writeTo(buffer));
  }

  public static ServerStatus parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 9) {
      throw new ParseException("Failed byte count validation!");
    }

    byte b = buffer.get();

    if (b != 0x00) {
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));
    }

    int numUsers = buffer.getInt();
    int numGames = buffer.getInt();

    int minLen = ((numUsers * 10) + (numGames * 13));
    if (buffer.remaining() < minLen) throw new ParseException("Failed byte count validation!");

    List<User> users = new ArrayList<User>(numUsers);
    for (int j = 0; j < numUsers; j++) {
      if (buffer.remaining() < 9) throw new ParseException("Failed byte count validation!");

      String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

      if (buffer.remaining() < 8) throw new ParseException("Failed byte count validation!");

      long ping = UnsignedUtil.getUnsignedInt(buffer);
      byte status = buffer.get();
      int userID = UnsignedUtil.getUnsignedShort(buffer);
      byte connectionType = buffer.get();

      users.add(User.create(userName, ping, status, userID, connectionType));
    }

    List<Game> games = new ArrayList<Game>(numGames);
    for (int j = 0; j < numGames; j++) {
      if (buffer.remaining() < 13) throw new ParseException("Failed byte count validation!");

      String romName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

      if (buffer.remaining() < 10) throw new ParseException("Failed byte count validation!");

      int gameID = buffer.getInt();

      String clientType = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

      if (buffer.remaining() < 5) throw new ParseException("Failed byte count validation!");

      String userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

      if (buffer.remaining() < 3) throw new ParseException("Failed byte count validation!");

      String players = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse);

      if (buffer.remaining() < 1) throw new ParseException("Failed byte count validation!");

      byte status = buffer.get();

      games.add(Game.create(romName.toString(), gameID, clientType, userName, players, status));
    }

    return ServerStatus.create(messageNumber, users, games);
  }

  // TODO(nue): this User and Game class should not be here.
  @AutoValue
  public abstract static class User {
    public abstract String username();

    public abstract long ping();

    public abstract byte status();

    public abstract int userId();

    public abstract byte connectionType();

    public static AutoValue_ServerStatus_User create(
        String username, long ping, byte status, int userId, byte connectionType)
        throws MessageFormatException {
      if (Strings.isNullOrEmpty(username))
        throw new MessageFormatException(
            "Invalid " + DESC + " format: userName.length == 0, (userID = " + userId + ")");

      if (ping < 0 || ping > 2048) // what should max ping be?
      throw new MessageFormatException(
            "Invalid " + DESC + " format: ping out of acceptable range: " + ping);

      if (status < 0 || status > 2)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: status out of acceptable range: " + status);

      if (userId < 0 || userId > 65535)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: userID out of acceptable range: " + userId);

      if (connectionType < 1 || connectionType > 6)
        throw new MessageFormatException(
            "Invalid "
                + DESC
                + " format: connectionType out of acceptable range: "
                + connectionType);

      return new AutoValue_ServerStatus_User(username, ping, status, userId, connectionType);
    }

    // TODO(nue): Get rid of this.
    @Override
    public String toString() {
      return String.format(
          "[userName=%s ping=%d status=%s userID=%d connectionType=%s]",
          username(),
          ping(),
          KailleraUser.STATUS_NAMES[status()],
          userId(),
          KailleraUser.CONNECTION_TYPE_NAMES[connectionType()]);
    }

    public int getNumBytes() {
      return V086Utils.getNumBytes(username()) + 9;
    }

    public void writeTo(ByteBuffer buffer) {
      EmuUtil.writeString(buffer, username(), 0x00, AppModule.charsetDoNotUse);
      UnsignedUtil.putUnsignedInt(buffer, ping());
      buffer.put(status());
      UnsignedUtil.putUnsignedShort(buffer, userId());
      buffer.put(connectionType());
    }
  }

  @AutoValue
  public abstract static class Game {
    public abstract String romName();

    public abstract int gameId();

    public abstract String clientType();

    public abstract String username();

    public abstract String players();

    public abstract byte status();

    public static AutoValue_ServerStatus_Game create(
        String romName, int gameId, String clientType, String username, String players, byte status)
        throws MessageFormatException {
      if (Strings.isNullOrEmpty(romName))
        throw new MessageFormatException("Invalid " + DESC + " format: romName.length == 0");

      if (gameId < 0 || gameId > 0xFFFF)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: gameID out of acceptable range: " + gameId);

      if (Strings.isNullOrEmpty(clientType))
        throw new MessageFormatException("Invalid " + DESC + " format: clientType.length == 0");

      if (Strings.isNullOrEmpty(username))
        throw new MessageFormatException("Invalid " + DESC + " format: userName.length == 0");

      if (status < 0 || status > 2)
        throw new MessageFormatException(
            "Invalid " + DESC + " format: gameStatus out of acceptable range: " + status);

      return new AutoValue_ServerStatus_Game(
          romName, gameId, clientType, username, players, status);
    }

    // TODO(nue): Get rid of this.
    @Override
    public String toString() {
      return "[romName="
          + romName()
          + " gameID="
          + gameId()
          + " clientType="
          + clientType()
          + " userName="
          + username()
          + " players="
          + players()
          + " status="
          + KailleraGame.STATUS_NAMES[status()]
          + "]";
    }

    public int getNumBytes() {
      return V086Utils.getNumBytes(romName())
          + 1
          + 4
          + V086Utils.getNumBytes(clientType())
          + 1
          + V086Utils.getNumBytes(username())
          + 1
          + players().length()
          + 1
          + 1;
    }

    public void writeTo(ByteBuffer buffer) {
      EmuUtil.writeString(buffer, romName(), 0x00, AppModule.charsetDoNotUse);
      buffer.putInt(gameId());
      EmuUtil.writeString(buffer, clientType(), 0x00, AppModule.charsetDoNotUse);
      EmuUtil.writeString(buffer, username(), 0x00, AppModule.charsetDoNotUse);
      EmuUtil.writeString(buffer, players(), 0x00, AppModule.charsetDoNotUse);
      buffer.put(status());
    }
  }
}

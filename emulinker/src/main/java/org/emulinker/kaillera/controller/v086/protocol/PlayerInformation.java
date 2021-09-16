package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.*;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.controller.v086.V086Utils;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.*;

@AutoValue
public abstract class PlayerInformation extends V086Message {
  public static final byte ID = 0x0D;
  private static final String DESC = "Player Information";

  public abstract ImmutableList<Player> players();

  public static AutoValue_PlayerInformation create(int messageNumber, List<Player> players)
      throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    return new AutoValue_PlayerInformation(messageNumber, ID, DESC, ImmutableList.copyOf(players));
  }

  public int getNumPlayers() {
    return players().size();
  }

  // TODO(nue): Get rid of this.
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getInfoString() + "[players=" + players().size() + "]");

    if (!players().isEmpty()) {
      sb.append(EmuUtil.LB);
    }

    for (Player p : players()) {
      sb.append("\t" + p);
      sb.append(EmuUtil.LB);
    }

    return sb.toString();
  }

  @Override
  public int getBodyLength() {
    return 5 + players().stream().mapToInt(p -> p.getNumBytes()).sum();
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    buffer.put((byte) 0x00);
    buffer.putInt(players().size());

    players().forEach(p -> p.writeTo(buffer));
  }

  public static PlayerInformation parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 14) throw new ParseException("Failed byte count validation!");

    byte b = buffer.get();

    if (b != 0x00)
      throw new MessageFormatException(
          "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));

    int numPlayers = buffer.getInt();

    int minLen = (numPlayers * 9);
    if (buffer.remaining() < minLen) throw new ParseException("Failed byte count validation!");

    List<Player> players = new ArrayList<Player>(numPlayers);
    for (int j = 0; j < numPlayers; j++) {
      if (buffer.remaining() < 9) throw new ParseException("Failed byte count validation!");

      String userName = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

      if (buffer.remaining() < 7) throw new ParseException("Failed byte count validation!");

      long ping = UnsignedUtil.getUnsignedInt(buffer);
      int userID = UnsignedUtil.getUnsignedShort(buffer);
      byte connectionType = buffer.get();

      players.add(Player.create(userName, ping, userID, connectionType));
    }

    return PlayerInformation.create(messageNumber, players);
  }

  @AutoValue
  public abstract static class Player {
    public abstract String username();

    public abstract long ping();

    public abstract int userId();

    public abstract byte connectionType();

    public static AutoValue_PlayerInformation_Player create(
        String username, long ping, int userId, byte connectionType) throws MessageFormatException {
      if (Strings.isNullOrEmpty(username)) {
        throw new MessageFormatException(
            "Invalid " + DESC + " format: userName.length == 0, (userID = " + userId + ")");
      }

      if (ping < 0 || ping > 2048) { // what should max ping be?
        throw new MessageFormatException(
            "Invalid " + DESC + " format: ping out of acceptable range: " + ping);
      }

      if (userId < 0 || userId > 65535) {
        throw new MessageFormatException(
            "Invalid " + DESC + " format: userID out of acceptable range: " + userId);
      }

      if (connectionType < 1 || connectionType > 6) {
        throw new MessageFormatException(
            "Invalid "
                + DESC
                + " format: connectionType out of acceptable range: "
                + connectionType);
      }
      return new AutoValue_PlayerInformation_Player(username, ping, userId, connectionType);
    }

    // TODO(nue): Try to get rid of this.
    @Override
    public String toString() {
      return "[userName="
          + username()
          + " ping="
          + ping()
          + " userID="
          + userId()
          + " connectionType="
          + KailleraUser.CONNECTION_TYPE_NAMES[connectionType()]
          + "]";
    }

    public int getNumBytes() {
      return V086Utils.getNumBytes(username()) + 8;
    }

    public void writeTo(ByteBuffer buffer) {
      EmuUtil.writeString(buffer, username(), 0x00, KailleraRelay.config.charset());
      UnsignedUtil.putUnsignedInt(buffer, ping());
      UnsignedUtil.putUnsignedShort(buffer, userId());
      buffer.put(connectionType());
    }
  }
}

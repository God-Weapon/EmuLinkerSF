package org.emulinker.kaillera.controller.v086.protocol;

import java.nio.ByteBuffer;
import java.util.*;

import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.util.*;

public class PlayerInformation extends V086Message
{
	public static final byte	ID		= 0x0D;
	public static final String	DESC	= "Player Information";

	private List<Player>		players;

	public PlayerInformation(int messageNumber, List<Player> players) throws MessageFormatException
	{
		super(messageNumber);
		this.players = players;
	}

	public byte getID()
	{
		return ID;
	}

	public String getDescription()
	{
		return DESC;
	}

	public int getNumPlayers()
	{
		return players.size();
	}

	public List<Player> getPlayers()
	{
		return players;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getInfoString() + "[players=" + players.size() + "]");

		if (!players.isEmpty())
			sb.append(EmuUtil.LB);

		for (Player p : players)
		{
			sb.append("\t" + p);
			sb.append(EmuUtil.LB);
		}

		return sb.toString();
	}

	public int getBodyLength()
	{
		int len = 5;
		for (Player p : players)
			len += p.getLength();
		return len;
	}

	public void writeBodyTo(ByteBuffer buffer)
	{
		buffer.put((byte) 0x00);
		buffer.putInt(players.size());

		for (Player p : players)
			p.writeTo(buffer);
	}

	public static PlayerInformation parse(int messageNumber, ByteBuffer buffer) throws ParseException, MessageFormatException
	{
		if (buffer.remaining() < 14)
			throw new ParseException("Failed byte count validation!");

		byte b = buffer.get();
		
		if (b != 0x00)
			throw new MessageFormatException("Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b));
		
		int numPlayers = buffer.getInt();

		int minLen = (numPlayers * 9);
		if (buffer.remaining() < minLen)
			throw new ParseException("Failed byte count validation!");

		List<Player> players = new ArrayList<Player>(numPlayers);
		for (int j = 0; j < numPlayers; j++)
		{
			if (buffer.remaining() < 9)
				throw new ParseException("Failed byte count validation!");

			String userName = EmuUtil.readString(buffer, 0x00, charset);
			
			if (buffer.remaining() < 7)
				throw new ParseException("Failed byte count validation!");
			
			long ping = UnsignedUtil.getUnsignedInt(buffer);
			int userID = UnsignedUtil.getUnsignedShort(buffer);
			byte connectionType = buffer.get();

			players.add(new Player(userName, ping, userID, connectionType));
		}

		return new PlayerInformation(messageNumber, players);
	}

	public static class Player
	{
		private String	userName;
		private long	ping;
		private int		userID;
		private byte	connectionType;

		public Player(String userName, long ping, int userID, byte connectionType) throws MessageFormatException
		{
			if (userName.length() == 0)
				throw new MessageFormatException("Invalid " + DESC + " format: userName.length == 0, (userID = " + userID + ")");

			if (ping < 0 || ping > 2048) // what should max ping be?
				throw new MessageFormatException("Invalid " + DESC + " format: ping out of acceptable range: " + ping);

			if (userID < 0 || userID > 65535)
				throw new MessageFormatException("Invalid " + DESC + " format: userID out of acceptable range: " + userID);

			if (connectionType < 1 || connectionType > 6)
				throw new MessageFormatException("Invalid " + DESC + " format: connectionType out of acceptable range: " + connectionType);

			this.userName = userName;
			this.ping = ping;
			this.userID = userID;
			this.connectionType = connectionType;
		}

		public String getUserName()
		{
			return userName;
		}

		public long getPing()
		{
			return ping;
		}

		public int getUserID()
		{
			return userID;
		}

		public byte getConnectionType()
		{
			return connectionType;
		}

		public String toString()
		{
			return "[userName=" + userName + " ping=" + ping + " userID=" + userID + " connectionType=" + org.emulinker.kaillera.model.KailleraUser.CONNECTION_TYPE_NAMES[connectionType] + "]";
		}

		public int getLength()
		{
			//return (charset.encode(userName).remaining() + 2);
			return (userName.length() + 8);
		}

		public void writeTo(ByteBuffer buffer)
		{
			EmuUtil.writeString(buffer, userName, 0x00, charset);
			UnsignedUtil.putUnsignedInt(buffer, ping);
			UnsignedUtil.putUnsignedShort(buffer, userID);
			buffer.put(connectionType);
		}
	}
}

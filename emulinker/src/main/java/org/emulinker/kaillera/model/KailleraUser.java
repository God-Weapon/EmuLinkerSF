package org.emulinker.kaillera.model;

import java.net.InetSocketAddress;
import java.util.Collection;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.kaillera.model.impl.KailleraGameImpl;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;

public interface KailleraUser {
  public static final byte CONNECTION_TYPE_LAN = 1;
  public static final byte CONNECTION_TYPE_EXCELLENT = 2;
  public static final byte CONNECTION_TYPE_GOOD = 3;
  public static final byte CONNECTION_TYPE_AVERAGE = 4;
  public static final byte CONNECTION_TYPE_LOW = 5;
  public static final byte CONNECTION_TYPE_BAD = 6;

  public static final String[] CONNECTION_TYPE_NAMES = {
    "DISABLED", "LAN", "Excellent", "Good", "Average", "Low", "Bad"
  };

  public static final byte STATUS_PLAYING = 0;
  public static final byte STATUS_IDLE = 1;
  public static final byte STATUS_CONNECTING = 2;
  public static final String[] STATUS_NAMES = {"Playing", "Idle", "Connecting"};

  public int getID();

  public InetSocketAddress getConnectSocketAddress();

  public String getProtocol();

  public long getConnectTime();

  public int getFrameCount();

  public byte[] getLostInput();

  public int getArraySize();

  public int getBytesPerAction();

  public void setTempDelay(int tempDelay);

  public void setFrameCount(int frameCount);

  public boolean getMsg();

  public void setMsg(boolean msg);

  public int getStatus();

  public void setLastMsgID(int lastMsgID);

  public int getDelay();

  public int getLastMsgID();

  public boolean getP2P();

  public int getTimeouts();

  public void setTimeouts(int timeouts);

  public boolean findIgnoredUser(String address);

  public void setP2P(boolean p2P);

  public void setPlayerNumber(int playerNumber);

  public boolean getMute();

  public void setMute(boolean mute);

  public KailleraGameImpl getGame();

  public int getAccess();

  public boolean removeIgnoredUser(String address, boolean removeAll);

  public boolean searchIgnoredUsers(String address);

  public void setIgnoreAll(boolean ignoreAll);

  public boolean getIgnoreAll();

  public void addIgnoredUser(String address);

  public Collection<KailleraUserImpl> getUsers();

  public String getName();

  public void setName(String name);

  public boolean getStealth();

  public void setStealth(boolean stealth);

  public String getClientType();

  public boolean isEmuLinkerClient();

  public void setClientType(String clientType);

  public byte getConnectionType();

  public void setConnectionType(byte connectionType);

  public InetSocketAddress getSocketAddress();

  public void setSocketAddress(InetSocketAddress clientSocketAddress);

  public int getPing();

  public void setPing(int ping);

  public void login()
      throws PingTimeException, ClientAddressException, ConnectionTypeException, UserNameException,
          LoginException;

  public long getLastActivity();

  public void updateLastActivity();

  public void updateLastKeepAlive();

  public long getLastKeepAlive();

  public boolean isLoggedIn();

  public KailleraServer getServer();

  public KailleraEventListener getListener();

  public void chat(String message) throws ChatException, FloodException;

  public KailleraGame createGame(String romName) throws CreateGameException, FloodException;

  public void quit(String message)
      throws QuitException, DropGameException, QuitGameException, CloseGameException;

  public KailleraGame joinGame(int gameID) throws JoinGameException;

  public int getPlayerNumber();

  public void startGame() throws StartGameException;

  public void gameChat(String message, int messageID) throws GameChatException;

  public void gameKick(int userID) throws GameKickException;

  public void playerReady() throws UserReadyException;

  public void addGameData(byte[] data) throws GameDataException;

  public void dropGame() throws DropGameException;

  public void quitGame() throws DropGameException, QuitGameException, CloseGameException;

  public void droppedPacket();

  public void stop();
}

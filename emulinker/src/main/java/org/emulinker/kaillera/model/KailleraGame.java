package org.emulinker.kaillera.model;

import java.util.Collection;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.kaillera.model.impl.PlayerActionQueue;

public interface KailleraGame {
  public static final byte STATUS_WAITING = 0;
  public static final byte STATUS_PLAYING = 2;
  public static final byte STATUS_SYNCHRONIZING = 1;

  public static final String[] STATUS_NAMES = {"Waiting", "Playing", "Synchronizing"};

  public int getID();

  public String getRomName();

  public String getClientType();

  public PlayerActionQueue[] getPlayerActionQueue();

  public void setStartTimeout(boolean startTimeout);

  public boolean getStartTimeout();

  public void setSameDelay(boolean sameDelay);

  public boolean getSameDelay();

  public int getHighestPing();

  public int getDelay();

  public long getStartTimeoutTime();

  public KailleraUser getOwner();

  public void setDelay(int delay);

  public void setMaxUsers(int maxUsers);

  public int getMaxUsers();

  public int getStartN();

  public void setStartN(int startN);

  public boolean getP2P();

  public void setP2P(boolean p2P);

  public void setMaxPing(int maxPing);

  public int getMaxPing();

  public int getPlayerNumber(KailleraUser user);

  public int getNumPlayers();

  public KailleraUser getPlayer(int playerNumber);

  public Collection<? extends KailleraUser> getPlayers();

  public int getStatus();

  public KailleraServer getServer();

  public void droppedPacket(KailleraUser user);

  public int join(KailleraUser user) throws JoinGameException;

  public void chat(KailleraUser user, String message) throws GameChatException;

  public void kick(KailleraUser requester, int userID) throws GameKickException;

  public void start(KailleraUser user) throws StartGameException;

  public void ready(KailleraUser user, int playerNumber) throws UserReadyException;

  public void addData(KailleraUser user, int playerNumber, byte[] data) throws GameDataException;

  public void drop(KailleraUser user, int playerNumber) throws DropGameException;

  public void quit(KailleraUser user, int playerNumber)
      throws DropGameException, QuitGameException, CloseGameException;
}

package org.emulinker.kaillera.model.impl;

import com.google.common.flogger.FluentLogger;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.util.*;

public final class KailleraGameImpl implements KailleraGame {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private int id;
  private String romName;
  private String toString;
  private Date startDate;

  private String lastAddress = "null";
  private int lastAddressCount = 0;

  private static int chatFloodTime = 3;
  private int maxUsers = 8;
  private int delay;
  private String aEmulator = "any";
  private String aConnection = "any";
  private int maxPing = 1000;
  private int startN = -1;
  private boolean p2P = false;
  private boolean sameDelay = false;
  public boolean swap = false;

  private int highestPing = 0;
  private int bufferSize;
  private boolean startTimeout = false;
  private long startTimeoutTime;
  private int timeoutMillis;
  private int desynchTimeouts;

  private KailleraServerImpl server;
  private KailleraUserImpl owner;
  private List<KailleraUserImpl> players = new CopyOnWriteArrayList<KailleraUserImpl>();
  private StatsCollector statsCollector;

  private List<String> kickedUsers = new ArrayList<String>();
  private List<String> mutedUsers = new ArrayList<String>();

  private int status = KailleraGame.STATUS_WAITING;
  private boolean synched = false;
  private int actionsPerMessage;
  private PlayerActionQueue[] playerActionQueues;
  private AutoFireDetector autoFireDetector;

  public KailleraGameImpl(
      int gameID,
      String romName,
      KailleraUserImpl owner,
      KailleraServerImpl server,
      int bufferSize,
      int timeoutMillis,
      int desynchTimeouts) {
    this.id = gameID;
    this.romName = romName;
    this.owner = owner;
    this.server = server;
    this.actionsPerMessage = owner.getConnectionType();
    this.bufferSize = bufferSize;
    this.timeoutMillis = 100; // timeoutMillis;
    this.desynchTimeouts = 120; // desynchTimeouts;

    toString =
        "Game"
            + id
            + "("
            + (romName.length() > 15 ? (romName.substring(0, 15) + "...") : romName)
            + ")";
    startDate = new Date();

    statsCollector = server.getStatsCollector();
    autoFireDetector = server.getAutoFireDetector(this);
  }

  @Override
  public int getID() {
    return id;
  }

  public List<String> getMutedUsers() {
    return mutedUsers;
  }

  @Override
  public int getDelay() {
    return delay;
  }

  public String getAEmulator() {
    return aEmulator;
  }

  public void setAEmulator(String aEmulator) {
    this.aEmulator = aEmulator;
  }

  public String getAConnection() {
    return aConnection;
  }

  public void setAConnection(String aConnection) {
    this.aConnection = aConnection;
  }

  @Override
  public PlayerActionQueue[] getPlayerActionQueue() {
    return playerActionQueues;
  }

  @Override
  public void setDelay(int delay) {
    this.delay = delay;
  }

  @Override
  public void setStartTimeout(boolean startTimeout) {
    this.startTimeout = startTimeout;
  }

  @Override
  public boolean getStartTimeout() {
    return startTimeout;
  }

  @Override
  public void setSameDelay(boolean sameDelay) {
    this.sameDelay = sameDelay;
  }

  @Override
  public boolean getSameDelay() {
    return sameDelay;
  }

  @Override
  public long getStartTimeoutTime() {
    return startTimeoutTime;
  }

  @Override
  public int getStartN() {
    return startN;
  }

  @Override
  public boolean getP2P() {
    return p2P;
  }

  @Override
  public void setP2P(boolean p2P) {
    this.p2P = p2P;
  }

  @Override
  public void setStartN(int startN) {
    this.startN = startN;
  }

  @Override
  public String getRomName() {
    return romName;
  }

  public Date getStartDate() {
    return startDate;
  }

  @Override
  public void setMaxUsers(int maxUsers) {
    this.maxUsers = maxUsers;
    server.addEvent(new GameStatusChangedEvent(server, this));
  }

  @Override
  public int getMaxUsers() {
    return maxUsers;
  }

  @Override
  public int getHighestPing() {
    return highestPing;
  }

  @Override
  public void setMaxPing(int maxPing) {
    this.maxPing = maxPing;
  }

  @Override
  public int getMaxPing() {
    return maxPing;
  }

  @Override
  public KailleraUser getOwner() {
    return owner;
  }

  @Override
  public int getPlayerNumber(KailleraUser user) {
    return (players.indexOf(user) + 1);
  }

  @Override
  public KailleraUser getPlayer(int playerNumber) {
    if (playerNumber > players.size()) {
      logger.atSevere().log(
          this + ": getPlayer(" + playerNumber + ") failed! (size = " + players.size() + ")");
      return null;
    }

    return players.get((playerNumber - 1));
  }

  @Override
  public int getNumPlayers() {
    return players.size();
  }

  @Override
  public List<KailleraUserImpl> getPlayers() {
    return players;
  }

  @Override
  public int getStatus() {
    return status;
  }

  public boolean isSynched() {
    return synched;
  }

  @Override
  public KailleraServerImpl getServer() {
    return server;
  }

  void setStatus(int status) {
    this.status = status;
    server.addEvent(new GameStatusChangedEvent(server, this));
  }

  @Override
  public String getClientType() {
    return getOwner().getClientType();
  }

  @Override
  public String toString() {
    return toString;
  }

  public String toDetailedString() {
    StringBuilder sb = new StringBuilder();
    sb.append("KailleraGame[id=");
    sb.append(getID());
    sb.append(" romName=");
    sb.append(getRomName());
    sb.append(" owner=");
    sb.append(getOwner());
    sb.append(" numPlayers=");
    sb.append(getNumPlayers());
    sb.append(" status=");
    sb.append(KailleraGame.STATUS_NAMES[getStatus()]);
    sb.append("]");
    return sb.toString();
  }

  int getPlayingCount() {
    int count = 0;
    for (KailleraUserImpl player : players) {
      if (player.getStatus() == KailleraUser.STATUS_PLAYING) count++;
    }

    return count;
  }

  int getSynchedCount() {
    if (playerActionQueues == null) return 0;

    int count = 0;
    for (int i = 0; i < playerActionQueues.length; i++) {
      if (playerActionQueues[i].isSynched()) count++;
    }

    return count;

    // return dataQueues.size();
    //		return readyCount;
  }

  void addEvent(GameEvent event) {
    for (KailleraUserImpl player : players) player.addEvent(event);
  }

  public AutoFireDetector getAutoFireDetector() {
    return autoFireDetector;
  }

  @Override
  public synchronized void chat(KailleraUser user, String message) throws GameChatException {
    if (!players.contains(user)) {
      logger.atWarning().log(user + " game chat denied: not in " + this);
      throw new GameChatException(EmuLang.getString("KailleraGameImpl.GameChatErrorNotInGame"));
    }

    if (user.getAccess() == AccessManager.ACCESS_NORMAL) {
      if (server.getMaxGameChatLength() > 0 && message.length() > server.getMaxGameChatLength()) {
        logger.atWarning().log(
            user + " gamechat denied: Message Length > " + server.getMaxGameChatLength());
        addEvent(
            new GameInfoEvent(
                this, EmuLang.getString("KailleraGameImpl.GameChatDeniedMessageTooLong"), user));
        throw new GameChatException(
            EmuLang.getString("KailleraGameImpl.GameChatDeniedMessageTooLong"));
      }
    }

    logger.atInfo().log(user + ", " + this + " gamechat: " + message);
    addEvent(new GameChatEvent(this, user, message));
  }

  public synchronized void announce(String announcement, KailleraUser user) {
    addEvent(new GameInfoEvent(this, announcement, user));
  }

  @Override
  public synchronized void kick(KailleraUser user, int userID) throws GameKickException {
    if (user.getAccess() < AccessManager.ACCESS_ADMIN) {
      if (!user.equals(getOwner())) {
        logger.atWarning().log(user + " kick denied: not the owner of " + this);
        throw new GameKickException(
            EmuLang.getString("KailleraGameImpl.GameKickDeniedNotGameOwner"));
      }
    }

    if (user.getID() == userID) {
      logger.atWarning().log(user + " kick denied: attempt to kick self");
      throw new GameKickException(
          EmuLang.getString("KailleraGameImpl.GameKickDeniedCannotKickSelf"));
    }

    for (KailleraUserImpl player : players) {
      if (player.getID() == userID) {
        try {
          if (user.getAccess() != AccessManager.ACCESS_SUPERADMIN) {
            if (player.getAccess() >= AccessManager.ACCESS_ADMIN) {
              return;
            }
          }

          logger.atInfo().log(user + " kicked: " + userID + " from " + this);
          // SF MOD - Changed to IP rather than ID
          kickedUsers.add(player.getConnectSocketAddress().getAddress().getHostAddress());
          player.quitGame();
          return;
        } catch (Exception e) {
          // this shouldn't happen
          logger.atSevere().withCause(e).log(
              "Caught exception while making user quit game! This shouldn't happen!");
        }
      }
    }

    logger.atWarning().log(user + " kick failed: user " + userID + " not found in: " + this);
    throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickErrorUserNotFound"));
  }

  @Override
  public synchronized int join(KailleraUser user) throws JoinGameException {

    int access = server.getAccessManager().getAccess(user.getSocketAddress().getAddress());

    // SF MOD - Join room spam protection
    if (lastAddress.equals(user.getConnectSocketAddress().getAddress().getHostAddress())) {
      lastAddressCount++;
      if (lastAddressCount >= 4) {
        logger.atInfo().log(user + " join spam protection: " + user.getID() + " from " + this);
        // SF MOD - Changed to IP rather than ID
        if (access < AccessManager.ACCESS_ADMIN) {
          kickedUsers.add(user.getConnectSocketAddress().getAddress().getHostAddress());
          try {
            user.quitGame();
          } catch (Exception e) {
          }
          throw new JoinGameException("Spam Protection");
        }
      }
    } else {
      lastAddressCount = 0;
      lastAddress = user.getConnectSocketAddress().getAddress().getHostAddress();
    }

    if (players.contains(user)) {
      logger.atWarning().log(user + " join game denied: already in " + this);
      throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameErrorAlreadyInGame"));
    }

    if (access < AccessManager.ACCESS_ELEVATED && getNumPlayers() >= getMaxUsers()) {
      logger.atWarning().log(user + " join game denied: max users reached " + this);
      throw new JoinGameException("This room's user capacity has been reached.");
    }

    if (access < AccessManager.ACCESS_ELEVATED && user.getPing() > getMaxPing()) {
      logger.atWarning().log(user + " join game denied: max ping reached " + this);
      throw new JoinGameException("Your ping is too high for this room.");
    }

    if (access < AccessManager.ACCESS_ELEVATED && !aEmulator.equals("any")) {
      if (!aEmulator.equals(user.getClientType())) {
        logger.atWarning().log(
            user + " join game denied: owner doesn't allow that emulator: " + user.getClientType());
        throw new JoinGameException("Owner only allows emulator version: " + aEmulator);
      }
    }

    if (access < AccessManager.ACCESS_ELEVATED && !aConnection.equals("any")) {
      if (user.getConnectionType() != getOwner().getConnectionType()) {
        logger.atWarning().log(
            user
                + "join game denied: owner doesn't allow that connection type: "
                + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);
        throw new JoinGameException(
            "Owner only allows connection type: "
                + KailleraUser.CONNECTION_TYPE_NAMES[getOwner().getConnectionType()]);
      }
    }

    if (access < AccessManager.ACCESS_ADMIN
        && kickedUsers.contains(user.getConnectSocketAddress().getAddress().getHostAddress())) {
      logger.atWarning().log(user + " join game denied: previously kicked: " + this);
      throw new JoinGameException(
          EmuLang.getString("KailleraGameImpl.JoinGameDeniedPreviouslyKicked"));
    }

    if (access == AccessManager.ACCESS_NORMAL && getStatus() != KailleraGame.STATUS_WAITING) {
      logger.atWarning().log(user + " join game denied: attempt to join game in progress: " + this);
      throw new JoinGameException(
          EmuLang.getString("KailleraGameImpl.JoinGameDeniedGameIsInProgress"));
    }

    if (mutedUsers.contains(user.getConnectSocketAddress().getAddress().getHostAddress())) {
      user.setMute(true);
    }

    players.add((KailleraUserImpl) user);
    user.setPlayerNumber(players.size());

    server.addEvent(new GameStatusChangedEvent(server, this));

    logger.atInfo().log(user + " joined: " + this);
    addEvent(new UserJoinedGameEvent(this, user));

    // SF MOD - /startn
    if (getStartN() != -1) {
      if (players.size() >= getStartN()) {
        try {
          Thread.sleep(1000);
        } catch (Exception e) {
        }
        try {
          start(getOwner());
        } catch (Exception e) {
        }
      }
    }

    // if(user.equals(owner))
    // {

    // TODO(nue): Localize this welcome message?
    // announce(
    //     "Help: "
    //         + getServer().getReleaseInfo().getProductName()
    //         + " v"
    //         + getServer().getReleaseInfo().getVersionString()
    //         + ": "
    //         + getServer().getReleaseInfo().getReleaseDate()
    //         + " - Visit: www.EmuLinker.org",
    //     user);
    // announce("************************", user);
    // announce("Type /p2pon to ignore ALL server activity during gameplay.", user);
    // announce("This will reduce lag that you contribute due to a busy server.", user);
    // announce("If server is greater than 60 users, option is auto set.", user);
    // announce("************************", user);

    /*
    if(autoFireDetector != null)
    {
    	if(autoFireDetector.getSensitivity() > 0)
    	{
    		announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOn"));
    		announce(EmuLang.getString("KailleraGameImpl.AutofireCurrentSensitivity", autoFireDetector.getSensitivity()));
    	}
    	else
    	{
    		announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOff"));
    	}
    	announce(EmuLang.getString("KailleraGameImpl.GameHelp"));
    }
    */
    // }

    // new SF MOD - different emulator versions notifications
    if (access < AccessManager.ACCESS_ADMIN
        && !user.getClientType().equals(owner.getClientType())
        && !owner.getGame().getRomName().startsWith("*"))
      addEvent(
          new GameInfoEvent(
              this,
              user.getName() + " using different emulator version: " + user.getClientType(),
              null));

    return (players.indexOf(user) + 1);
  }

  @Override
  public synchronized void start(KailleraUser user) throws StartGameException {

    int access = server.getAccessManager().getAccess(user.getSocketAddress().getAddress());

    if (!user.equals(getOwner()) && access < AccessManager.ACCESS_ADMIN) {
      logger.atWarning().log(user + " start game denied: not the owner of " + this);
      throw new StartGameException(
          EmuLang.getString("KailleraGameImpl.StartGameDeniedOnlyOwnerMayStart"));
    }

    if (status == KailleraGame.STATUS_SYNCHRONIZING) {
      logger.atWarning().log(
          user + " start game failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]);
      throw new StartGameException(
          EmuLang.getString("KailleraGameImpl.StartGameErrorSynchronizing"));
    } else if (status == KailleraGame.STATUS_PLAYING) {
      logger.atWarning().log(
          user + " start game failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]);
      throw new StartGameException(
          EmuLang.getString("KailleraGameImpl.StartGameErrorStatusIsPlaying"));
    }

    if (access == AccessManager.ACCESS_NORMAL
        && getNumPlayers() < 2
        && !server.getAllowSinglePlayer()) {
      logger.atWarning().log(user + " start game denied: " + this + " needs at least 2 players");
      throw new StartGameException(
          EmuLang.getString("KailleraGameImpl.StartGameDeniedSinglePlayerNotAllowed"));
    }

    // do not start if not game
    if (owner.getGame().getRomName().startsWith("*")) return;

    for (KailleraUser player : players) {
      if (player.getStealth() == false) {
        if (player.getConnectionType() != owner.getConnectionType()) {
          logger.atWarning().log(
              user
                  + " start game denied: "
                  + this
                  + ": All players must use the same connection type");
          addEvent(
              new GameInfoEvent(
                  this,
                  EmuLang.getString(
                      "KailleraGameImpl.StartGameConnectionTypeMismatchInfo",
                      KailleraUser.CONNECTION_TYPE_NAMES[owner.getConnectionType()]),
                  null));
          throw new StartGameException(
              EmuLang.getString("KailleraGameImpl.StartGameDeniedConnectionTypeMismatch"));
        }

        if (!player.getClientType().equals(getClientType())) {
          logger.atWarning().log(
              user + " start game denied: " + this + ": All players must use the same emulator!");
          addEvent(
              new GameInfoEvent(
                  this,
                  EmuLang.getString(
                      "KailleraGameImpl.StartGameEmulatorMismatchInfo", getClientType()),
                  null));
          throw new StartGameException(
              EmuLang.getString("KailleraGameImpl.StartGameDeniedEmulatorMismatch"));
        }
      }
    }

    logger.atInfo().log(user + " started: " + this);
    setStatus(KailleraGame.STATUS_SYNCHRONIZING);

    if (autoFireDetector != null) autoFireDetector.start(players.size());

    playerActionQueues = new PlayerActionQueue[players.size()];

    startTimeout = false;
    delay = 1;

    if (server.getUsers().size() > 60) {
      p2P = true;
    }

    for (int i = 0; i < playerActionQueues.length && i < players.size(); i++) {
      KailleraUserImpl player = players.get(i);
      int playerNumber = (i + 1);

      if (!swap) player.setPlayerNumber(playerNumber);
      player.setTimeouts(0);
      player.setFrameCount(0);

      playerActionQueues[i] =
          new PlayerActionQueue(
              playerNumber, player, getNumPlayers(), bufferSize, timeoutMillis, true);
      // playerActionQueues[i] = new PlayerActionQueue(playerNumber, player, getNumPlayers(),
      // GAME_BUFFER_SIZE, (player.getPing()*3));
      // SF MOD - player.setPlayerNumber(playerNumber);
      // SF MOD - Delay Value = [(60/connectionType) * (ping/1000)] + 1

      double delayVal =
          ((60 / player.getConnectionType()) * ((double) player.getPing() / 1000)) + 1;

      player.setDelay((int) delayVal);
      if ((int) delayVal > delay) {
        delay = (int) delayVal;
      }

      if (player.getPing() > highestPing) {
        highestPing = user.getPing();
      }

      if (p2P) {
        player.setP2P(true);
        announce("This game is ignoring ALL server activity during gameplay!", player);
      }
      /*else{
      	player.setP2P(false);
      }*/

      logger.atInfo().log(this + ": " + player + " is player number " + playerNumber);

      if (autoFireDetector != null) autoFireDetector.addPlayer(player, playerNumber);
    }

    if (statsCollector != null) statsCollector.gameStarted(server, this);

    /*if(user.getConnectionType() > KailleraUser.CONNECTION_TYPE_GOOD || user.getConnectionType() < KailleraUser.CONNECTION_TYPE_GOOD){
    	//sameDelay = true;
    }*/

    // timeoutMillis = highestPing;
    addEvent(new GameStartedEvent(this));
  }

  @Override
  public synchronized void ready(KailleraUser user, int playerNumber) throws UserReadyException {
    if (!players.contains(user)) {
      logger.atWarning().log(user + " ready game failed: not in " + this);
      throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorNotInGame"));
    }

    if (status != KailleraGame.STATUS_SYNCHRONIZING) {
      logger.atWarning().log(
          user + " ready failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]);
      throw new UserReadyException(
          EmuLang.getString("KailleraGameImpl.ReadyGameErrorIncorrectState"));
    }

    if (playerActionQueues == null) {
      logger.atSevere().log(user + " ready failed: " + this + " playerActionQueues == null!");
      throw new UserReadyException(
          EmuLang.getString("KailleraGameImpl.ReadyGameErrorInternalError"));
    }

    logger.atInfo().log(user + " (player " + playerNumber + ") is ready to play: " + this);
    playerActionQueues[(playerNumber - 1)].setSynched(true);

    if (getSynchedCount() == getNumPlayers()) {
      logger.atInfo().log(this + " all players are ready: starting...");

      setStatus(KailleraGame.STATUS_PLAYING);
      synched = true;
      startTimeoutTime = System.currentTimeMillis();
      addEvent(new AllReadyEvent(this));

      int frameDelay = ((delay + 1) * owner.getConnectionType()) - 1;
      if (sameDelay) {
        announce("This game's delay is: " + delay + " (" + frameDelay + " frame delay)", null);
      } else {
        for (int i = 0; i < playerActionQueues.length && i < players.size(); i++) {
          KailleraUserImpl player = players.get(i);
          // do not show delay if stealth mode
          if (player != null && !player.getStealth()) {
            frameDelay = ((player.getDelay() + 1) * player.getConnectionType()) - 1;
            announce(
                "P"
                    + (i + 1)
                    + " Delay = "
                    + player.getDelay()
                    + " ("
                    + frameDelay
                    + " frame delay)",
                null);
          }
        }
      }
    }
  }

  @Override
  public synchronized void drop(KailleraUser user, int playerNumber) throws DropGameException {
    if (!players.contains(user)) {
      logger.atWarning().log(user + " drop game failed: not in " + this);
      throw new DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorNotInGame"));
    }

    if (playerActionQueues == null) {
      logger.atSevere().log(user + " drop failed: " + this + " playerActionQueues == null!");
      throw new DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorInternalError"));
    }

    logger.atInfo().log(user + " dropped: " + this);

    if ((playerNumber - 1) < playerActionQueues.length)
      playerActionQueues[(playerNumber - 1)].setSynched(false);

    if (getSynchedCount() < 2 && synched) {
      synched = false;
      for (PlayerActionQueue q : playerActionQueues) q.setSynched(false);
      logger.atInfo().log(this + ": game desynched: less than 2 players playing!");
    }

    if (autoFireDetector != null) autoFireDetector.stop(playerNumber);

    if (getPlayingCount() == 0) {
      if (getStartN() != -1) {
        setStartN(-1);
        announce("StartN is now off.", null);
      }
      setStatus(KailleraGame.STATUS_WAITING);
    }
    addEvent(new UserDroppedGameEvent(this, user, playerNumber));

    if (user.getP2P()) {
      // KailleraUserImpl u = (KailleraUserImpl) user;
      // u.addEvent(ServerACK.create(.getNextMessageNumber());
      // u.addEvent(new ConnectedEvent(server, user));
      // u.addEvent(new UserQuitEvent(server, user, "Rejoining..."));
      // try{user.quit("Rejoining...");}catch(Exception e){}
      announce("Rejoin server to update client of ignored server activity!", user);
    }
  }

  @Override
  public void quit(KailleraUser user, int playerNumber)
      throws DropGameException, QuitGameException, CloseGameException {
    synchronized (this) {
      if (!players.remove(user)) {
        logger.atWarning().log(user + " quit game failed: not in " + this);
        throw new QuitGameException(EmuLang.getString("KailleraGameImpl.QuitGameErrorNotInGame"));
      }

      logger.atInfo().log(user + " quit: " + this);

      addEvent(new UserQuitGameEvent(this, user));

      user.setP2P(false);

      swap = false;

      if (status == STATUS_WAITING) {
        for (int i = 0; i < this.getNumPlayers(); i++) {
          getPlayer(i + 1).setPlayerNumber(i + 1);
          logger.atFine().log(
              getPlayer(i + 1).getName() + ":::" + getPlayer(i + 1).getPlayerNumber());
        }
      }
    }

    if (user.equals(owner)) server.closeGame(this, user);
    else server.addEvent(new GameStatusChangedEvent(server, this));
  }

  synchronized void close(KailleraUser user) throws CloseGameException {
    if (!user.equals(owner)) {
      logger.atWarning().log(user + " close game denied: not the owner of " + this);
      throw new CloseGameException(
          EmuLang.getString("KailleraGameImpl.CloseGameErrorNotGameOwner"));
    }

    if (synched) {
      synched = false;
      for (PlayerActionQueue q : playerActionQueues) q.setSynched(false);
      logger.atInfo().log(this + ": game desynched: game closed!");
    }

    for (KailleraUserImpl player : players) {
      player.setStatus(KailleraUser.STATUS_IDLE);
      player.setMute(false);
      player.setP2P(false);
      player.setGame(null);
    }

    if (autoFireDetector != null) autoFireDetector.stop();

    players.clear();
  }

  @Override
  public synchronized void droppedPacket(KailleraUser user) {
    if (!synched) return;

    int playerNumber = user.getPlayerNumber();
    if (user.getPlayerNumber() > playerActionQueues.length) {
      logger.atInfo().log(
          this
              + ": "
              + user
              + ": player desynched: dropped a packet! Also left the game already: KailleraGameImpl -> DroppedPacket");
    }

    if (playerActionQueues != null && playerActionQueues[(playerNumber - 1)].isSynched()) {
      playerActionQueues[(playerNumber - 1)].setSynched(false);
      logger.atInfo().log(this + ": " + user + ": player desynched: dropped a packet!");
      addEvent(
          new PlayerDesynchEvent(
              this,
              user,
              EmuLang.getString("KailleraGameImpl.DesynchDetectedDroppedPacket", user.getName())));

      if (getSynchedCount() < 2 && synched) {
        synched = false;
        for (PlayerActionQueue q : playerActionQueues) q.setSynched(false);
        logger.atInfo().log(this + ": game desynched: less than 2 players synched!");
      }
    }
  }

  @Override
  public void addData(KailleraUser user, int playerNumber, byte[] data) throws GameDataException {
    if (playerActionQueues == null) return;

    // int bytesPerAction = (data.length / actionsPerMessage);
    int timeoutCounter = 0;
    // int arraySize = (playerActionQueues.length * actionsPerMessage * user.getBytesPerAction());

    if (!synched) {
      throw new GameDataException(
          EmuLang.getString("KailleraGameImpl.DesynchedWarning"),
          data,
          actionsPerMessage,
          playerNumber,
          playerActionQueues.length);
    }

    playerActionQueues[(playerNumber - 1)].addActions(data);

    if (autoFireDetector != null)
      autoFireDetector.addData(playerNumber, data, user.getBytesPerAction());

    byte[] response = new byte[user.getArraySize()];
    for (int actionCounter = 0; actionCounter < actionsPerMessage; actionCounter++) {
      for (int playerCounter = 0; playerCounter < playerActionQueues.length; playerCounter++) {
        while (synched) {
          try {
            playerActionQueues[playerCounter].getAction(
                playerNumber,
                response,
                ((actionCounter * (playerActionQueues.length * user.getBytesPerAction()))
                    + (playerCounter * user.getBytesPerAction())),
                user.getBytesPerAction());
            break;
          } catch (PlayerTimeoutException e) {
            e.setTimeoutNumber(++timeoutCounter);
            handleTimeout(e);
          }
        }
      }
    }

    if (!synched)
      throw new GameDataException(
          EmuLang.getString("KailleraGameImpl.DesynchedWarning"),
          data,
          user.getBytesPerAction(),
          playerNumber,
          playerActionQueues.length);

    ((KailleraUserImpl) user).addEvent(new GameDataEvent(this, response));
  }

  // it's very important this method is synchronized
  private synchronized void handleTimeout(PlayerTimeoutException e) {
    if (!synched) return;

    int playerNumber = e.getPlayerNumber();
    int timeoutNumber = e.getTimeoutNumber();
    PlayerActionQueue playerActionQueue = playerActionQueues[(playerNumber - 1)];

    if (!playerActionQueue.isSynched() || e.equals(playerActionQueue.getLastTimeout())) return;

    playerActionQueue.setLastTimeout(e);

    KailleraUser player = e.getPlayer();
    if (timeoutNumber < desynchTimeouts) {
      if (startTimeout) player.setTimeouts(player.getTimeouts() + 1);

      if (timeoutNumber % 12 == 0) {
        logger.atInfo().log(this + ": " + player + ": Timeout #" + timeoutNumber / 12);
        addEvent(new GameTimeoutEvent(this, player, timeoutNumber / 12));
      }
    } else {
      logger.atInfo().log(this + ": " + player + ": Timeout #" + timeoutNumber / 12);
      playerActionQueue.setSynched(false);
      logger.atInfo().log(this + ": " + player + ": player desynched: Lagged!");
      addEvent(
          new PlayerDesynchEvent(
              this,
              player,
              EmuLang.getString("KailleraGameImpl.DesynchDetectedPlayerLagged", player.getName())));

      if (getSynchedCount() < 2) {
        synched = false;
        for (PlayerActionQueue q : playerActionQueues) q.setSynched(false);
        logger.atInfo().log(this + ": game desynched: less than 2 players synched!");
      }
    }
  }
}

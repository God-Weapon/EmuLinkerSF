package org.emulinker.kaillera.model.impl;

import java.util.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.connectcontroller.protocol.ConnectMessage_HELLOD00D;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.protocol.ServerACK;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.*;

public final class KailleraGameImpl implements KailleraGame
{
	private static Log	log							= LogFactory.getLog(KailleraGameImpl.class);

	private int						id;
	private String					romName;
	private String					toString;
	private Date					startDate;
	
	private String                  lastAddress = "null";
	private int						lastAddressCount = 0;
	 
	private static int              chatFloodTime = 3;
	private int                     maxUsers = 8;
	private int                     delay;
	private String                  aEmulator = "any";
	private String                  aConnection = "any";
	private int						maxPing = 1000;
	private int                     startN = -1;
	private boolean                 p2P = false;
	private boolean					sameDelay = false;
	public boolean					swap = false;
		
	private int						highestPing = 0;
	private int						bufferSize;
	private boolean                 startTimeout = false;
	private long                    startTimeoutTime;
	private int 					timeoutMillis;
	private int						desynchTimeouts;

	private KailleraServerImpl		server;
	private KailleraUserImpl		owner;
	private List<KailleraUserImpl>	players				= new CopyOnWriteArrayList<KailleraUserImpl>();
	private StatsCollector			statsCollector;

	private List<String>			kickedUsers			= new ArrayList<String>();
	private List<String>			mutedUsers			= new ArrayList<String>();

	private int						status				= KailleraGame.STATUS_WAITING;
	private boolean					synched				= false;
	private int						actionsPerMessage;
	private PlayerActionQueue[]		playerActionQueues;
	private AutoFireDetector		autoFireDetector;
	
	public KailleraGameImpl(int gameID, String romName, KailleraUserImpl owner, KailleraServerImpl server, int bufferSize, int timeoutMillis, int desynchTimeouts)
	{
		this.id = gameID;
		this.romName = romName;
		this.owner = owner;
		this.server = server;
		this.actionsPerMessage = owner.getConnectionType();
		this.bufferSize = bufferSize;
		this.timeoutMillis = 100;//timeoutMillis;
		this.desynchTimeouts = 120;//desynchTimeouts;
		
		toString = "Game" + id + "(" + (romName.length() > 15 ? (romName.substring(0, 15) + "...") : romName) + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		startDate = new Date();

		statsCollector = server.getStatsCollector();
		autoFireDetector = server.getAutoFireDetector(this);
	}

	public int getID()
	{
		return id;
	}
	
	public List<String> getMutedUsers() {
		return mutedUsers;
	}
	
	public int getDelay(){
		return delay;
	}
	
	public String getAEmulator(){
		return aEmulator;
	}
	public void setAEmulator(String aEmulator){
		this.aEmulator = aEmulator;
	}
	
	public String getAConnection(){
		return aConnection;
	}
	public void setAConnection(String aConnection){
		this.aConnection = aConnection;
	}
	
	public PlayerActionQueue[] getPlayerActionQueue(){
		return playerActionQueues;
	}
	
	public void setDelay(int delay){
		this.delay = delay;
	}
	
	public void setStartTimeout(boolean startTimeout){
		this.startTimeout = startTimeout;
	}
	
	public boolean getStartTimeout(){
		return startTimeout;
	}
		
	
	public void setSameDelay(boolean sameDelay){
		this.sameDelay = sameDelay;
	}
	
	public boolean getSameDelay(){
		return sameDelay;
	}
	
	public long getStartTimeoutTime(){
		return startTimeoutTime;
	}
	
	public int getStartN(){
		return startN;
	}
	
	
	public boolean getP2P(){
		return p2P;
	}
	
	public void setP2P(boolean p2P){
		this.p2P = p2P;
	}
	
	public void setStartN(int startN){
		this.startN = startN;
	}
	
	public String getRomName()
	{
		return romName;
	}
	
	public Date getStartDate()
	{
		return startDate;
	}
		
	public void setMaxUsers(int maxUsers){
		this.maxUsers = maxUsers;
		server.addEvent(new GameStatusChangedEvent(server, this));
	}
	
	public int getMaxUsers(){
		return maxUsers;
	}
	
	public int getHighestPing(){
		return highestPing;
	}
	
	public void setMaxPing(int maxPing){
		this.maxPing = maxPing;
	}
	
	public int getMaxPing(){
		return maxPing;
	}

	public KailleraUser getOwner()
	{
		return owner;
	}

	public int getPlayerNumber(KailleraUser user)
	{
		return (players.indexOf(user) + 1);
	}

	public KailleraUser getPlayer(int playerNumber)
	{
		if (playerNumber > players.size())
		{
			log.error(this + ": getPlayer(" + playerNumber + ") failed! (size = " + players.size() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return null;
		}

		return players.get((playerNumber - 1));
	}

	public int getNumPlayers()
	{
		return players.size();
	}

	public List<KailleraUserImpl> getPlayers()
	{
		return players;
	}

	public int getStatus()
	{
		return status;
	}

	public boolean isSynched()
	{
		return synched;
	}

	public KailleraServerImpl getServer()
	{
		return server;
	}

	void setStatus(int status)
	{
		this.status = status;
		server.addEvent(new GameStatusChangedEvent(server, this));
	}

	public String getClientType()
	{
		return getOwner().getClientType();
	}

	public String toString()
	{
		return toString;
	}

	public String toDetailedString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("KailleraGame[id="); //$NON-NLS-1$
		sb.append(getID());
		sb.append(" romName="); //$NON-NLS-1$
		sb.append(getRomName());
		sb.append(" owner="); //$NON-NLS-1$
		sb.append(getOwner());
		sb.append(" numPlayers="); //$NON-NLS-1$
		sb.append(getNumPlayers());
		sb.append(" status="); //$NON-NLS-1$
		sb.append(KailleraGame.STATUS_NAMES[getStatus()]);
		sb.append("]"); //$NON-NLS-1$
		return sb.toString();
	}

	int getPlayingCount()
	{
		int count = 0;
		for (KailleraUserImpl player : players)
		{
			if (player.getStatus() == KailleraUser.STATUS_PLAYING)
				count++;
		}

		return count;
	}

	int getSynchedCount()
	{
		if (playerActionQueues == null)
			return 0;

		int count = 0;
		for (int i = 0; i < playerActionQueues.length; i++)
		{
			if (playerActionQueues[i].isSynched())
				count++;
		}

		return count;

		// return dataQueues.size();
		//		return readyCount;
	}

	void addEvent(GameEvent event)
	{
		for (KailleraUserImpl player : players)
			player.addEvent(event);
	}

	public AutoFireDetector getAutoFireDetector()
	{
		return autoFireDetector;
	}

	public synchronized void chat(KailleraUser user, String message) throws GameChatException
	{
		if (!players.contains(user))
		{
			log.warn(user + " game chat denied: not in " + this); //$NON-NLS-1$
			throw new GameChatException(EmuLang.getString("KailleraGameImpl.GameChatErrorNotInGame")); //$NON-NLS-1$
		}
		
		if (user.getAccess() == AccessManager.ACCESS_NORMAL)
		{
			if (server.getMaxGameChatLength() > 0 && message.length() > server.getMaxGameChatLength())
			{
				log.warn(user + " gamechat denied: Message Length > " + server.getMaxGameChatLength());
				addEvent(new GameInfoEvent(this, EmuLang.getString("KailleraGameImpl.GameChatDeniedMessageTooLong"), user));
				throw new GameChatException(EmuLang.getString("KailleraGameImpl.GameChatDeniedMessageTooLong"));
			}
		}

		log.info(user + ", " + this + " gamechat: " + message); //$NON-NLS-1$ //$NON-NLS-2$
		addEvent(new GameChatEvent(this, user, message));
	}

	public synchronized void announce(String announcement, KailleraUser user)
	{
		addEvent(new GameInfoEvent(this, announcement, user));
	}

	public synchronized void kick(KailleraUser user, int userID) throws GameKickException
	{
		if(user.getAccess() < AccessManager.ACCESS_ADMIN){
			if (!user.equals(getOwner()))
			{
				log.warn(user + " kick denied: not the owner of " + this); //$NON-NLS-1$
				throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickDeniedNotGameOwner")); //$NON-NLS-1$
			}
		}

		if (user.getID() == userID)
		{
			log.warn(user + " kick denied: attempt to kick self"); //$NON-NLS-1$
			throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickDeniedCannotKickSelf")); //$NON-NLS-1$
		}
		

		for (KailleraUserImpl player : players)
		{
			if (player.getID() == userID)
			{
				try
				{
					if(user.getAccess() != AccessManager.ACCESS_SUPERADMIN){
						if(player.getAccess() >= AccessManager.ACCESS_ADMIN){
							return;
						}
					}
					
					log.info(user + " kicked: " + userID + " from " + this); //$NON-NLS-1$ //$NON-NLS-2$
					//SF MOD - Changed to IP rather than ID
					kickedUsers.add(player.getConnectSocketAddress().getAddress().getHostAddress());
					player.quitGame();
					return;
				}
				catch (Exception e)
				{
					// this shouldn't happen
					log.error("Caught exception while making user quit game! This shouldn't happen!", e); //$NON-NLS-1$
				}
			}
		}

		log.warn(user + " kick failed: user " + userID + " not found in: " + this); //$NON-NLS-1$ //$NON-NLS-2$
		throw new GameKickException(EmuLang.getString("KailleraGameImpl.GameKickErrorUserNotFound")); //$NON-NLS-1$
	}

	public synchronized int join(KailleraUser user) throws JoinGameException
	{
		
		int access = server.getAccessManager().getAccess(user.getSocketAddress().getAddress());

		//SF MOD - Join room spam protection
		if(lastAddress.equals(user.getConnectSocketAddress().getAddress().getHostAddress())){
			lastAddressCount++;
			if(lastAddressCount >= 4){
				log.info(user + " join spam protection: " + user.getID() + " from " + this); //$NON-NLS-1$ //$NON-NLS-2$
				//SF MOD - Changed to IP rather than ID
				if(access < AccessManager.ACCESS_ADMIN){
					kickedUsers.add(user.getConnectSocketAddress().getAddress().getHostAddress());
					try { user.quitGame(); } catch(Exception e) {}
					throw new JoinGameException("Spam Protection"); //$NON-NLS-1$	
				}
			}
		}
		else{
			lastAddressCount = 0;
			lastAddress = user.getConnectSocketAddress().getAddress().getHostAddress();
		}
		
		if (players.contains(user))
		{
			log.warn(user + " join game denied: already in " + this); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameErrorAlreadyInGame")); //$NON-NLS-1$
		}

		if(access < AccessManager.ACCESS_ELEVATED && getNumPlayers() >=  getMaxUsers())
		{
			log.warn(user + " join game denied: max users reached " + this); //$NON-NLS-1$
			throw new JoinGameException("This room's user capacity has been reached."); //$NON-NLS-1$		
		}
		
		if(access < AccessManager.ACCESS_ELEVATED && user.getPing() > getMaxPing())
		{
			log.warn(user + " join game denied: max ping reached " + this); //$NON-NLS-1$
			throw new JoinGameException("Your ping is too high for this room."); //$NON-NLS-1$		
		}

		if(access < AccessManager.ACCESS_ELEVATED && !aEmulator.equals("any"))
		{
			if(!aEmulator.equals(user.getClientType())){
				log.warn(user + " join game denied: owner doesn't allow that emulator: " + user.getClientType()); //$NON-NLS-1$
				throw new JoinGameException("Owner only allows emulator version: " + aEmulator); //$NON-NLS-1$		
			}
		}
		
		if(access < AccessManager.ACCESS_ELEVATED && !aConnection.equals("any"))
		{
			if (user.getConnectionType() != getOwner().getConnectionType())
			{
				log.warn(user + "join game denied: owner doesn't allow that connection type: " + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]); //$NON-NLS-1$
				throw new JoinGameException("Owner only allows connection type: " + KailleraUser.CONNECTION_TYPE_NAMES[getOwner().getConnectionType()]); //$NON-NLS-1$
			}
		}		

		if (access < AccessManager.ACCESS_ADMIN && kickedUsers.contains(user.getConnectSocketAddress().getAddress().getHostAddress()))
		{
			log.warn(user + " join game denied: previously kicked: " + this); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameDeniedPreviouslyKicked")); //$NON-NLS-1$
		}

		if (access == AccessManager.ACCESS_NORMAL && getStatus() != KailleraGame.STATUS_WAITING)
		{
			log.warn(user + " join game denied: attempt to join game in progress: " + this); //$NON-NLS-1$
			throw new JoinGameException(EmuLang.getString("KailleraGameImpl.JoinGameDeniedGameIsInProgress")); //$NON-NLS-1$
		}
		
		if (mutedUsers.contains(user.getConnectSocketAddress().getAddress().getHostAddress()))
		{
			user.setMute(true);
		}

		players.add((KailleraUserImpl) user);
		user.setPlayerNumber(players.size());
		
		server.addEvent(new GameStatusChangedEvent(server, this));
		
		log.info(user + " joined: " + this); //$NON-NLS-1$
		addEvent(new UserJoinedGameEvent(this, user));
		
		//SF MOD - /startn
		if(getStartN() != -1){
			if(players.size() >= getStartN()){
				try { Thread.sleep(1000); } catch(Exception e) {}
				try { start(getOwner()); } catch(Exception e) {}
			}
		}

		//if(user.equals(owner))
		//{

			announce("Help: " + getServer().getReleaseInfo().getProductName() + " v" + getServer().getReleaseInfo().getVersionString() + ": " + getServer().getReleaseInfo().getReleaseDate() + " - Visit: www.EmuLinker.org", user);
			announce("************************", user);
			announce("Type /p2pon to ignore ALL server activity during gameplay.", user);
			announce("This will reduce lag that you contribute due to a busy server.", user);
			announce("If server is greater than 60 users, option is auto set.", user);
			announce("************************", user);
			/*
			if(autoFireDetector != null)
			{
				if(autoFireDetector.getSensitivity() > 0)
				{
					announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOn")); //$NON-NLS-1$
					announce(EmuLang.getString("KailleraGameImpl.AutofireCurrentSensitivity", autoFireDetector.getSensitivity())); //$NON-NLS-1$
				}
				else
				{
					announce(EmuLang.getString("KailleraGameImpl.AutofireDetectionOff")); //$NON-NLS-1$
				}
				announce(EmuLang.getString("KailleraGameImpl.GameHelp")); //$NON-NLS-1$
			}
			*/
		//}
			
		//new SF MOD - different emulator versions notifications
		if (access < AccessManager.ACCESS_ADMIN && !user.getClientType().equals(owner.getClientType()) && !owner.getGame().getRomName().startsWith("*"))
			addEvent(new GameInfoEvent(this, user.getName() + " using different emulator version: " + user.getClientType(), null));

		return (players.indexOf(user) + 1);
	}

	public synchronized void start(KailleraUser user) throws StartGameException
	{
		
		int access = server.getAccessManager().getAccess(user.getSocketAddress().getAddress());

		if (!user.equals(getOwner()) && access < AccessManager.ACCESS_ADMIN)
		{
			log.warn(user + " start game denied: not the owner of " + this); //$NON-NLS-1$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedOnlyOwnerMayStart")); //$NON-NLS-1$
		}

		if (status == KailleraGame.STATUS_SYNCHRONIZING)
		{
			log.warn(user + " start game failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]); //$NON-NLS-1$ //$NON-NLS-2$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameErrorSynchronizing")); //$NON-NLS-1$
		}
		else if (status == KailleraGame.STATUS_PLAYING)
		{
			log.warn(user + " start game failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]); //$NON-NLS-1$ //$NON-NLS-2$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameErrorStatusIsPlaying")); //$NON-NLS-1$
		}

		if (access == AccessManager.ACCESS_NORMAL && getNumPlayers() < 2 && !server.getAllowSinglePlayer())
		{
			log.warn(user + " start game denied: " + this + " needs at least 2 players"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedSinglePlayerNotAllowed")); //$NON-NLS-1$
		}
		
		// do not start if not game
		if(owner.getGame().getRomName().startsWith("*"))
			return;

		for (KailleraUser player : players)
		{
			if(player.getStealth() == false){
				if (player.getConnectionType() != owner.getConnectionType())
				{
					log.warn(user + " start game denied: " + this + ": All players must use the same connection type"); //$NON-NLS-1$ //$NON-NLS-2$
					addEvent(new GameInfoEvent(this, EmuLang.getString("KailleraGameImpl.StartGameConnectionTypeMismatchInfo", KailleraUser.CONNECTION_TYPE_NAMES[owner.getConnectionType()]), null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedConnectionTypeMismatch")); //$NON-NLS-1$
				}

				if (!player.getClientType().equals(getClientType()))
				{
					log.warn(user + " start game denied: " + this + ": All players must use the same emulator!"); //$NON-NLS-1$ //$NON-NLS-2$
					addEvent(new GameInfoEvent(this, EmuLang.getString("KailleraGameImpl.StartGameEmulatorMismatchInfo", getClientType()), null)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					throw new StartGameException(EmuLang.getString("KailleraGameImpl.StartGameDeniedEmulatorMismatch")); //$NON-NLS-1$
				}
			}
		}

		log.info(user + " started: " + this); //$NON-NLS-1$
		setStatus(KailleraGame.STATUS_SYNCHRONIZING);

		if(autoFireDetector != null)
			autoFireDetector.start(players.size());

		playerActionQueues = new PlayerActionQueue[players.size()];
						
		startTimeout = false;
		delay = 1;
		
		if(server.getUsers().size() > 60){
			p2P = true;
		}

		for (int i = 0; i < playerActionQueues.length && i < players.size(); i++)
		{
			KailleraUserImpl player = players.get(i);
			int playerNumber = (i + 1);			
			
			if(!swap) player.setPlayerNumber(playerNumber);
			player.setTimeouts(0);
			player.setFrameCount(0);
									
			playerActionQueues[i] = new PlayerActionQueue(playerNumber, player, getNumPlayers(), bufferSize, timeoutMillis, true);
			//playerActionQueues[i] = new PlayerActionQueue(playerNumber, player, getNumPlayers(), GAME_BUFFER_SIZE, (player.getPing()*3));
			//SF MOD - player.setPlayerNumber(playerNumber);
			//SF MOD - Delay Value = [(60/connectionType) * (ping/1000)] + 1

			double delayVal = ((60/player.getConnectionType()) * ((double)player.getPing()/1000)) + 1;
						
			player.setDelay((int)delayVal);	
			if((int)delayVal > delay){
				delay = (int)delayVal;
			}
			
			if(player.getPing() > highestPing){
				highestPing = user.getPing();
			}
			
		
			if(p2P){
				player.setP2P(true);
				announce("This game is ignoring ALL server activity during gameplay!", player);
			}
			/*else{
				player.setP2P(false);
			}*/
			
			log.info(this + ": " + player + " is player number " + playerNumber); //$NON-NLS-1$ //$NON-NLS-2$
			
			if(autoFireDetector != null)
				autoFireDetector.addPlayer(player, playerNumber);
		}

		if (statsCollector != null)
			statsCollector.gameStarted(server, this);
		
		/*if(user.getConnectionType() > KailleraUser.CONNECTION_TYPE_GOOD || user.getConnectionType() < KailleraUser.CONNECTION_TYPE_GOOD){
			//sameDelay = true;
		}*/

		//timeoutMillis = highestPing;
		addEvent(new GameStartedEvent(this));
	}

	public synchronized void ready(KailleraUser user, int playerNumber) throws UserReadyException
	{
		if (!players.contains(user))
		{
			log.warn(user + " ready game failed: not in " + this); //$NON-NLS-1$
			throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorNotInGame")); //$NON-NLS-1$
		}

		if (status != KailleraGame.STATUS_SYNCHRONIZING)
		{
			log.warn(user + " ready failed: " + this + " status is " + KailleraGame.STATUS_NAMES[status]); //$NON-NLS-1$ //$NON-NLS-2$
			throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorIncorrectState")); //$NON-NLS-1$
		}

		if (playerActionQueues == null)
		{
			log.error(user + " ready failed: " + this + " playerActionQueues == null!"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new UserReadyException(EmuLang.getString("KailleraGameImpl.ReadyGameErrorInternalError")); //$NON-NLS-1$
		}

		log.info(user + " (player " + playerNumber + ") is ready to play: " + this); //$NON-NLS-1$ //$NON-NLS-2$
		playerActionQueues[(playerNumber - 1)].setSynched(true);

		if (getSynchedCount() == getNumPlayers())
		{
			log.info(this + " all players are ready: starting..."); //$NON-NLS-1$

			setStatus(KailleraGame.STATUS_PLAYING);
			synched = true;
			startTimeoutTime = System.currentTimeMillis();
			addEvent(new AllReadyEvent(this));
			
			int frameDelay = ((delay + 1) * owner.getConnectionType()) - 1;
			if(sameDelay){
				announce("This game's delay is: " + delay + " (" + frameDelay + " frame delay)", null);
			}
			else{
				for (int i = 0; i < playerActionQueues.length && i < players.size(); i++){
					KailleraUserImpl player = players.get(i);
					// do not show delay if stealth mode
					if(player != null && !player.getStealth()){
						frameDelay = ((player.getDelay() + 1) * player.getConnectionType()) - 1;
						announce("P" + (i + 1) + " Delay = " + player.getDelay() + " (" + frameDelay + " frame delay)", null);
					}
				}
			}
		}
		
	}

	public synchronized void drop(KailleraUser user, int playerNumber) throws DropGameException
	{
		if (!players.contains(user))
		{
			log.warn(user + " drop game failed: not in " + this); //$NON-NLS-1$
			throw new DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorNotInGame")); //$NON-NLS-1$
		}

		if (playerActionQueues == null)
		{
			log.error(user + " drop failed: " + this + " playerActionQueues == null!"); //$NON-NLS-1$ //$NON-NLS-2$
			throw new DropGameException(EmuLang.getString("KailleraGameImpl.DropGameErrorInternalError")); //$NON-NLS-1$
		}

		log.info(user + " dropped: " + this); //$NON-NLS-1$
		
		if((playerNumber - 1) < playerActionQueues.length)
			playerActionQueues[(playerNumber - 1)].setSynched(false);

		if (getSynchedCount() < 2 && synched)
		{
			synched = false;
			for (PlayerActionQueue q : playerActionQueues)
				q.setSynched(false);
			log.info(this + ": game desynched: less than 2 players playing!"); //$NON-NLS-1$
		}

		if(autoFireDetector != null)
			autoFireDetector.stop(playerNumber);

		if (getPlayingCount() == 0){
			if(getStartN() != -1){
				setStartN(-1);
				announce("StartN is now off.", null);
			}
			setStatus(KailleraGame.STATUS_WAITING);
		}
		addEvent(new UserDroppedGameEvent(this, user, playerNumber));
		
		if(user.getP2P()){
			//KailleraUserImpl u = (KailleraUserImpl) user;
			//u.addEvent(new ServerACK(.getNextMessageNumber());
			//u.addEvent(new ConnectedEvent(server, user));
			//u.addEvent(new UserQuitEvent(server, user, "Rejoining..."));
			//try{user.quit("Rejoining...");}catch(Exception e){}
			announce("Rejoin server to update client of ignored server activity!", user);
		}
	}

	public void quit(KailleraUser user, int playerNumber) throws DropGameException, QuitGameException, CloseGameException
	{
		synchronized(this)
		{
			if (!players.remove(user))
			{
				log.warn(user + " quit game failed: not in " + this); //$NON-NLS-1$
				throw new QuitGameException(EmuLang.getString("KailleraGameImpl.QuitGameErrorNotInGame")); //$NON-NLS-1$
			}
	
			log.info(user + " quit: " + this); //$NON-NLS-1$
	
			addEvent(new UserQuitGameEvent(this, user));
			
			user.setP2P(false);
			
			swap = false;
			
			if(status == STATUS_WAITING){
				for(int i = 0; i < this.getNumPlayers(); i++){
					getPlayer(i + 1).setPlayerNumber(i + 1);
					log.debug(getPlayer(i + 1).getName() + ":::" + getPlayer(i + 1).getPlayerNumber());
				}
			}
		}

		if (user.equals(owner))
			server.closeGame(this, user);
		else
			server.addEvent(new GameStatusChangedEvent(server, this));
	}

	synchronized void close(KailleraUser user) throws CloseGameException
	{
		if (!user.equals(owner))
		{
			log.warn(user + " close game denied: not the owner of " + this); //$NON-NLS-1$
			throw new CloseGameException(EmuLang.getString("KailleraGameImpl.CloseGameErrorNotGameOwner")); //$NON-NLS-1$
		}

		if (synched)
		{
			synched = false;
			for (PlayerActionQueue q : playerActionQueues)
				q.setSynched(false);
			log.info(this + ": game desynched: game closed!"); //$NON-NLS-1$
		}
		

		for (KailleraUserImpl player : players){
			player.setStatus(KailleraUser.STATUS_IDLE);
			player.setMute(false);
			player.setP2P(false);
			player.setGame(null);
		}
		
		if(autoFireDetector != null)
			autoFireDetector.stop();
		
		players.clear();
	}

	public synchronized void droppedPacket(KailleraUser user)
	{
		if(!synched)
			return;

		int playerNumber = user.getPlayerNumber();
		if(user.getPlayerNumber() > playerActionQueues.length){
			log.info(this + ": " + user + ": player desynched: dropped a packet! Also left the game already: KailleraGameImpl -> DroppedPacket"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		if (playerActionQueues != null && playerActionQueues[(playerNumber - 1)].isSynched())
		{
			playerActionQueues[(playerNumber - 1)].setSynched(false);
			log.info(this + ": " + user + ": player desynched: dropped a packet!"); //$NON-NLS-1$ //$NON-NLS-2$
			addEvent(new PlayerDesynchEvent(this, user, EmuLang.getString("KailleraGameImpl.DesynchDetectedDroppedPacket", user.getName()))); //$NON-NLS-1$

			if (getSynchedCount() < 2 && synched)
			{
				synched = false;
				for (PlayerActionQueue q : playerActionQueues)
					q.setSynched(false);
				log.info(this + ": game desynched: less than 2 players synched!"); //$NON-NLS-1$
			}
		}
	}
	


	public void addData(KailleraUser user, int playerNumber, byte[] data) throws GameDataException
	{
		if(playerActionQueues == null)
			return;
		
		//int bytesPerAction = (data.length / actionsPerMessage);
		int timeoutCounter = 0;
		//int arraySize = (playerActionQueues.length * actionsPerMessage * user.getBytesPerAction());
		
		if (!synched)
		{
			throw new GameDataException(EmuLang.getString("KailleraGameImpl.DesynchedWarning"), data, actionsPerMessage, playerNumber, playerActionQueues.length); //$NON-NLS-1$
		}
		

		playerActionQueues[(playerNumber - 1)].addActions(data);	
			
		
		if(autoFireDetector != null)
			autoFireDetector.addData(playerNumber, data, user.getBytesPerAction());	

		byte[] response = new byte[user.getArraySize()];
		for(int actionCounter = 0; actionCounter < actionsPerMessage; actionCounter++)
		{
			for(int playerCounter = 0; playerCounter < playerActionQueues.length; playerCounter++)
			{
				while (synched)
				{
					try
					{
						playerActionQueues[playerCounter].getAction(playerNumber, response, ((actionCounter * (playerActionQueues.length * user.getBytesPerAction())) + (playerCounter * user.getBytesPerAction())), user.getBytesPerAction());
						break;
					}
					catch (PlayerTimeoutException e)
					{
						e.setTimeoutNumber(++timeoutCounter);
						handleTimeout(e);
					}
				}
			}
		}

		if (!synched)
			throw new GameDataException(EmuLang.getString("KailleraGameImpl.DesynchedWarning"), data, user.getBytesPerAction(), playerNumber, playerActionQueues.length); //$NON-NLS-1$
				
		((KailleraUserImpl) user).addEvent(new GameDataEvent(this, response));
	}

	// it's very important this method is synchronized
	private synchronized void handleTimeout(PlayerTimeoutException e)
	{
		if (!synched)
			return;

		int playerNumber = e.getPlayerNumber();
		int timeoutNumber = e.getTimeoutNumber();
		PlayerActionQueue playerActionQueue = playerActionQueues[(playerNumber - 1)];

		if (!playerActionQueue.isSynched() || e.equals(playerActionQueue.getLastTimeout()))
			return;

		playerActionQueue.setLastTimeout(e);

		KailleraUser player = e.getPlayer();
		if (timeoutNumber < desynchTimeouts)
		{		
			if(startTimeout)
				player.setTimeouts(player.getTimeouts() + 1);
			
			if(timeoutNumber % 12 == 0){
				log.info(this + ": " + player + ": Timeout #" + timeoutNumber/12); //$NON-NLS-1$ //$NON-NLS-2$
				addEvent(new GameTimeoutEvent(this, player, timeoutNumber/12));
			}	
		}
		else
		{
			log.info(this + ": " + player + ": Timeout #" + timeoutNumber/12); //$NON-NLS-1$ //$NON-NLS-2$
			playerActionQueue.setSynched(false);
			log.info(this + ": " + player + ": player desynched: Lagged!"); //$NON-NLS-1$ //$NON-NLS-2$
			addEvent(new PlayerDesynchEvent(this, player, EmuLang.getString("KailleraGameImpl.DesynchDetectedPlayerLagged", player.getName()))); //$NON-NLS-1$

			if (getSynchedCount() < 2)
			{
				synched = false;
				for (PlayerActionQueue q : playerActionQueues)
					q.setSynched(false);
				log.info(this + ": game desynched: less than 2 players synched!"); //$NON-NLS-1$
			}
		}
	}
}

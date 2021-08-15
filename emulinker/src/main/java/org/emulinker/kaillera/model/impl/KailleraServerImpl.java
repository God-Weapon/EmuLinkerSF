package org.emulinker.kaillera.model.impl;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.text.*;

import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;
import org.emulinker.release.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.util.*;

public class KailleraServerImpl implements KailleraServer, Executable
{
	protected static Log						log							= LogFactory.getLog(KailleraServerImpl.class);

	protected int								maxPing;
	protected int								maxUsers;
	protected int								maxGames;
	protected int								idleTimeout;
	protected int								keepAliveTimeout;
	protected int								chatFloodTime;
	protected int								createGameFloodTime;
	protected int								maxUserNameLength;
	protected int								maxChatLength;
	protected int								maxGameChatLength;
	protected int								maxGameNameLength;
	protected int								maxQuitMessageLength;
	protected int								maxClientNameLength;
	
	protected int								gameBufferSize;
	protected int								gameTimeoutMillis;
	protected int								gameDesynchTimeouts;
	protected int								gameAutoFireSensitivity;

	protected boolean[]							allowedConnectionTypes		= new boolean[7];

	protected List<String>						loginMessages				= new ArrayList<String>();
	protected boolean							allowSinglePlayer			= false;
	protected boolean							allowMultipleConnections	= false;

	protected boolean							stopFlag					= false;
	protected boolean							isRunning					= false;

	protected int								connectionCounter			= 1;
	protected int								gameCounter					= 1;

	protected ThreadPoolExecutor				threadPool					= null;
	protected AccessManager						accessManager;
	protected StatsCollector					statsCollector;
	protected ReleaseInfo						releaseInfo;
	protected AutoFireDetectorFactory 			autoFireDetectorFactory;

	protected Map<Integer, KailleraUserImpl>	users;
	protected Map<Integer, KailleraGameImpl>	games;

	private Trivia trivia = null;
	private Thread triviaThread;
	private boolean switchTrivia = false;
	
	public KailleraServerImpl(ThreadPoolExecutor threadPool, AccessManager accessManager, Configuration config, StatsCollector statsCollector, ReleaseInfo releaseInfo, AutoFireDetectorFactory autoFireDetectorFactory) throws NoSuchElementException, ConfigurationException
	{
		this.threadPool = threadPool;
		this.accessManager = accessManager;
		this.releaseInfo = releaseInfo;
		this.autoFireDetectorFactory = autoFireDetectorFactory;

		maxPing = config.getInt("server.maxPing");
		maxUsers = config.getInt("server.maxUsers");
		maxGames = config.getInt("server.maxGames");
		keepAliveTimeout = config.getInt("server.keepAliveTimeout");
		idleTimeout = config.getInt("server.idleTimeout");
		chatFloodTime = config.getInt("server.chatFloodTime");
		createGameFloodTime = config.getInt("server.createGameFloodTime");
		allowSinglePlayer = config.getBoolean("server.allowSinglePlayer");
		allowMultipleConnections = config.getBoolean("server.allowMultipleConnections");
		maxUserNameLength = config.getInt("server.maxUserNameLength");
		if(maxUserNameLength > 31)
			maxUserNameLength = 31;
		maxChatLength = config.getInt("server.maxChatLength");
		maxGameChatLength = config.getInt("server.maxGameChatLength");
		maxGameNameLength = config.getInt("server.maxGameNameLength");
		if(maxGameNameLength > 127)
			maxGameNameLength = 127;
		maxQuitMessageLength = config.getInt("server.maxQuitMessageLength");
		maxClientNameLength = config.getInt("server.maxClientNameLength");
		if(maxClientNameLength > 127)
			maxClientNameLength = 127;
		
		for(int i=1; i<=999; i++)
		{
			if(EmuLang.hasString("KailleraServerImpl.LoginMessage."+i))
				loginMessages.add(EmuLang.getString("KailleraServerImpl.LoginMessage."+i));
			else
				break;
		}
		
		gameBufferSize = config.getInt("game.bufferSize");
		if (gameBufferSize <= 0)
			throw new ConfigurationException("game.bufferSize can not be <= 0");

		gameTimeoutMillis = config.getInt("game.timeoutMillis");
		if (gameTimeoutMillis <= 0)
			throw new ConfigurationException("game.timeoutMillis can not be <= 0");

		gameDesynchTimeouts = config.getInt("game.desynchTimeouts");
		
		gameAutoFireSensitivity = config.getInt("game.defaultAutoFireSensitivity");
		if(gameAutoFireSensitivity < 0 || gameAutoFireSensitivity > 5)
			throw new ConfigurationException("game.defaultAutoFireSensitivity must be 0-5");
		
		List<String> connectionTypes = config.getList("server.allowedConnectionTypes");
		for (String s : connectionTypes)
		{
			try
			{
				int ct = Integer.parseInt(s);
				if (ct < 1 || ct > 6)
					throw new ConfigurationException("Invalid connectionType: " + s);
				allowedConnectionTypes[ct] = true;
			}
			catch (NumberFormatException e)
			{
				throw new ConfigurationException("Invalid connectionType: " + s);
			}
		}

		if (maxPing <= 0)
			throw new ConfigurationException("server.maxPing can not be <= 0");

		if (maxPing > 1000)
			throw new ConfigurationException("server.maxPing can not be > 1000");

		if (keepAliveTimeout <= 0)
			throw new ConfigurationException("server.keepAliveTimeout must be > 0 (190 is recommended)");

		users = new ConcurrentHashMap<Integer, KailleraUserImpl>(maxUsers);
		games = new ConcurrentHashMap<Integer, KailleraGameImpl>(maxGames);

		boolean touchKaillera = config.getBoolean("masterList.touchKaillera", false);

		if (touchKaillera)
			this.statsCollector = statsCollector;
	}

	public void setTrivia(Trivia trivia){
		this.trivia = trivia;
	}
	public void setTriviaThread(Thread triviaThread){
		this.triviaThread = triviaThread;
	}
	public void setSwitchTrivia(boolean switchTrivia){
		this.switchTrivia = switchTrivia;
	}
	public Trivia getTrivia(){
		return trivia;
	}
	public Thread getTriviaThread(){
		return triviaThread;
	}
	public boolean getSwitchTrivia(){
		return switchTrivia;
	}
	
	public AccessManager getAccessManager()
	{
		return accessManager;
	}

	public KailleraUser getUser(int userID)
	{
		return users.get(userID);
	}

	public KailleraGame getGame(int gameID)
	{
		return games.get(gameID);
	}

	public Collection<KailleraUserImpl> getUsers()
	{
		return users.values();
	}

	public Collection<KailleraGameImpl> getGames()
	{
		return games.values();
	}

	public int getNumUsers()
	{
		return users.size();
	}

	public int getNumGames()
	{
		return games.size();
	}

	public int getNumGamesPlaying()
	{
		int count = 0;
		for (KailleraGameImpl game : getGames())
		{
			if (game.getStatus() != KailleraGame.STATUS_WAITING)
				count++;
		}
		return count;
	}

	public int getMaxPing()
	{
		return maxPing;
	}

	public int getMaxUsers()
	{
		return maxUsers;
	}


	public int getMaxGames()
	{
		return maxGames;
	}

	public boolean isRunning()
	{
		return isRunning;
	}

	protected int getChatFloodTime()
	{
		return chatFloodTime;
	}

	protected int getCreateGameFloodTime()
	{
		return createGameFloodTime;
	}

	protected boolean getAllowSinglePlayer()
	{
		return allowSinglePlayer;
	}

	protected int getMaxUserNameLength()
	{
		return maxUserNameLength;
	}

	protected int getMaxChatLength()
	{
		return maxChatLength;
	}
	
	protected int getMaxGameChatLength()
	{
		return maxGameChatLength;
	}

	protected int getMaxGameNameLength()
	{
		return maxGameNameLength;
	}

	protected int getQuitMessageLength()
	{
		return maxQuitMessageLength;
	}

	protected int getMaxClientNameLength()
	{
		return maxClientNameLength;
	}

	protected boolean getAllowMultipleConnections()
	{
		return allowMultipleConnections;
	}

	public ThreadPoolExecutor getThreadPool()
	{
		return threadPool;
	}

	public String toString()
	{
		return "KailleraServerImpl[numUsers=" + getNumUsers() + " numGames=" + getNumGames() + " isRunning=" + isRunning() + "]";   //$NON-NLS-4$
	}

	public synchronized void start()
	{
		log.debug("KailleraServer thread received start request!");
		log.debug("KailleraServer thread starting (ThreadPool:" + threadPool.getActiveCount() + "/" + threadPool.getPoolSize() + ")");  
		stopFlag = false;
		threadPool.execute(this);
		Thread.yield();
	}

	public synchronized void stop()
	{
		log.debug("KailleraServer thread received stop request!");

		if (!isRunning())
		{
			log.debug("KailleraServer thread stop request ignored: not running!");
			return;
		}

		stopFlag = true;

		for (KailleraUserImpl user : users.values())
			user.stop();

		users.clear();
		games.clear();
	}

	// not synchronized because I know the caller will be thread safe
	protected int getNextUserID()
	{
		if (connectionCounter > 0xFFFF)
			connectionCounter = 1;

		return connectionCounter++;
	}

	// not synchronized because I know the caller will be thread safe
	protected int getNextGameID()
	{
		if (gameCounter > 0xFFFF)
			gameCounter = 1;

		return gameCounter++;
	}
	
	protected StatsCollector getStatsCollector()
	{
		return statsCollector;
	}
	
	protected AutoFireDetector getAutoFireDetector(KailleraGame game)
	{
		return autoFireDetectorFactory.getInstance(game, gameAutoFireSensitivity);
	}
	
	public ReleaseInfo getReleaseInfo()
	{
		return releaseInfo;
	}
	
	public synchronized KailleraUser newConnection(InetSocketAddress clientSocketAddress, String protocol, KailleraEventListener listener) throws ServerFullException, NewConnectionException
	{
		// we'll assume at this point that ConnectController has already asked AccessManager if this IP is banned, so no need to do it again here

		log.debug("Processing connection request from " + EmuUtil.formatSocketAddress(clientSocketAddress));

		int access = accessManager.getAccess(clientSocketAddress.getAddress());

		// admins will be allowed in even if the server is full
		if (getMaxUsers() > 0 && users.size() >= getMaxUsers() && !(access > AccessManager.ACCESS_NORMAL))
		{
			log.warn("Connection from " + EmuUtil.formatSocketAddress(clientSocketAddress) + " denied: Server is full!"); 
			throw new ServerFullException(EmuLang.getString("KailleraServerImpl.LoginDeniedServerFull"));
		}
		
		int userID = getNextUserID();
		KailleraUserImpl user = new KailleraUserImpl(userID, protocol, clientSocketAddress, listener, this);
		user.setStatus(KailleraUser.STATUS_CONNECTING);

		log.info(user + " attempting new connection using protocol " + protocol + " from " + EmuUtil.formatSocketAddress(clientSocketAddress)); 

		log.debug(user + " Thread starting (ThreadPool:" + threadPool.getActiveCount() + "/" + threadPool.getPoolSize() + ")");  
		threadPool.execute(user);
		Thread.yield();
		log.debug(user + " Thread started (ThreadPool:" + threadPool.getActiveCount() + "/" + threadPool.getPoolSize() + ")");  
		users.put(userID, user);

		return user;
	}

	public synchronized void login(KailleraUser user) throws PingTimeException, ClientAddressException, ConnectionTypeException, UserNameException, LoginException
	{
		KailleraUserImpl userImpl = (KailleraUserImpl) user;
		
		long loginDelay = (System.currentTimeMillis() - user.getConnectTime());
		log.info(user + ": login request: delay=" + loginDelay + "ms, clientAddress=" + EmuUtil.formatSocketAddress(user.getSocketAddress()) + ", name=" + user.getName() + ", ping=" + user.getPing() + ", client=" + user.getClientType() + ", connection=" + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]);   //$NON-NLS-4$ //$NON-NLS-5$

		if (user.isLoggedIn())
		{
			log.warn(user + " login denied: Already logged in!");
			throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginDeniedAlreadyLoggedIn"));
		}

		Integer userListKey = new Integer(user.getID());
		KailleraUser u = users.get(userListKey);
		if (u == null)
		{
			log.warn(user + " login denied: Connection timed out!");
			throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginDeniedConnectionTimedOut"));
		}

		int access = accessManager.getAccess(user.getSocketAddress().getAddress());
		if (access < AccessManager.ACCESS_NORMAL)
		{
			log.info(user + " login denied: Access denied");
			users.remove(userListKey);
			throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginDeniedAccessDenied"));
		}

		if (access == AccessManager.ACCESS_NORMAL && getMaxPing() > 0 && user.getPing() > getMaxPing())
		{
			log.info(user + " login denied: Ping " + user.getPing() + " > " + getMaxPing()); 
			users.remove(userListKey);
			throw new PingTimeException(EmuLang.getString("KailleraServerImpl.LoginDeniedPingTooHigh", (user.getPing() + " > " + getMaxPing()))); 
		}

		if (access == AccessManager.ACCESS_NORMAL && allowedConnectionTypes[user.getConnectionType()] == false)
		{
			log.info(user + " login denied: Connection " + KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()] + " Not Allowed"); 
			users.remove(userListKey);
			throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginDeniedConnectionTypeDenied",  KailleraUser.CONNECTION_TYPE_NAMES[user.getConnectionType()]));
		}

		if (user.getPing() < 0)
		{
			log.warn(user + " login denied: Invalid ping: " + user.getPing());
			users.remove(userListKey);
			throw new PingTimeException(EmuLang.getString("KailleraServerImpl.LoginErrorInvalidPing", user.getPing()));
		}

		if (access == AccessManager.ACCESS_NORMAL && user.getName().trim().length() == 0)
		{
			log.info(user + " login denied: Empty UserName");
			users.remove(userListKey);
			throw new UserNameException(EmuLang.getString("KailleraServerImpl.LoginDeniedUserNameEmpty"));
		}
		
		//new SF MOD - Username filter
		if((user.getName().equals("Server") || user.getName().toLowerCase().contains("|")) || (access == AccessManager.ACCESS_NORMAL && (user.getName().toLowerCase().contains("www.") || user.getName().toLowerCase().contains("http://") || user.getName().toLowerCase().contains("https://") || user.getName().toLowerCase().contains("\\") || user.getName().toLowerCase().contains(" ") || user.getName().toLowerCase().contains("­"))))
		{
			log.info(user + " login denied: Illegal characters in UserName");
			users.remove(userListKey);
			throw new UserNameException(EmuLang.getString("KailleraServerImpl.LoginDeniedIllegalCharactersInUserName"));
		}

		//access == AccessManager.ACCESS_NORMAL && 
		if (maxUserNameLength > 0 && user.getName().length() > getMaxUserNameLength())
		{
			log.info(user + " login denied: UserName Length > " + getMaxUserNameLength());
			users.remove(userListKey);
			throw new UserNameException(EmuLang.getString("KailleraServerImpl.LoginDeniedUserNameTooLong"));
		}

		if (access == AccessManager.ACCESS_NORMAL && maxClientNameLength > 0 && user.getClientType().length() > getMaxClientNameLength())
		{
			log.info(user + " login denied: Client Name Length > " + getMaxClientNameLength());
			users.remove(userListKey);
			throw new UserNameException(EmuLang.getString("KailleraServerImpl.LoginDeniedEmulatorNameTooLong"));
		}
		
		if (user.getClientType().toLowerCase().contains("|"))
		{
			log.warn(user + " login denied: Illegal characters in EmulatorName");
			users.remove(userListKey);
			throw new UserNameException("Illegal characters in Emulator Name");
		}

		if (access == AccessManager.ACCESS_NORMAL)
		{
			char[] chars = user.getName().toCharArray();
			for (int i = 0; i < chars.length; i++)
			{
				if (chars[i] < 32)
				{
					log.info(user + " login denied: Illegal characters in UserName");
					users.remove(userListKey);
					throw new UserNameException(EmuLang.getString("KailleraServerImpl.LoginDeniedIllegalCharactersInUserName"));
				}
			}
		}

		if (u.getStatus() != KailleraUser.STATUS_CONNECTING)
		{
			users.remove(userListKey);
			log.warn(user + " login denied: Invalid status=" + KailleraUser.STATUS_NAMES[u.getStatus()]);
			throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginErrorInvalidStatus", u.getStatus())); 
		}

		if (!u.getConnectSocketAddress().getAddress().equals(user.getSocketAddress().getAddress()))
		{
			users.remove(userListKey);
			log.warn(user + " login denied: Connect address does not match login address: " + u.getConnectSocketAddress().getAddress().getHostAddress() + " != " + user.getSocketAddress().getAddress().getHostAddress()); 
			throw new ClientAddressException(EmuLang.getString("KailleraServerImpl.LoginDeniedAddressMatchError"));
		}

		if (access == AccessManager.ACCESS_NORMAL && !accessManager.isEmulatorAllowed(user.getClientType()))
		{
			log.info(user + " login denied: AccessManager denied emulator: " + user.getClientType());
			users.remove(userListKey);
			throw new LoginException(EmuLang.getString("KailleraServerImpl.LoginDeniedEmulatorRestricted", user.getClientType()));
		}

		for (KailleraUserImpl u2 : getUsers())
		{
			if (u2.isLoggedIn())
			{
				if (!u2.equals(u) && u.getConnectSocketAddress().getAddress().equals(u2.getConnectSocketAddress().getAddress()) && u.getName().equals(u2.getName()))
				{
					// user is attempting to login more than once with the same name and address
					// logoff the old user and login the new one

					try
					{
						quit(u2, EmuLang.getString("KailleraServerImpl.ForcedQuitReconnected"));
					}
					catch (Exception e)
					{
						log.error("Error forcing " + u2 + " quit for reconnect!", e); 
					}
				}
				else if(!u2.equals(u) && u2.getName().toLowerCase().trim().equals(u.getName().toLowerCase().trim())){
					users.remove(userListKey);
					log.warn(user + " login denied: Duplicating Names is not allowed! " + u2.getName());
					throw new ClientAddressException("Duplicating names is not allowed: " + u2.getName());
				}
				
				if (access == AccessManager.ACCESS_NORMAL && !u2.equals(u) && u.getConnectSocketAddress().getAddress().equals(u2.getConnectSocketAddress().getAddress()) && !u.getName().equals(u2.getName()) && !allowMultipleConnections)
				{
					users.remove(userListKey);
					log.warn(user + " login denied: Address already logged in as " + u2.getName());
					throw new ClientAddressException(EmuLang.getString("KailleraServerImpl.LoginDeniedAlreadyLoggedInAs", u2.getName()));
				}
				

			}
		}

		// passed all checks
		
		userImpl.setAccess(access);
		userImpl.setStatus(KailleraUser.STATUS_IDLE);
		userImpl.setLoggedIn();
		users.put(userListKey, userImpl);
		userImpl.addEvent(new ConnectedEvent(this, user));
		try { Thread.sleep(20); } catch(Exception e) {}

		for (String loginMessage : loginMessages){
			userImpl.addEvent(new InfoMessageEvent(user, loginMessage));
			try { Thread.sleep(20); } catch(Exception e) {}
		}
		
		if (access > AccessManager.ACCESS_NORMAL)
			log.info(user + " logged in successfully with " + AccessManager.ACCESS_NAMES[access] + " access!"); 
		else
			log.info(user + " logged in successfully");

		// this is fairly ugly
		if(user.isEmuLinkerClient())
		{
			userImpl.addEvent(new InfoMessageEvent(user, ":ACCESS="+userImpl.getAccessStr()));
			
			if(access >= AccessManager.ACCESS_SUPERADMIN)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(":USERINFO=");
				int sbCount = 0;
				for (KailleraUserImpl u3 : getUsers())
				{
					if (!u3.isLoggedIn())
						continue;
					
					sb.append(u3.getID());
					sb.append((char)0x02);
					sb.append(u3.getConnectSocketAddress().getAddress().getHostAddress());
					sb.append((char)0x02);
					sb.append(u3.getAccessStr());
					sb.append((char)0x02);
					//str = u3.getName().replace(',','.');
					//str = str.replace(';','.');
					sb.append(u3.getName());
					sb.append((char)0x02);
					sb.append(u3.getPing());
					sb.append((char)0x02);
					sb.append(u3.getStatus());
					sb.append((char)0x02);
					sb.append(u3.getConnectionType());
					sb.append((char)0x03);
					sbCount++;
					
					if(sb.length() > 300)
					{
						((KailleraUserImpl) user).addEvent(new InfoMessageEvent(user, sb.toString()));
						sb = new StringBuilder();
						sb.append(":USERINFO=");
						sbCount = 0;
						try { Thread.sleep(100); } catch(Exception e) {}
					}
				}
				if(sbCount > 0)
					((KailleraUserImpl) user).addEvent(new InfoMessageEvent(user, sb.toString()));
				try { Thread.sleep(100); } catch(Exception e) {}
			}
		}
		
		try { Thread.sleep(20); } catch(Exception e) {}
		if (access >= AccessManager.ACCESS_ADMIN)
			userImpl.addEvent(new InfoMessageEvent(user, EmuLang.getString("KailleraServerImpl.AdminWelcomeMessage")));
		
		try { Thread.sleep(20); } catch(Exception e) {}
		userImpl.addEvent(new InfoMessageEvent(user, getReleaseInfo().getProductName() + " v" + getReleaseInfo().getVersionString() + ": " + getReleaseInfo().getReleaseDate() + " - Visit: www.EmuLinker.org"));		
		
		try { Thread.sleep(20); } catch(Exception e) {}
		addEvent(new UserJoinedEvent(this, user));
		
		try { Thread.sleep(20); } catch(Exception e) {}
		String announcement = accessManager.getAnnouncement(user.getSocketAddress().getAddress());
		if (announcement != null)
			announce(announcement, false, null);		
	}

	public synchronized void quit(KailleraUser user, String message) throws QuitException, DropGameException, QuitGameException, CloseGameException
	{
		if (!user.isLoggedIn())
		{
			users.remove(user.getID());
			log.error(user + " quit failed: Not logged in");
			throw new QuitException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
		}

		if (users.remove(user.getID()) == null)
			log.error(user + " quit failed: not in user list");
			

		KailleraGameImpl userGame = ((KailleraUserImpl) user).getGame();
		if (userGame != null)
			user.quitGame();

		String quitMsg = message.trim();
		if (quitMsg.length() == 0 || (maxQuitMessageLength > 0 && quitMsg.length() > maxQuitMessageLength))
			quitMsg = EmuLang.getString("KailleraServerImpl.StandardQuitMessage");
		
		int access = user.getServer().getAccessManager().getAccess(user.getSocketAddress().getAddress());
		if (access < AccessManager.ACCESS_SUPERADMIN && user.getServer().getAccessManager().isSilenced(user.getSocketAddress().getAddress())){
			quitMsg = "www.EmuLinker.org";
		}

		log.info(user + " quit: " + quitMsg);

		UserQuitEvent quitEvent = new UserQuitEvent(this, user, quitMsg);

		addEvent(quitEvent);
		((KailleraUserImpl) user).addEvent(quitEvent);
	}

	public synchronized void chat(KailleraUser user, String message) throws ChatException, FloodException
	{
		if (!user.isLoggedIn())
		{
			log.error(user + " chat failed: Not logged in");
			throw new ChatException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
		}

		int access = accessManager.getAccess(user.getSocketAddress().getAddress());
		if (access < AccessManager.ACCESS_SUPERADMIN && accessManager.isSilenced(user.getSocketAddress().getAddress()))
		{
			log.warn(user + " chat denied: Silenced: " + message);
			throw new ChatException(EmuLang.getString("KailleraServerImpl.ChatDeniedSilenced"));
		}

		if (access == AccessManager.ACCESS_NORMAL && chatFloodTime > 0 && (System.currentTimeMillis() - ((KailleraUserImpl) user).getLastChatTime()) < (chatFloodTime * 1000))
		{
			log.warn(user + " chat denied: Flood: " + message);
			throw new FloodException(EmuLang.getString("KailleraServerImpl.ChatDeniedFloodControl"));
		}

		if(message.equals(":USER_COMMAND")){
			return;
		}

		message = message.trim();
		if (message.length() == 0 || message.startsWith(" ") || message.startsWith("­"))
			return;

		if (access == AccessManager.ACCESS_NORMAL)
		{
			char[] chars = message.toCharArray();
			for (int i = 0; i < chars.length; i++)
			{
				if (chars[i] < 32)
				{
					log.warn(user + " chat denied: Illegal characters in message");
					throw new ChatException(EmuLang.getString("KailleraServerImpl.ChatDeniedIllegalCharacters"));
				}
			}

			if (maxChatLength > 0 && message.length() > maxChatLength)
			{
				log.warn(user + " chat denied: Message Length > " + maxChatLength);
				throw new ChatException(EmuLang.getString("KailleraServerImpl.ChatDeniedMessageTooLong"));
			}
		}
		

		log.info(user + " chat: " + message);

		
		addEvent(new ChatEvent(this, user, message));

		if(switchTrivia){
			if(!trivia.isAnswered() && trivia.isCorrect(message)){
				trivia.addScore(user.getName(), user.getSocketAddress().getAddress().getHostAddress(), message);
			}
		}
	}

	public synchronized KailleraGame createGame(KailleraUser user, String romName) throws CreateGameException, FloodException
	{
		if (!user.isLoggedIn())
		{
			log.error(user + " create game failed: Not logged in");
			throw new CreateGameException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
		}

		if (((KailleraUserImpl) user).getGame() != null)
		{
			log.error(user + " create game failed: already in game: " + ((KailleraUserImpl) user).getGame());
			throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameErrorAlreadyInGame"));
		}

		if (maxGameNameLength > 0 && romName.trim().length() > maxGameNameLength)
		{
			log.warn(user + " create game denied: Rom Name Length > " + maxGameNameLength);
			throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameDeniedNameTooLong"));
		}		
		
		if (romName.toLowerCase().contains("|"))
		{
			log.warn(user + " create game denied: Illegal characters in ROM name");
			throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameDeniedIllegalCharacters"));
		}
		
		int access = accessManager.getAccess(user.getSocketAddress().getAddress());
		if (access == AccessManager.ACCESS_NORMAL)
		{
			if (createGameFloodTime > 0 && (System.currentTimeMillis() - ((KailleraUserImpl) user).getLastCreateGameTime()) < (createGameFloodTime * 1000))
			{
				log.warn(user + " create game denied: Flood: " + romName);
				throw new FloodException(EmuLang.getString("KailleraServerImpl.CreateGameDeniedFloodControl"));
			}

			if (maxGames > 0 && getNumGames() >= maxGames)
			{
				log.warn(user + " create game denied: Over maximum of " + maxGames + " current games!"); 
				throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameDeniedMaxGames", maxGames));
			}

			char[] chars = romName.toCharArray();
			for (int i = 0; i < chars.length; i++)
			{
				if (chars[i] < 32)
				{
					log.warn(user + " create game denied: Illegal characters in ROM name");
					throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameDeniedIllegalCharacters"));
				}
			}

			if (romName.trim().length() == 0)
			{
				log.warn(user + " create game denied: Rom Name Empty");
				throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameErrorEmptyName"));
			}


			if (!accessManager.isGameAllowed(romName))
			{
				log.warn(user + " create game denied: AccessManager denied game: " + romName);
				throw new CreateGameException(EmuLang.getString("KailleraServerImpl.CreateGameDeniedGameBanned"));
			}
		}

		KailleraGameImpl game = null;

		int gameID = getNextGameID();
		game = new KailleraGameImpl(gameID, romName, (KailleraUserImpl) user, this, gameBufferSize, gameTimeoutMillis, gameDesynchTimeouts);
		games.put(gameID, game);

		addEvent(new GameCreatedEvent(this, game));

		log.info(user + " created: " + game + ": " + game.getRomName()); 

		try
		{
			user.joinGame(game.getID());
		}
		catch (Exception e)
		{
			// this shouldn't happen
			log.error("Caught exception while making owner join game! This shouldn't happen!", e);
		}

		announce(EmuLang.getString("KailleraServerImpl.UserCreatedGameAnnouncement", user.getName(), game.getRomName()), false, null);
		
		return game;
	}

	synchronized void closeGame(KailleraGame game, KailleraUser user) throws CloseGameException
	{
		if (!user.isLoggedIn())
		{
			log.error(user + " close " + game + " failed: Not logged in"); 
			throw new CloseGameException(EmuLang.getString("KailleraServerImpl.NotLoggedIn"));
		}

		if (!games.containsKey(game.getID()))
		{
			log.error(user + " close " + game + " failed: not in list: " + game); 
			return;
		}

		((KailleraGameImpl) game).close(user);
		games.remove(game.getID());

		log.info(user + " closed: " + game);
		addEvent(new GameClosedEvent(this, game));
	}
	
	public boolean checkMe(KailleraUser user, String message){
		//>>>>>>>>>>>>>>>>>>>>
		if (!user.isLoggedIn())
		{
			log.error(user + " chat failed: Not logged in");
			return false;
		}

		int access = accessManager.getAccess(user.getSocketAddress().getAddress());
		if (access < AccessManager.ACCESS_SUPERADMIN && accessManager.isSilenced(user.getSocketAddress().getAddress()))
		{
			log.warn(user + " /me: Silenced: " + message);
			return false;
		}

		//if (access == AccessManager.ACCESS_NORMAL && chatFloodTime > 0 && (System.currentTimeMillis() - ((KailleraUserImpl) user).getLastChatTime()) < (chatFloodTime * 1000))
		//{
		//	log.warn(user + " /me denied: Flood: " + message);
		//	return false;
		//}

		if(message.equals(":USER_COMMAND")){
			return false;
		}

		message = message.trim();
		if (message.length() == 0)
			return false;

		if (access == AccessManager.ACCESS_NORMAL)
		{
			char[] chars = message.toCharArray();
			for (int i = 0; i < chars.length; i++)
			{
				if (chars[i] < 32)
				{
					log.warn(user + " /me: Illegal characters in message");
					return false;
				}
			}

			if (maxChatLength > 0 && message.length() > maxChatLength)
			{
				log.warn(user + " /me denied: Message Length > " + maxChatLength);
				return false;
			}
		}	
		
		return true;
	}

	public void announce(String announcement, boolean gamesAlso, KailleraUserImpl user)
	{
		if(user != null){
			if(gamesAlso){//   /msg and /me commands
				for (KailleraUserImpl kailleraUser : getUsers())
				{
					if (kailleraUser.isLoggedIn()){
						int access = accessManager.getAccess(user.getConnectSocketAddress().getAddress());
						if(access <  AccessManager.ACCESS_ADMIN){
							if(!kailleraUser.searchIgnoredUsers(user.getConnectSocketAddress().getAddress().getHostAddress()))
								kailleraUser.addEvent(new InfoMessageEvent(kailleraUser, announcement));
						}
						else{
							kailleraUser.addEvent(new InfoMessageEvent(kailleraUser, announcement));
						}
						
						/*//SF MOD
						if(gamesAlso){
							if(kailleraUser.getGame() != null){
								kailleraUser.getGame().announce(announcement, kailleraUser);
								Thread.yield();
							}
						}
						*/
					}
				}	
			}
			else{
				user.addEvent(new InfoMessageEvent(user, announcement));
			}
			return;
		}
		

		for (KailleraUserImpl kailleraUser : getUsers())
		{
			if (kailleraUser.isLoggedIn()){
				kailleraUser.addEvent(new InfoMessageEvent(kailleraUser, announcement));
				
				//SF MOD
				if(gamesAlso){
					if(kailleraUser.getGame() != null){
						kailleraUser.getGame().announce(announcement, kailleraUser);
						Thread.yield();
					}
				}
			}
		}
	}

	protected void addEvent(ServerEvent event)
	{
		for (KailleraUserImpl user : users.values())
		{
			if(user.isLoggedIn()){
				if(user.getStatus() != KailleraUser.STATUS_IDLE){
					if(user.getP2P()){
						if(event.toString().equals("GameDataEvent"))
							user.addEvent(event);
						else if(event.toString().equals("ChatEvent"))
							continue;
						else if(event.toString().equals("UserJoinedEvent"))
							continue;
						else if(event.toString().equals("UserQuitEvent"))
							continue;
						else if(event.toString().equals("GameStatusChangedEvent"))
							continue;
						else if(event.toString().equals("GameClosedEvent"))
							continue;
						else if(event.toString().equals("GameCreatedEvent"))
							continue;
						else
							user.addEvent(event);
					}
					else{
						user.addEvent(event);
					}
				}
				else{
					user.addEvent(event);
				}
			}
			else{
				log.debug(user + ": not adding event, not logged in: " + event);
			}
		}
	}

	public void run()
	{
		isRunning = true;
		log.debug("KailleraServer thread running...");

		try
		{
			while (!stopFlag)
			{
				try
				{
					Thread.sleep((long) (maxPing * 3));
				}
				catch (InterruptedException e)
				{
					log.error("Sleep Interrupted!", e);
				}

				//				log.debug(this + " running maintenance...");

				if (stopFlag)
					break;

				if (users.isEmpty())
					continue;

				for (KailleraUserImpl user : getUsers())
				{
					synchronized (user)
					{
						int access = accessManager.getAccess(user.getConnectSocketAddress().getAddress());
						((KailleraUserImpl) user).setAccess(access);
						
						//LagStat
						if(user.isLoggedIn()){
							if(user.getGame() != null && user.getGame().getStatus() == KailleraGame.STATUS_PLAYING && !user.getGame().getStartTimeout()){
								if(System.currentTimeMillis() - user.getGame().getStartTimeoutTime() > 15000){
									user.getGame().setStartTimeout(true);
								}
							}
						}
						
						if (!user.isLoggedIn() && (System.currentTimeMillis() - user.getConnectTime()) > (maxPing * 15))
						{
							log.info(user + " connection timeout!");
							user.stop();
							users.remove(user.getID());
						}
						else if (user.isLoggedIn() && (System.currentTimeMillis() - user.getLastKeepAlive()) > (keepAliveTimeout * 1000))
						{
							log.info(user + " keepalive timeout!");
							try
							{
								quit(user, EmuLang.getString("KailleraServerImpl.ForcedQuitPingTimeout"));
							}
							catch (Exception e)
							{
								log.error("Error forcing " + user + " quit for keepalive timeout!", e); 
							}
						}
						else if (idleTimeout > 0 && access == AccessManager.ACCESS_NORMAL && user.isLoggedIn() && (System.currentTimeMillis() - user.getLastActivity()) > (idleTimeout * 1000))
						{
							log.info(user + " inactivity timeout!");
							try
							{
								quit(user, EmuLang.getString("KailleraServerImpl.ForcedQuitInactivityTimeout"));
							}
							catch (Exception e)
							{
								log.error("Error forcing " + user + " quit for inactivity timeout!", e); 
							}
						}
						else if (user.isLoggedIn() && access < AccessManager.ACCESS_NORMAL)
						{
							log.info(user + " banned!");
							try
							{
								quit(user, EmuLang.getString("KailleraServerImpl.ForcedQuitBanned"));
							}
							catch (Exception e)
							{
								log.error("Error forcing " + user + " quit because banned!", e); 
							}
						}
						else if (user.isLoggedIn() && access == AccessManager.ACCESS_NORMAL && !accessManager.isEmulatorAllowed(user.getClientType()))
						{
							log.info(user + ": emulator restricted!");
							try
							{
								quit(user, EmuLang.getString("KailleraServerImpl.ForcedQuitEmulatorRestricted"));
							}
							catch (Exception e)
							{
								log.error("Error forcing " + user + " quit because emulator restricted!", e); 
							}
						}
					}
				}
			}
		}
		catch (Throwable e)
		{
			if (!stopFlag)
				log.fatal("KailleraServer thread caught unexpected exception: " + e, e);
		}
		finally
		{
			isRunning = false;
			log.debug("KailleraServer thread exiting...");
		}
	}
}

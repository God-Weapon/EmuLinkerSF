package org.emulinker.kaillera.controller.v086.action;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.exception.GameChatException;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;

public class GameChatAction implements V086Action, V086GameEventHandler
{
	public static final String	ADMIN_COMMAND_ESCAPE_STRING	= "/";

	private static Log				log			= LogFactory.getLog(GameChatAction.class);
	private static final String		desc		= "GameChatAction";
	public static final byte		STATUS_IDLE					= 1;
	private static GameChatAction	singleton	= new GameChatAction();

	public static GameChatAction getInstance()
	{
		return singleton;
	}

	private int	actionCount		= 0;
	private int	handledCount	= 0;

	private GameChatAction()
	{

	}

	public int getActionPerformedCount()
	{
		return actionCount;
	}

	public int getHandledEventCount()
	{
		return handledCount;
	}

	public String toString()
	{
		return desc;
	}

	public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException
	{
		if(!(message instanceof GameChat_Request))
			throw new FatalActionException("Received incorrect instance of GameChat: " + message);
	
		if(clientHandler.getUser() == null){
			throw new FatalActionException("User does not exist: GameChatAction " + message);
		}
	
	
		if (((GameChat) message).getMessage().startsWith(ADMIN_COMMAND_ESCAPE_STRING))
		{
			if(clientHandler.getUser().getAccess() >= AccessManager.ACCESS_ADMIN || clientHandler.getUser().equals(clientHandler.getUser().getGame().getOwner())){
				try
				{
					if(GameOwnerCommandAction.getInstance().isValidCommand(((GameChat) message).getMessage()))
						GameOwnerCommandAction.getInstance().performAction(message, clientHandler);
					else
						checkCommands(message, clientHandler);
					return;
				}
				catch (FatalActionException e)
				{
					log.warn("GameOwner command failed: " + e.getMessage());
				}
				
			}
		}	
		
		actionCount++;

		GameChat_Request gameChatMessage = (GameChat_Request) message;

		try
		{
			clientHandler.getUser().gameChat(gameChatMessage.getMessage(), gameChatMessage.getNumber());
		}
		catch (GameChatException e)
		{
			log.debug("Failed to send game chat message: " + e.getMessage());
		}
	}

	private void checkCommands(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException
	{	
		boolean doCommand = true;

		if(clientHandler.getUser().getAccess() < AccessManager.ACCESS_ELEVATED){
			try{
				clientHandler.getUser().chat(":USER_COMMAND");
			}
			catch (ActionException e){
				doCommand = false;
			}
		}

			if(doCommand){
				if(((GameChat) message).getMessage().equals("/msgon")){
					clientHandler.getUser().setMsg(true);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "Private messages are now on."));} catch(Exception e) {}
					return;
				}
				else if(((GameChat) message).getMessage().equals("/msgoff")){
					clientHandler.getUser().setMsg(false);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "Private messages are now off."));} catch(Exception e) {}
					return;
				}		
				else if(((GameChat) message).getMessage().startsWith("/p2p")){
					KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
					if(((GameChat) message).getMessage().equals("/p2pon")){
						clientHandler.getUser().setP2P(true);
						
						if(clientHandler.getUser().getGame().getOwner().equals(clientHandler.getUser())){
							clientHandler.getUser().getGame().setP2P(true);
							for(KailleraUserImpl u : clientHandler.getUser().getGame().getPlayers()){
								if(u.isLoggedIn()){
									u.getGame().announce("This game will NOT receive any server activity during gameplay!", u);
								}
							}
						}
						else{
							for(KailleraUserImpl u : clientHandler.getUser().getGame().getPlayers()){
								if(u.isLoggedIn()){
									u.getGame().announce(clientHandler.getUser().getName() + " will NOT receive any server activity during gameplay!", u);
								}
							}
						}
					}
					else if(((GameChat) message).getMessage().equals("/p2poff")){
						clientHandler.getUser().setP2P(false);
						
						if(clientHandler.getUser().getGame().getOwner().equals(clientHandler.getUser())){
							clientHandler.getUser().getGame().setP2P(false);
							for (KailleraUserImpl u : clientHandler.getUser().getGame().getPlayers()){
								if(u.isLoggedIn()){
									u.getGame().announce("This game will NOW receive ALL server activity during gameplay!", u);
								}
							}
						}
						else{
							for (KailleraUserImpl u : clientHandler.getUser().getGame().getPlayers()){
								if(u.isLoggedIn()){
									u.getGame().announce(clientHandler.getUser().getName() + " will NOW receive ALL server activity during gameplay!", u);
								}
							}
						}					
					}
					else{
						user.getGame().announce("Failed P2P: /p2pon or /p2poff", user); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return;
				}
				else if (((GameChat) message).getMessage().startsWith("/msg")){
					KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
					Scanner scanner = new Scanner(((GameChat) message).getMessage()).useDelimiter(" "); //$NON-NLS-1$
					
					int access = clientHandler.getUser().getServer().getAccessManager().getAccess(clientHandler.getUser().getSocketAddress().getAddress());
					if (access < AccessManager.ACCESS_SUPERADMIN && clientHandler.getUser().getServer().getAccessManager().isSilenced(clientHandler.getUser().getSocketAddress().getAddress())){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "You are silenced!"));} catch(Exception e) {}
						return;
					}
		
					try
					{
						scanner.next();
						int userID = scanner.nextInt();
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);
		
						StringBuilder sb = new StringBuilder();
						while (scanner.hasNext())
						{
							sb.append(scanner.next());
							sb.append(" "); //$NON-NLS-1$
						}
						
						if (user == null){
							user1.getGame().announce("User not found!", user1);
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
							return;
						}
		
						if(user == clientHandler.getUser()){
							user1.getGame().announce("You can't private message yourself!", user1);
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't private message yourself!")); } catch(Exception e) {}
							return;					
						}
						
						if(user.getMsg() == false || user.searchIgnoredUsers(clientHandler.getUser().getConnectSocketAddress().getAddress().getHostAddress()) == true){
							user1.getGame().announce("<" + user.getName() + "> Is not accepting private messages!", user1);
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","<" + user.getName() + "> Is not accepting private messages!")); } catch(Exception e) {}
							return;						
						}
		
						String m = sb.toString();

						
						user1.setLastMsgID(user.getID());
						user.setLastMsgID(user1.getID());
						
						user1.getServer().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, false, user1);
						user.getServer().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, false, user);
						if(user1.getGame() != null){
							user1.getGame().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user1);
						}	
		
						if(user.getGame() != null){
							user.getGame().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user);
						}
						return;
					}
					catch (NoSuchElementException e)
					{
						if(user1.getLastMsgID() != -1){
							try
							{
								KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser().getServer().getUser(user1.getLastMsgID());
		
								StringBuilder sb = new StringBuilder();
								while (scanner.hasNext())
								{
									sb.append(scanner.next());
									sb.append(" "); //$NON-NLS-1$
								}
								
								if (user == null){
									user1.getGame().announce("User not found!", user1);
									try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e1) {}
									return;
								}
		
								if(user == clientHandler.getUser()){
									user1.getGame().announce("You can't private message yourself!", user1);
									try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't private message yourself!")); } catch(Exception e1) {}
									return;					
								}
								
								if(user.getMsg() == false){
									user1.getGame().announce("<" + user.getName() + "> Is not accepting private messages!", user1);
									try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","<" + user.getName() + "> Is not accepting private messages!")); } catch(Exception e1) {}
									return;						
								}
		
								String m = sb.toString();

								
								user1.getServer().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, false, user1);
								user.getServer().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, false, user);						
								
								if(user1.getGame() != null){
									user1.getGame().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user1);
								}	
		
								if(user.getGame() != null){
									user.getGame().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user);
								}
								return;
							}
							catch(Exception e1){
								user1.getGame().announce("Private Message Error: /msg <UserID> <message>", user1);
								return;
							}
						}
						user1.getGame().announce("Private Message Error: /msg <UserID> <message>", user1);
						return;
					}		
				}
				else if(((GameChat) message).getMessage().equals("/ignoreall")){
					clientHandler.getUser().setIgnoreAll(true);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server",clientHandler.getUser().getName() + " is now ignoring everyone!")); } catch(Exception e) {}
					return;
				}
				else if(((GameChat) message).getMessage().equals("/unignoreall")){
					clientHandler.getUser().setIgnoreAll(false);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server",clientHandler.getUser().getName() + " is now unignoring everyone!")); } catch(Exception e) {}
					return;
				}
				else if(((GameChat) message).getMessage().startsWith("/ignore")){
					Scanner scanner = new Scanner(((GameChat) message).getMessage()).useDelimiter(" "); //$NON-NLS-1$
		
					try
					{
						scanner.next();
						int userID = scanner.nextInt();
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);
					
						if (user == null){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
							return;
						}
						if (user.getAccess() >= AccessManager.ACCESS_MODERATOR){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You cannot ignore an admin!")); } catch(Exception e) {}
							return;
						}
						if(user == clientHandler.getUser()){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't ignore yourself!")); } catch(Exception e) {}
							return;					
						}
						if(clientHandler.getUser().findIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress())){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't ignore a user that is already ignored!")); } catch(Exception e) {}
							return;
						}
						
						clientHandler.getUser().addIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress());
						user.getServer().announce(clientHandler.getUser().getName() + " is now ignoring <" + user.getName() + "> ID: " + user.getID(), false, null); //$NON-NLS-1$ //$NON-NLS-2$
						return;
					}
					catch (NoSuchElementException e)
					{
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
						user.getServer().announce("Ignore User Error: /ignore <UserID>", false, user); //$NON-NLS-1$ //$NON-NLS-2$
						log.info("IGNORE USER ERROR: " + user.getName() + ": " + clientHandler.getRemoteSocketAddress().getHostName());
						return;
					}
				}
				else if(((GameChat) message).getMessage().startsWith("/unignore")){
					Scanner scanner = new Scanner(((GameChat) message).getMessage()).useDelimiter(" "); //$NON-NLS-1$
		
					try
					{
						scanner.next();
						int userID = scanner.nextInt();
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);
										
						if (user == null){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
							return;
						}
						if(clientHandler.getUser().findIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress())){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't unignore a user that isn't ignored!")); } catch(Exception e) {}
							return;
						}
					
						if(clientHandler.getUser().removeIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress(), false) == true)
							user.getServer().announce(clientHandler.getUser().getName() + " is now unignoring <" + user.getName() + "> ID: " + user.getID(), false, null); //$NON-NLS-1$ //$NON-NLS-2$
						else
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
							
						return;
					}
					catch (NoSuchElementException e)
					{
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
						user.getServer().announce("Unignore User Error: /ignore <UserID>", false, user); //$NON-NLS-1$ //$NON-NLS-2$
						log.info("UNIGNORE USER ERROR: " + user.getName() + ": " + clientHandler.getRemoteSocketAddress().getHostName());
						return;
					}
		
				}
				else if(((GameChat) message).getMessage().startsWith("/me")){
					int space = ((GameChat) message).getMessage().indexOf(' ');
					if (space < 0){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","Invalid # of Fields!")); } catch(Exception e) {}
						return;
					}
				
					String announcement = ((GameChat) message).getMessage().substring(space + 1);
					if(announcement.startsWith(":"))
						announcement = announcement.substring(1);	// this protects against people screwing up the emulinker supraclient
					
					
					int access = clientHandler.getUser().getServer().getAccessManager().getAccess(clientHandler.getUser().getSocketAddress().getAddress());
					if (access < AccessManager.ACCESS_SUPERADMIN && clientHandler.getUser().getServer().getAccessManager().isSilenced(clientHandler.getUser().getSocketAddress().getAddress())){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "You are silenced!"));} catch(Exception e) {}
						return;
					}
					
					String m = announcement;

					
					announcement = "*" + clientHandler.getUser().getName() + " " + m;
					
					for (KailleraUserImpl user : clientHandler.getUser().getGame().getPlayers()){
						user.getGame().announce(announcement, user);
					}
					return;
				}
				else if(((GameChat) message).getMessage().equals("/help")){	
					KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
					user.getGame().announce("/me <message> to make personal message eg. /me is bored ...SupraFast is bored.", user);
					try { Thread.sleep(20); } catch(Exception e) {}			
					user.getGame().announce("/msg <UserID> <msg> to PM somebody. /msgon or /msgoff to turn pm on | off.", user);
					try { Thread.sleep(20); } catch(Exception e) {}
					user.getGame().announce("/ignore <UserID> or /unignore <UserID> or /ignoreall or /unignoreall to ignore users.", user);
					try { Thread.sleep(20); } catch(Exception e) {}
					user.getGame().announce("/p2pon or /p2poff this option ignores all server activity during gameplay.", user);
					try { Thread.sleep(20); } catch(Exception e) {}
					return;
				}
				clientHandler.getUser().getGame().announce("Uknown Command: " + ((GameChat) message).getMessage(), clientHandler.getUser());
			}
			else{
				clientHandler.getUser().getGame().announce("Denied: Flood Control", clientHandler.getUser());
			}
	}
	
	public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler)
	{
		handledCount++;

		GameChatEvent gameChatEvent = (GameChatEvent) event;

		try
		{
			if(clientHandler.getUser().searchIgnoredUsers(gameChatEvent.getUser().getConnectSocketAddress().getAddress().getHostAddress()) == true)
				return;
			else if(clientHandler.getUser().getIgnoreAll() == true){
				if(gameChatEvent.getUser().getAccess() < AccessManager.ACCESS_ADMIN && gameChatEvent.getUser() != clientHandler.getUser())
					return;
			}
			
			String m = gameChatEvent.getMessage();

			
			clientHandler.send(new GameChat_Notification(clientHandler.getNextMessageNumber(), gameChatEvent.getUser().getName(), m));
		}
		catch (MessageFormatException e)
		{
			log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
		}
	}
}

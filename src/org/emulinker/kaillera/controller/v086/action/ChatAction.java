package org.emulinker.kaillera.controller.v086.action;

import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.exception.FloodException;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuLang;
import org.emulinker.util.WildcardStringPattern;

public class ChatAction implements V086Action, V086ServerEventHandler
{
	public static final String	ADMIN_COMMAND_ESCAPE_STRING	= "/";

	private static Log			log							= LogFactory.getLog(ChatAction.class);
	private static final String	desc						= "ChatAction";
	private static ChatAction	singleton					= new ChatAction();

	public static ChatAction getInstance()
	{
		return singleton;
	}

	private int	actionCount		= 0;
	private int	handledCount	= 0;

	private ChatAction()
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

		if(!(message instanceof Chat_Request))
			throw new FatalActionException("Received incorrect instance of Chat: " + message);
		
		if (((Chat) message).getMessage().startsWith(ADMIN_COMMAND_ESCAPE_STRING)){
			if(clientHandler.getUser().getAccess() > AccessManager.ACCESS_ELEVATED){
				try
				{
					if(AdminCommandAction.getInstance().isValidCommand(((Chat) message).getMessage())){
						AdminCommandAction.getInstance().performAction(message, clientHandler);
						if(((Chat) message).getMessage().equals("/help"))
							checkCommands(message, clientHandler);
					}
					else
						checkCommands(message, clientHandler);
				}
				catch (FatalActionException e)
				{
					log.warn("Admin command failed: " + e.getMessage());
				}
				return;
			}
			checkCommands(message, clientHandler);
			return;
		}
					
		
		actionCount++;

		try
		{
			clientHandler.getUser().chat(((Chat) message).getMessage());
		}
		catch (ActionException e)
		{
			log.info("Chat Denied: " + clientHandler.getUser() + ": " + ((Chat) message).getMessage());

			try
			{
				clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", EmuLang.getString("ChatAction.ChatDenied", e.getMessage())));
			}
			catch (MessageFormatException e2)
			{
				log.error("Failed to contruct InformationMessage message: " + e.getMessage(), e);
			}
		}
	}

	private void checkCommands(V086Message message, V086Controller.V086ClientHandler clientHandler) throws FatalActionException{
		boolean doCommand = true;
		KailleraUserImpl userN = (KailleraUserImpl) clientHandler.getUser();
		
		if(userN.getAccess() < AccessManager.ACCESS_ELEVATED){
			try{
				clientHandler.getUser().chat(":USER_COMMAND");
			}catch (ActionException e){
				doCommand = false;
			}
		}

			if(doCommand){
				//SF MOD - User Commands
				if(((Chat) message).getMessage().equals("/alivecheck")){
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", ":ALIVECHECK=EmulinkerSF Alive Check: You are still logged in."));} catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().equals("/version") && clientHandler.getUser().getAccess() < AccessManager.ACCESS_ADMIN){
					ReleaseInfo releaseInfo = clientHandler.getUser().getServer().getReleaseInfo();
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "VERSION: " + releaseInfo.getProductName() + ": " + releaseInfo.getVersionString() + ": " + releaseInfo.getReleaseDate()));} catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().equals("/myip")){
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "Your IP Address is: " + clientHandler.getUser().getConnectSocketAddress().getAddress().getHostAddress()));} catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().equals("/msgon")){
					clientHandler.getUser().setMsg(true);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "Private messages are now on."));} catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().equals("/msgoff")){
					clientHandler.getUser().setMsg(false);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "Private messages are now off."));} catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().startsWith("/me")){
					int space = ((Chat) message).getMessage().indexOf(' ');
					if (space < 0){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","Invalid # of Fields!")); } catch(Exception e) {}
						return;
					}
				
					String announcement = ((Chat) message).getMessage().substring(space + 1);
					if(announcement.startsWith(":"))
						announcement = announcement.substring(1);	// this protects against people screwing up the emulinker supraclient
					
					
					int access = clientHandler.getUser().getServer().getAccessManager().getAccess(clientHandler.getUser().getSocketAddress().getAddress());
					if (access < AccessManager.ACCESS_SUPERADMIN && clientHandler.getUser().getServer().getAccessManager().isSilenced(clientHandler.getUser().getSocketAddress().getAddress())){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "You are silenced!"));} catch(Exception e) {}
						return;
					}
					
					if(clientHandler.getUser().getServer().checkMe(clientHandler.getUser(), announcement)){
						String m = announcement;
					
						announcement = "*" + clientHandler.getUser().getName() + " " + m;
						KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
						clientHandler.getUser().getServer().announce(announcement, true, user1);
					}
				}
				else if(((Chat) message).getMessage().startsWith("/msg")){
					KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
					Scanner scanner = new Scanner(((Chat) message).getMessage()).useDelimiter(" "); //$NON-NLS-1$
					
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
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
							return;
						}
		
						if(user == clientHandler.getUser()){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't private message yourself!")); } catch(Exception e) {}
							return;					
						}
		
						if(user.getMsg() == false || user.searchIgnoredUsers(clientHandler.getUser().getConnectSocketAddress().getAddress().getHostAddress()) == true){
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
									try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e1) {}
									return;
								}
		
								if(user == clientHandler.getUser()){
									try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't private message yourself!")); } catch(Exception e1) {}
									return;					
								}
								
								if(user.getMsg() == false){
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
							}
							catch(Exception e1){
								try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","Private Message Error: /msg <UserID> <message>")); } catch(Exception e2) {}
								return;
							}
						}
						
						
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","Private Message Error: /msg <UserID> <message>")); } catch(Exception e1) {}
						return;
					}		
				}
				else if(((Chat) message).getMessage().equals("/ignoreall")){
					clientHandler.getUser().setIgnoreAll(true);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server",clientHandler.getUser().getName() + " is now ignoring everyone!")); } catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().equals("/unignoreall")){
					clientHandler.getUser().setIgnoreAll(false);
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server",clientHandler.getUser().getName() + " is now unignoring everyone!")); } catch(Exception e) {}
				}
				else if(((Chat) message).getMessage().startsWith("/ignore")){
					Scanner scanner = new Scanner(((Chat) message).getMessage()).useDelimiter(" "); //$NON-NLS-1$
		
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
					}
					catch (NoSuchElementException e)
					{
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
						user.getServer().announce("Ignore User Error: /ignore <UserID>", false, user); //$NON-NLS-1$ //$NON-NLS-2$
						log.info("IGNORE USER ERROR: " + user.getName() + ": " + clientHandler.getRemoteSocketAddress().getHostName());
						return;
					}
				}
				else if(((Chat) message).getMessage().startsWith("/unignore")){
					Scanner scanner = new Scanner(((Chat) message).getMessage()).useDelimiter(" "); //$NON-NLS-1$
		
					try
					{
						scanner.next();
						int userID = scanner.nextInt();
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);
										
						if (user == null){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
							return;
						}
						if(!clientHandler.getUser().findIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress())){
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","You can't unignore a user that isn't ignored!")); } catch(Exception e) {}
							return;
						}
		
						if(clientHandler.getUser().removeIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress(), false) == true)
							user.getServer().announce(clientHandler.getUser().getName() + " is now unignoring <" + user.getName() + "> ID: " + user.getID(), false, null); //$NON-NLS-1$ //$NON-NLS-2$
						else
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server","User Not Found!")); } catch(Exception e) {}
					}
					catch (NoSuchElementException e)
					{
						KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
						user.getServer().announce("Unignore User Error: /ignore <UserID>", false, user); //$NON-NLS-1$ //$NON-NLS-2$
						log.info("UNIGNORE USER ERROR: " + user.getName() + ": " + clientHandler.getRemoteSocketAddress().getHostName());
						return;
					}
		
				}
				else if(((Chat) message).getMessage().equals("/help")){	
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/me <message> to make personal message eg. /me is bored ...SupraFast is bored.")); } catch(Exception e) {}
					try { Thread.sleep(20); } catch(Exception e) {}
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/ignore <UserID> or /unignore <UserID> or /ignoreall or /unignoreall to ignore users.")); } catch(Exception e) {}
					try { Thread.sleep(20); } catch(Exception e) {}
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/msg <UserID> <msg> to PM somebody. /msgon or /msgoff to turn on | off.")); } catch(Exception e) {}
					try { Thread.sleep(20); } catch(Exception e) {}
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/myip to get your IP Address.")); } catch(Exception e) {}
					try { Thread.sleep(20); } catch(Exception e) {}
					try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/version to get server version.")); } catch(Exception e) {}
					try { Thread.sleep(20); } catch(Exception e) {}			
					if(clientHandler.getUser().getAccess() == AccessManager.ACCESS_MODERATOR){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/silence <UserID> <Min> to silence a user. 15min max.")); } catch(Exception e) {}
						try { Thread.sleep(20); } catch(Exception e) {}		
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/kick <UserID> to kick a user.")); } catch(Exception e) {}
						try { Thread.sleep(20); } catch(Exception e) {}	
					}
					
					if(clientHandler.getUser().getAccess() < AccessManager.ACCESS_ADMIN){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "/finduser <Nick> to get a user's info. eg. /finduser sup ...will return SupraFast info.")); } catch(Exception e) {}
						try { Thread.sleep(20); } catch(Exception e) {}
						return;
					}
				}
				else if(((Chat) message).getMessage().startsWith("/finduser") && clientHandler.getUser().getAccess() < AccessManager.ACCESS_ADMIN){
					int space = ((Chat) message).getMessage().indexOf(' ');
					if (space < 0){
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "Finduser Error: /finduser <nick> eg. /finduser sup ...will return SupraFast info.")); } catch(Exception e) {}
						return;
					}
					int foundCount = 0;
					String str =(((Chat) message).getMessage().substring(space + 1));
					//WildcardStringPattern pattern = new WildcardStringPattern
					for (KailleraUserImpl user : clientHandler.getUser().getUsers())
					{
						if (!user.isLoggedIn())
							continue;
		
						if (user.getName().toLowerCase().contains(str.toLowerCase()))
						{
							StringBuilder sb = new StringBuilder();
							sb.append("UserID: "); //$NON-NLS-1$
							sb.append(user.getID());
							sb.append(", Nick: < "); //$NON-NLS-1$
							sb.append(user.getName());
							sb.append(">"); //$NON-NLS-1$
							sb.append(", Access: ");
							if(user.getAccessStr().equals("SuperAdmin") || user.getAccessStr().equals("Admin")){
								sb.append("Normal");
							}
							else{
								sb.append(user.getAccessStr());
							}
							
							if(user.getGame() != null){
								sb.append(", GameID: "); //$NON-NLS-1$
								sb.append(user.getGame().getID());
								sb.append(", Game: "); //$NON-NLS-1$
								sb.append(user.getGame().getRomName());
							}
							try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", sb.toString())); } catch(Exception e) {}
							foundCount++;
						}
					}
		
					if (foundCount == 0)
						try {clientHandler.send(new InformationMessage(clientHandler.getNextMessageNumber(), "server", "No Users Found!")); } catch(Exception e) {}					
				}
				else
					userN.getServer().announce("Unknown Command: " + ((Chat) message).getMessage(), false, userN);
			}
			else{
				userN.getServer().announce("Denied: Flood Control", false, userN);
			}
	}

	
	
	public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler)
	{
		handledCount++;

		ChatEvent chatEvent = (ChatEvent) event;
		
		try
		{
			if(clientHandler.getUser().searchIgnoredUsers(chatEvent.getUser().getConnectSocketAddress().getAddress().getHostAddress()) == true)
				return;
			else if(clientHandler.getUser().getIgnoreAll() == true){
				if(chatEvent.getUser().getAccess() < AccessManager.ACCESS_ADMIN && chatEvent.getUser() != clientHandler.getUser())
					return;
			}
			
			String m = chatEvent.getMessage();
		
		
			clientHandler.send(new Chat_Notification(clientHandler.getNextMessageNumber(), chatEvent.getUser().getName(), m));
		}
		catch (MessageFormatException e)
		{
			log.error("Failed to contruct Chat_Notification message: " + e.getMessage(), e);
		}
	}
}

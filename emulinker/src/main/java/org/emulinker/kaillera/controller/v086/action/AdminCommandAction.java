package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import java.net.InetAddress;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.impl.*;
import org.emulinker.release.*;
import org.emulinker.util.*;

@Singleton
public class AdminCommandAction implements V086Action<Chat> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final String COMMAND_ANNOUNCE = "/announce";
  public static final String COMMAND_ANNOUNCEALL = "/announceall";
  public static final String COMMAND_ANNOUNCEGAME = "/announcegame";
  public static final String COMMAND_BAN = "/ban";
  public static final String COMMAND_CLEAR = "/clear";
  public static final String COMMAND_CLOSEGAME = "/closegame";
  public static final String COMMAND_FINDGAME = "/findgame";
  public static final String COMMAND_FINDUSER = "/finduser";
  public static final String COMMAND_HELP = "/help";
  public static final String COMMAND_KICK = "/kick";
  public static final String COMMAND_SILENCE = "/silence";
  public static final String COMMAND_TEMPADMIN = "/tempadmin";
  public static final String COMMAND_VERSION = "/version";
  public static final String COMMAND_TRIVIA = "/trivia";

  // SF MOD
  public static final String COMMAND_STEALTH = "/stealth";
  public static final String COMMAND_TEMPELEVATED = "/tempelevated";
  public static final String COMMAND_TEMPMODERATOR = "/tempmoderator";
  private static final String DESC = "AdminCommandAction";

  private int actionCount = 0;

  @Inject
  AdminCommandAction() {}

  @Override
  public int getActionPerformedCount() {
    return actionCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  public boolean isValidCommand(String chat) {
    if (chat.startsWith(COMMAND_HELP)) {
      return true;
    } else if (chat.startsWith(COMMAND_FINDUSER)) {
      return true;
    } else if (chat.startsWith(COMMAND_FINDGAME)) {
      return true;
    } else if (chat.startsWith(COMMAND_CLOSEGAME)) {
      return true;
    } else if (chat.startsWith(COMMAND_KICK)) {
      return true;
    } else if (chat.startsWith(COMMAND_BAN)) {
      return true;
    } else if (chat.startsWith(COMMAND_TEMPELEVATED)) {
      return true;
    } else if (chat.startsWith(COMMAND_TEMPMODERATOR)) {
      return true;
    } else if (chat.startsWith(COMMAND_SILENCE)) {
      return true;
    } else if (chat.startsWith(COMMAND_ANNOUNCEGAME)) {
      return true;
    } else if (chat.startsWith(COMMAND_ANNOUNCE)) {
      return true;
    } else if (chat.startsWith(COMMAND_TEMPADMIN)) {
      return true;
    } else if (chat.startsWith(COMMAND_VERSION)) {
      return true;
    } else if (chat.startsWith(COMMAND_CLEAR)) {
      return true;
    } else if (chat.startsWith(COMMAND_STEALTH)) {
      return true;
    } else if (chat.startsWith(COMMAND_TRIVIA)) {
      return true;
    }
    return false;
  }

  @Override
  public void performAction(Chat chatMessage, V086ClientHandler clientHandler)
      throws FatalActionException {
    String chat = chatMessage.message();
    KailleraServerImpl server = (KailleraServerImpl) clientHandler.getController().getServer();
    AccessManager accessManager = server.getAccessManager();
    KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();

    if (accessManager.getAccess(clientHandler.getRemoteInetAddress())
        < AccessManager.ACCESS_ADMIN) {
      if (chat.startsWith(COMMAND_SILENCE)
          || chat.startsWith(COMMAND_KICK)
          || chat.startsWith(COMMAND_HELP)
          || chat.startsWith(COMMAND_FINDUSER)
          || chat.startsWith(COMMAND_VERSION)
              && accessManager.getAccess(clientHandler.getRemoteInetAddress())
                  > AccessManager.ACCESS_ELEVATED) {
        // SF MOD - Moderators can silence and Kick
        // DO NOTHING
      } else {
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "Admin Command Error: You are not an admin!"));
        } catch (MessageFormatException e) {
        }
        throw new FatalActionException(
            "Admin Command Denied: " + user + " does not have Admin access: " + chat);
      }
    }

    logger.atInfo().log(user + ": Admin Command: " + chat);

    try {
      if (chat.startsWith(COMMAND_HELP)) {
        processHelp(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_FINDUSER)) {
        processFindUser(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_FINDGAME)) {
        processFindGame(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_CLOSEGAME)) {
        processCloseGame(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_KICK)) {
        processKick(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_BAN)) {
        processBan(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_TEMPELEVATED)) {
        processTempElevated(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_TEMPMODERATOR)) {
        processTempModerator(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_SILENCE)) {
        processSilence(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_ANNOUNCEGAME)) {
        processGameAnnounce(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_ANNOUNCE)) {
        processAnnounce(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_TEMPADMIN)) {
        processTempAdmin(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_VERSION)) {
        processVersion(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_CLEAR)) {
        processClear(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_STEALTH)) {
        processStealth(chat, server, user, clientHandler);
      } else if (chat.startsWith(COMMAND_TRIVIA)) {
        processTrivia(chat, server, user, clientHandler);
      } else throw new ActionException("Invalid Command: " + chat);
    } catch (ActionException e) {
      logger.atSevere().withCause(e).log("Admin Command Failed: " + user + ": " + chat);

      try {
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                EmuLang.getString("AdminCommandAction.Failed", e.getMessage())));
      } catch (MessageFormatException e2) {
        logger.atSevere().withCause(e2).log("Failed to contruct InformationMessage message");
      }
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct message");
    }
  }

  private void processHelp(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (admin.getAccess() == AccessManager.ACCESS_MODERATOR) return;
    // clientHandler.send(InformationMessage.create(clientHandler.getNextMessageNumber(), "server",
    // EmuLang.getString("AdminCommandAction.AdminCommands")));
    // try { Thread.sleep(20); } catch(Exception e) {}
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpVersion")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpKick")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpSilence")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpBan")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }

    if (admin.getAccess() == AccessManager.ACCESS_ADMIN) {
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              EmuLang.getString("AdminCommandAction.HelpClear")));
      try {
        Thread.sleep(20);
      } catch (Exception e) {
      }
    }

    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpCloseGame")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpAnnounce")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpAnnounceAll")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpAnnounceGame")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpFindUser")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            EmuLang.getString("AdminCommandAction.HelpFindGame")));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            "/triviaon to start the trivia bot- /triviapause to pause the bot- /triviaresume to resume the bot after pause- /triviasave to save the bot's scores- /triviatime <#> to change the question delay"));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            "/triviaoff to stop the bot- /triviascores to show top 3 scores- /triviawin to show a winner- /triviaupdate <IP Address> <New IP Address> to update ip address"));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    clientHandler.send(
        InformationMessage.create(
            clientHandler.getNextMessageNumber(),
            "server",
            "/stealthon /stealthoff to join a room invisibly."));
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }

    if (admin.getAccess() == AccessManager.ACCESS_SUPERADMIN) {
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              "/tempelevated <UserID> <min> to give a user temporary elevated access."));
      try {
        Thread.sleep(20);
      } catch (Exception e) {
      }
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              "/tempmoderator <UserID> <min> to give a user temporary moderator access."));
      try {
        Thread.sleep(20);
      } catch (Exception e) {
      }
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              EmuLang.getString("AdminCommandAction.HelpTempAdmin")));
      try {
        Thread.sleep(20);
      } catch (Exception e) {
      }
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              "/clear <IP Address> to remove any temp ban, silence, elevated, moderator or admin."));
      try {
        Thread.sleep(20);
      } catch (Exception e) {
      }
    }
  }

  private void processFindUser(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    int space = message.indexOf(' ');
    if (space < 0) throw new ActionException(EmuLang.getString("AdminCommandAction.FindUserError"));

    int foundCount = 0;
    String str = (message.substring(space + 1));
    // WildcardStringPattern pattern = new WildcardStringPattern

    for (KailleraUserImpl user : server.getUsers()) {
      if (!user.isLoggedIn()) continue;

      if (user.getName().toLowerCase().contains(str.toLowerCase())) {
        StringBuilder sb = new StringBuilder();
        sb.append("UserID: ");
        sb.append(user.getID());
        sb.append(", IP: ");
        sb.append(user.getConnectSocketAddress().getAddress().getHostAddress());
        sb.append(", Nick: <");
        sb.append(user.getName());
        sb.append(">, Access: ");
        sb.append(user.getAccessStr());
        if (user.getGame() != null) {
          sb.append(", GameID: ");
          sb.append(user.getGame().getID());
          sb.append(", Game: ");
          sb.append(user.getGame().getRomName());
        }

        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(), "server", sb.toString()));
        foundCount++;
      }
    }

    if (foundCount == 0)
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              EmuLang.getString("AdminCommandAction.NoUsersFound")));
  }

  private void processFindGame(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    int space = message.indexOf(' ');
    if (space < 0) throw new ActionException(EmuLang.getString("AdminCommandAction.FindGameError"));

    int foundCount = 0;
    WildcardStringPattern pattern = new WildcardStringPattern(message.substring(space + 1));
    for (KailleraGameImpl game : server.getGames()) {
      if (pattern.match(game.getRomName())) {
        StringBuilder sb = new StringBuilder();
        sb.append("GameID: ");
        sb.append(game.getID());
        sb.append(", Owner: <");
        sb.append(game.getOwner().getName());
        sb.append(">, Game: ");
        sb.append(game.getRomName());
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(), "server", sb.toString()));
        foundCount++;
      }
    }

    if (foundCount == 0)
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              EmuLang.getString("AdminCommandAction.NoGamesFound")));
  }

  private void processSilence(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int userID = scanner.nextInt();
      int minutes = scanner.nextInt();

      KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
      if (user == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", +userID));

      if (user.getID() == admin.getID())
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotSilenceSelf"));

      int access = server.getAccessManager().getAccess(user.getConnectSocketAddress().getAddress());
      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN)
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotSilenceAdmin"));

      if (access == AccessManager.ACCESS_MODERATOR
          && admin.getAccess() == AccessManager.ACCESS_MODERATOR)
        throw new ActionException("You cannot silence a moderator if you're not an admin!");

      if (admin.getAccess() == AccessManager.ACCESS_MODERATOR) {
        if (server.getAccessManager().isSilenced(user.getSocketAddress().getAddress()))
          throw new ActionException(
              "This User has already been Silenced.  Please wait until his time expires.");
        if (minutes > 15)
          throw new ActionException("Moderators can only silence up to 15 minutes!");
      }

      server
          .getAccessManager()
          .addSilenced(user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
      server.announce(
          EmuLang.getString("AdminCommandAction.Silenced", minutes, user.getName()), false, null);
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.SilenceError"));
    }
  }

  private void processKick(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int userID = scanner.nextInt();

      KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
      if (user == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID));

      if (user.getID() == admin.getID())
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotKickSelf"));

      int access = server.getAccessManager().getAccess(user.getConnectSocketAddress().getAddress());

      if (access == AccessManager.ACCESS_MODERATOR
          && admin.getAccess() == AccessManager.ACCESS_MODERATOR)
        throw new ActionException("You cannot kick a moderator if you're not an admin!");

      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN)
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotKickAdmin"));

      user.quit(EmuLang.getString("AdminCommandAction.QuitKicked"));
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.KickError"));
    }
  }

  private void processCloseGame(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int gameID = scanner.nextInt();

      KailleraGameImpl game = (KailleraGameImpl) server.getGame(gameID);
      if (game == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.GameNotFound", gameID));

      KailleraUserImpl owner = (KailleraUserImpl) game.getOwner();
      int access =
          server.getAccessManager().getAccess(owner.getConnectSocketAddress().getAddress());

      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN
          && owner.isLoggedIn())
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotCloseAdminGame"));

      owner.quitGame();
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.CloseGameError"));
    }
  }

  private void processBan(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int userID = scanner.nextInt();
      int minutes = scanner.nextInt();

      KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
      if (user == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID));

      if (user.getID() == admin.getID())
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotBanSelf"));

      int access = server.getAccessManager().getAccess(user.getConnectSocketAddress().getAddress());
      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN)
        throw new ActionException(EmuLang.getString("AdminCommandAction.CanNotBanAdmin"));

      server.announce(
          EmuLang.getString("AdminCommandAction.Banned", minutes, user.getName()), false, null);
      user.quit(EmuLang.getString("AdminCommandAction.QuitBanned"));
      server
          .getAccessManager()
          .addTempBan(user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.BanError"));
    }
  }

  private void processTempElevated(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (admin.getAccess() != AccessManager.ACCESS_SUPERADMIN) {
      throw new ActionException("Only SUPER ADMIN's can give Temp Elevated Status!");
    }

    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int userID = scanner.nextInt();
      int minutes = scanner.nextInt();

      KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
      if (user == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID));

      if (user.getID() == admin.getID())
        throw new ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin"));

      int access = server.getAccessManager().getAccess(user.getConnectSocketAddress().getAddress());
      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin"));
      else if (access == AccessManager.ACCESS_ELEVATED)
        throw new ActionException("User is already elevated.");

      server
          .getAccessManager()
          .addTempElevated(user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
      server.announce(
          "Temp Elevated Granted: " + user.getName() + " for " + minutes + "min", false, null);
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("Temp Elevated Error."));
    }
  }

  // new superadmin command /tempmoderator
  private void processTempModerator(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (admin.getAccess() != AccessManager.ACCESS_SUPERADMIN) {
      throw new ActionException("Only SUPER ADMIN's can give Temp Moderator Status!");
    }

    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int userID = scanner.nextInt();
      int minutes = scanner.nextInt();

      KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
      if (user == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID));

      if (user.getID() == admin.getID())
        throw new ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin"));

      int access = server.getAccessManager().getAccess(user.getConnectSocketAddress().getAddress());
      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin"));
      else if (access == AccessManager.ACCESS_MODERATOR)
        throw new ActionException("User is already moderator.");

      server
          .getAccessManager()
          .addTempModerator(user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
      server.announce(
          "Temp Moderator Granted: " + user.getName() + " for " + minutes + "min.", false, null);
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("Temp Moderator Error."));
    }
  }

  private void processTempAdmin(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (admin.getAccess() != AccessManager.ACCESS_SUPERADMIN) {
      throw new ActionException("Only SUPER ADMIN's can give Temp Admin Status!");
    }

    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int userID = scanner.nextInt();
      int minutes = scanner.nextInt();

      KailleraUserImpl user = (KailleraUserImpl) server.getUser(userID);
      if (user == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID));

      if (user.getID() == admin.getID())
        throw new ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin"));

      int access = server.getAccessManager().getAccess(user.getConnectSocketAddress().getAddress());
      if (access >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN)
        throw new ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin"));

      server
          .getAccessManager()
          .addTempAdmin(user.getConnectSocketAddress().getAddress().getHostAddress(), minutes);
      server.announce(
          EmuLang.getString("AdminCommandAction.TempAdminGranted", minutes, user.getName()),
          false,
          null);
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.TempAdminError"));
    }
  }

  private void processStealth(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (admin.getGame() != null)
      throw new ActionException("Can't use /stealth while in a gameroom.");

    if (message.equals("/stealthon")) {
      admin.setStealth(true);
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(), "server", "Stealth Mode is on."));
    } else if (message.equals("/stealthoff")) {
      admin.setStealth(false);
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(), "server", "Stealth Mode is off."));
    } else throw new ActionException("Stealth Mode Error: /stealthon /stealthoff");
  }

  private void processTrivia(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (message.equals("/triviareset")) {
      if (server.getSwitchTrivia()) {
        server.getTrivia().saveScores(true);
        server.getTriviaThread().stop();
      }

      server.announce("<Trivia> " + "SupraTrivia has been reset!", false, null);
      Trivia trivia = new Trivia(server);
      Thread triviaThread = new Thread(trivia);
      triviaThread.start();
      server.setTriviaThread(triviaThread);
      server.setTrivia(trivia);
      trivia.setTriviaPaused(false);
    } else if (message.equals("/triviaon")) {
      if (server.getSwitchTrivia() == true) throw new ActionException("Trivia already started!");
      server.announce("SupraTrivia has been started!", false, null);
      Trivia trivia = new Trivia(server);
      Thread triviaThread = new Thread(trivia);
      triviaThread.start();
      server.setTriviaThread(triviaThread);
      server.setTrivia(trivia);
      trivia.setTriviaPaused(false);
    } else if (message.equals("/triviaoff")) {
      if (server.getTrivia() == null)
        throw new ActionException("Trivia needs to be started first!");
      server.announce("SupraTrivia has been stopped!", false, null);
      server.getTrivia().saveScores(false);
      server.getTriviaThread().stop();
      server.setSwitchTrivia(false);
      server.setTrivia(null);
    } else if (message.equals("/triviapause")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }

      server.getTrivia().setTriviaPaused(true);
      server.announce("<Trivia> " + "SupraTrivia will be paused after this question!", false, null);
    } else if (message.equals("/triviaresume")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }

      server.getTrivia().setTriviaPaused(false);
      server.announce("<Trivia> " + "SupraTrivia has been resumed!", false, null);
    } else if (message.equals("/triviasave")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }

      server.getTrivia().saveScores(true);
    } else if (message.equals("/triviascores")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }
      server.getTrivia().displayHighScores(false);
    } else if (message.equals("/triviawin")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }
      server.getTrivia().displayHighScores(true);
    } else if (message.startsWith("/triviaupdate")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }

      Scanner scanner = new Scanner(message).useDelimiter(" ");

      try {
        scanner.next();
        String ip = scanner.next();
        String ip_update = scanner.next();

        if (server.getTrivia().updateIP(ip, ip_update)) {
          server.announce(
              "<Trivia> " + ip_update.subSequence(0, 4) + ".... Trivia IP was updated!",
              false,
              admin);
        } else {
          server.announce(
              "<Trivia> " + ip.subSequence(0, 4) + " was not found!  Error updating score!",
              false,
              admin);
        }
      } catch (Exception e) {
        throw new ActionException("Invalid Trivia Score Update!");
      }

    } else if (message.startsWith("/triviatime")) {
      if (server.getTrivia() == null) {
        throw new ActionException("Trivia needs to be started first!");
      }

      Scanner scanner = new Scanner(message).useDelimiter(" ");

      try {
        scanner.next();
        int questionTime = scanner.nextInt();
        server.getTrivia().setQuestionTime(questionTime * 1000);
        server.announce(
            "<Trivia> " + "SupraTrivia's question delay has been changed to " + questionTime + "s!",
            false,
            admin);
      } catch (Exception e) {
        throw new ActionException("Invalid Trivia Time!");
      }
    }
  }

  private void processAnnounce(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    int space = message.indexOf(' ');
    if (space < 0) throw new ActionException(EmuLang.getString("AdminCommandAction.AnnounceError"));

    boolean all = false;
    if (message.startsWith(COMMAND_ANNOUNCEALL)) {
      all = true;
    }

    String announcement = message.substring(space + 1);
    if (announcement.startsWith(":"))
      announcement =
          announcement.substring(
              1); // this protects against people screwing up the emulinker supraclient

    server.announce(announcement, all, null);
  }

  private void processGameAnnounce(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      scanner.next();
      int gameID = scanner.nextInt();

      StringBuilder sb = new StringBuilder();
      while (scanner.hasNext()) {
        sb.append(scanner.next());
        sb.append(" ");
      }

      KailleraGameImpl game = (KailleraGameImpl) server.getGame(gameID);
      if (game == null)
        throw new ActionException(EmuLang.getString("AdminCommandAction.GameNotFound", gameID));

      game.announce(sb.toString(), null);
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.AnnounceGameError"));
    }
  }

  private void processClear(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    int space = message.indexOf(' ');
    if (space < 0) throw new ActionException(EmuLang.getString("AdminCommandAction.ClearError"));

    String addressStr = message.substring(space + 1);
    InetAddress inetAddr = null;
    try {
      inetAddr = InetAddress.getByName(addressStr);
    } catch (Exception e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.ClearAddressFormatError"));
    }

    if (admin.getAccess() == AccessManager.ACCESS_SUPERADMIN
            && server.getAccessManager().clearTemp(inetAddr, true)
        || admin.getAccess() == AccessManager.ACCESS_ADMIN
            && server.getAccessManager().clearTemp(inetAddr, false))
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              EmuLang.getString("AdminCommandAction.ClearSuccess")));
    else
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              EmuLang.getString("AdminCommandAction.ClearNotFound")));
  }

  private void processVersion(
      String message,
      KailleraServerImpl server,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    try {
      ReleaseInfo releaseInfo = server.getReleaseInfo();
      clientHandler.send(
          InformationMessage.create(
              clientHandler.getNextMessageNumber(),
              "server",
              "VERSION: "
                  + releaseInfo.getProductName()
                  + ": "
                  + releaseInfo.getVersionString()
                  + ": "
                  + EmuUtil.toSimpleUtcDatetime(releaseInfo.getBuildDate())));
      sleep(20);
      if (admin.getAccess() >= AccessManager.ACCESS_ADMIN) {
        Properties props = System.getProperties();
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "JAVAVER: " + props.getProperty("java.version")));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "JAVAVEND: " + props.getProperty("java.vendor")));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "OSNAME: " + props.getProperty("os.name")));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "OSARCH: " + props.getProperty("os.arch")));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "OSVER: " + props.getProperty("os.version")));
        sleep(20);

        Runtime runtime = Runtime.getRuntime();
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "NUMPROCS: " + runtime.availableProcessors()));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "FREEMEM: " + runtime.freeMemory()));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(), "server", "MAXMEM: " + runtime.maxMemory()));
        sleep(20);
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                "TOTMEM: " + runtime.totalMemory()));
        sleep(20);

        Map<String, String> env = System.getenv();

        if (EmuUtil.systemIsWindows()) {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "COMPNAME: " + env.get("COMPUTERNAME")));
          sleep(20);
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(), "server", "USER: " + env.get("USERNAME")));
          sleep(20);
        } else {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "COMPNAME: " + env.get("HOSTNAME")));
          sleep(20);
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(), "server", "USER: " + env.get("USERNAME")));
          sleep(20);
        }
      }
    } catch (NoSuchElementException e) {
      throw new ActionException(EmuLang.getString("AdminCommandAction.VersionError"));
    }
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (Exception e) {
    }
  }
}

package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.impl.*;
import org.emulinker.util.EmuLang;

@Singleton
public class GameOwnerCommandAction implements V086Action<GameChat> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final String COMMAND_HELP = "/help";
  public static final String COMMAND_DETECTAUTOFIRE = "/detectautofire";

  // SF MOD
  public static final String COMMAND_LAGSTAT = "/lag";
  public static final String COMMAND_MAXUSERS = "/maxusers";
  public static final String COMMAND_MAXPING = "/maxping";
  public static final String COMMAND_START = "/start";
  public static final String COMMAND_STARTN = "/startn";
  public static final String COMMAND_MUTE = "/mute";
  public static final String COMMAND_UNMUTE = "/unmute";
  public static final String COMMAND_SWAP = "/swap";
  public static final String COMMAND_KICK = "/kick";
  public static final String COMMAND_EMU = "/setemu";
  public static final String COMMAND_CONN = "/setconn";
  public static final String COMMAND_SAMEDELAY = "/samedelay";
  public static final String COMMAND_NUM = "/num";

  private static long lastMaxUserChange = 0;
  private static final String DESC = "GameOwnerCommandAction";

  private int actionCount = 0;

  @Inject
  GameOwnerCommandAction() {}

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
    } else if (chat.startsWith(COMMAND_DETECTAUTOFIRE)) {
      return true;
    } else if (chat.startsWith(COMMAND_MAXUSERS)) {
      return true;
    } else if (chat.startsWith(COMMAND_MAXPING)) {
      return true;
    } else if (chat.equals(COMMAND_START)) {
      return true;
    } else if (chat.startsWith(COMMAND_STARTN)) {
      return true;
    } else if (chat.startsWith(COMMAND_MUTE)) {
      return true;
    } else if (chat.startsWith(COMMAND_EMU)) {
      return true;
    } else if (chat.startsWith(COMMAND_CONN)) {
      return true;
    } else if (chat.startsWith(COMMAND_UNMUTE)) {
      return true;
    } else if (chat.startsWith(COMMAND_SWAP)) {
      return true;
    } else if (chat.startsWith(COMMAND_KICK)) {
      return true;
    } else if (chat.startsWith(COMMAND_SAMEDELAY)) {
      return true;
    } else if (chat.startsWith(COMMAND_LAGSTAT)) {
      return true;
    } else if (chat.startsWith(COMMAND_NUM)) {
      return true;
    }
    return false;
  }

  @Override
  public void performAction(GameChat chatMessage, V086ClientHandler clientHandler)
      throws FatalActionException {
    String chat = chatMessage.message();

    KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
    KailleraGameImpl game = user.getGame();

    if (game == null) {
      throw new FatalActionException("GameOwner Command Failed: Not in a game: " + chat);
    }

    if (!user.equals(game.getOwner()) && user.getAccess() < AccessManager.ACCESS_SUPERADMIN) {
      if (chat.startsWith(COMMAND_HELP)) {

      } else {
        logger.atWarning().log(
            "GameOwner Command Denied: Not game owner: " + game + ": " + user + ": " + chat);
        game.announce("GameOwner Command Error: You are not an owner!", user);
        return;
      }
    }

    try {
      if (chat.startsWith(COMMAND_HELP)) {
        processHelp(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_DETECTAUTOFIRE)) {
        processDetectAutoFire(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_MAXUSERS)) {
        processMaxUsers(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_MAXPING)) {
        processMaxPing(chat, game, user, clientHandler);
      } else if (chat.equals(COMMAND_START)) {
        processStart(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_STARTN)) {
        processStartN(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_MUTE)) {
        processMute(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_EMU)) {
        processEmu(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_CONN)) {
        processConn(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_UNMUTE)) {
        processUnmute(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_SWAP)) {
        processSwap(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_KICK)) {
        processKick(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_LAGSTAT)) {
        processLagstat(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_SAMEDELAY)) {
        processSameDelay(chat, game, user, clientHandler);
      } else if (chat.startsWith(COMMAND_NUM)) {
        processNum(chat, game, user, clientHandler);
      } else {
        game.announce("Unknown Command: " + chat, user);
        logger.atInfo().log("Unknown GameOwner Command: " + game + ": " + user + ": " + chat);
      }
    } catch (ActionException e) {
      logger.atInfo().withCause(e).log(
          "GameOwner Command Failed: " + game + ": " + user + ": " + chat);
      game.announce(
          EmuLang.getString("GameOwnerCommandAction.CommandFailed", e.getMessage()), user);
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct message");
    }
  }

  private void processHelp(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (!admin.equals(game.getOwner()) && admin.getAccess() < AccessManager.ACCESS_SUPERADMIN)
      return;
    // game.setIndividualGameAnnounce(admin.getPlayerNumber());
    // game.announce(EmuLang.getString("GameOwnerCommandAction.AvailableCommands"));
    // try { Thread.sleep(20); } catch(Exception e) {}
    game.announce(EmuLang.getString("GameOwnerCommandAction.SetAutofireDetection"), admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/maxusers <#> to set capacity of room", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/maxping <#> to set maximum ping for room", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/start or /startn <#> start game when n players are joined.", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/mute /unmute  <UserID> or /muteall or /unmuteall to mute player(s).", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce(
        "/swap <order> eg. 123..n {n = total # of players; Each slot = new player#}", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/kick <Player#> or /kickall to kick a player(s).", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/setemu To restrict the gameroom to this emulator!", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce("/setconn To restrict the gameroom to this connection type!", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce(
        "/lagstat To check who has the most lag spikes or /lagreset to reset lagstat!", admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce(
        "/samedelay {true | false} to play at the same delay as player with highest ping. Default is false.",
        admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
  }

  private void autoFireHelp(KailleraGameImpl game, KailleraUserImpl admin) {
    int cur = game.getAutoFireDetector().getSensitivity();
    game.announce(EmuLang.getString("GameOwnerCommandAction.HelpSensitivity"), admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce(EmuLang.getString("GameOwnerCommandAction.HelpDisable"), admin);
    try {
      Thread.sleep(20);
    } catch (Exception e) {
    }
    game.announce(
        EmuLang.getString("GameOwnerCommandAction.HelpCurrentSensitivity", cur)
            + (cur == 0 ? (EmuLang.getString("GameOwnerCommandAction.HelpDisabled")) : ""),
        admin);
  }

  private void processDetectAutoFire(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (game.getStatus() != KailleraGame.STATUS_WAITING) {
      game.announce(EmuLang.getString("GameOwnerCommandAction.AutoFireChangeDeniedInGame"), admin);
      return;
    }

    StringTokenizer st = new StringTokenizer(message, " ");
    if (st.countTokens() != 2) {
      autoFireHelp(game, admin);
      return;
    }

    String command = st.nextToken();
    String sensitivityStr = st.nextToken();
    int sensitivity = -1;

    try {
      sensitivity = Integer.parseInt(sensitivityStr);
    } catch (NumberFormatException e) {
    }

    if (sensitivity > 5 || sensitivity < 0) {
      autoFireHelp(game, admin);
      return;
    }

    game.getAutoFireDetector().setSensitivity(sensitivity);
    game.announce(
        EmuLang.getString("GameOwnerCommandAction.HelpCurrentSensitivity", sensitivity)
            + (sensitivity == 0 ? (EmuLang.getString("GameOwnerCommandAction.HelpDisabled")) : ""),
        null);
  }

  private void processEmu(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    String emu = game.getOwner().getClientType();

    if (message.equals("/setemu any")) {
      emu = "any";
    }

    admin.getGame().setAEmulator(emu);
    admin.getGame().announce("Owner has restricted the emulator to: " + emu, null);
    return;
  }

  // new gameowner command /setconn
  private void processConn(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    String conn = KailleraUser.CONNECTION_TYPE_NAMES[game.getOwner().getConnectionType()];

    if (message.equals("/setconn any")) {
      conn = "any";
    }

    admin.getGame().setAConnection(conn);
    admin.getGame().announce("Owner has restricted the connection type to: " + conn, null);
    return;
  }

  private void processNum(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    admin.getGame().announce(game.getNumPlayers() + " in the room!", admin);
  }

  private void processLagstat(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (game.getStatus() != KailleraGame.STATUS_PLAYING)
      game.announce("Lagstat is only available during gameplay!", admin);

    if (message.equals("/lagstat")) {
      String str = "";
      for (KailleraUser player : game.getPlayers()) {
        if (!player.getStealth())
          str = str + "P" + player.getPlayerNumber() + ": " + player.getTimeouts() + ", ";
      }
      if (str.length() > 0) {
        str = str.substring(0, str.length() - ", ".length());
        game.announce(str + " lag spikes", null);
      }
    } else if (message.equals("/lagreset")) {
      for (KailleraUser player : game.getPlayers()) {
        player.setTimeouts(0);
      }

      game.announce("LagStat has been reset!", null);
    }
  }

  private void processSameDelay(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    if (message.equals("/samedelay true")) {
      game.setSameDelay(true);
      admin
          .getGame()
          .announce("Players will have the same delay when game starts (restarts)!", null);
    } else {
      game.setSameDelay(false);
      admin
          .getGame()
          .announce("Players will have independent delays when game starts (restarts)!", null);
    }
  }

  private void processMute(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      String str = scanner.next();
      if (str.equals("/muteall")) {
        for (int w = 1; w <= game.getPlayers().size(); w++) {
          // do not mute owner or admin
          if (game.getPlayer(w).getAccess() < AccessManager.ACCESS_ADMIN
              && !game.getPlayer(w).equals(game.getOwner())) {
            game.getPlayer(w).setMute(true);
            game.getMutedUsers()
                .add(game.getPlayer(w).getConnectSocketAddress().getAddress().getHostAddress());
          }
        }
        admin.getGame().announce("All players have been muted!", null);
        return;
      }

      int userID = scanner.nextInt();
      KailleraUserImpl user =
          (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);

      if (user == null) {
        admin.getGame().announce("Player doesn't exist!", admin);
        return;
      }

      if (user == clientHandler.getUser()) {
        user.getGame().announce("You can't mute yourself!", admin);
        return;
      }

      if (user.getAccess() >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN) {
        user.getGame().announce("You can't mute an Admin", admin);
        return;
      }

      // mute by IP
      game.getMutedUsers().add(user.getConnectSocketAddress().getAddress().getHostAddress());
      user.setMute(true);
      KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
      user1.getGame().announce(user.getName() + " has been muted!", null);
    } catch (NoSuchElementException e) {
      KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
      user.getGame().announce("Mute Player Error: /mute <UserID>", admin);
    }
  }

  private void processUnmute(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      String str = scanner.next();
      if (str.equals("/unmuteall")) {
        for (int w = 1; w <= game.getPlayers().size(); w++) {
          game.getPlayer(w).setMute(false);
          game.getMutedUsers()
              .remove(game.getPlayer(w).getConnectSocketAddress().getAddress().getHostAddress());
        }
        admin.getGame().announce("All players have been unmuted!", null);
        return;
      }

      int userID = scanner.nextInt();
      KailleraUserImpl user =
          (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);

      if (user == null) {
        admin.getGame().announce("Player doesn't exist!", admin);
        return;
      }

      if (user == clientHandler.getUser()) {
        user.getGame().announce("You can't unmute yourself!", admin);
        return;
      }

      if (user.getAccess() >= AccessManager.ACCESS_ADMIN
          && admin.getAccess() != AccessManager.ACCESS_SUPERADMIN) {
        user.getGame().announce("You can't unmute an Admin", admin);
        return;
      }

      game.getMutedUsers().remove(user.getConnectSocketAddress().getAddress().getHostAddress());
      user.setMute(false);
      KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
      user1.getGame().announce(user.getName() + " has been unmuted!", null);
    } catch (NoSuchElementException e) {
      KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
      user.getGame().announce("Unmute Player Error: /unmute <UserID>", admin);
    }
  }

  private void processStartN(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");
    try {
      scanner.next();
      int num = scanner.nextInt();

      if (num > 0 && num < 101) {
        game.setStartN((byte) num);
        game.announce("This game will start when " + num + " players have joined.", null);
      } else {
        game.announce("StartN Error: Enter value between 1 and 100.", admin);
      }
    } catch (NoSuchElementException e) {
      game.announce("Failed: /startn <#>", admin);
    }
  }

  private void processSwap(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    /*if(game.getStatus() != KailleraGame.STATUS_PLAYING){
    	game.announce("Failed: wap Players can only be used during gameplay!", admin);
    	return;
    }*/

    Scanner scanner = new Scanner(message).useDelimiter(" ");

    try {
      int i = 1;
      String str;

      scanner.next();
      int test = scanner.nextInt();
      str = Integer.toString(test);

      if (game.getPlayers().size() < str.length()) {
        game.announce("Failed: You can't swap more than the # of players in the room.", admin);
        return;
      }

      if (test > 0) {
        int numCount = 0;
        int[] num = new int[str.length()];
        // before swap check numbers to prevent errors due to incorrectly entered numbers
        for (i = 0; i < num.length; i++) {
          num[i] = Integer.parseInt(String.valueOf(str.charAt(i)));
          numCount = 1;
          if (num[i] == 0 || num[i] > game.getPlayers().size()) break;
          for (int j = 0; j < num.length; j++) {
            if (num[i] != num[j]) numCount++;
          }
        }
        if (numCount == game.getPlayers().size()) {
          game.swap = true;
          // PlayerActionQueue temp = game.getPlayerActionQueue()[0];
          for (i = 0; i < str.length(); i++) {
            KailleraUserImpl player = game.getPlayers().get(i);
            player.setPlayerNumber(num[i]);
            /*if(num[i] == 1){
            	game.getPlayerActionQueue()[i] = temp;
            }
            else{
            	game.getPlayerActionQueue()[i] = game.getPlayerActionQueue()[num[i]-1];
            }*/

            game.announce(player.getName() + " is now Player#: " + player.getPlayerNumber(), null);
          }
        } else
          game.announce(
              "Swap Player Error: /swap <order> eg. 123..n {n = total # of players; Each slot = new player#}",
              admin);
      }

    } catch (NoSuchElementException e) {
      game.announce(
          "Swap Player Error: /swap <order> eg. 123..n {n = total # of players; Each slot = new player#}",
          admin);
    }
  }

  private void processStart(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    game.start(admin);
  }

  private void processKick(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");
    try {
      String str = scanner.next();
      if (str.equals("/kickall")) {
        // start kick players from last to first and don't kick owner or admin
        for (int w = game.getPlayers().size(); w >= 1; w--) {
          if (game.getPlayer(w).getAccess() < AccessManager.ACCESS_ADMIN
              && !game.getPlayer(w).equals(game.getOwner()))
            game.kick(admin, game.getPlayer(w).getID());
        }
        admin.getGame().announce("All players have been kicked!", null);
        return;
      }
      int playerNumber = scanner.nextInt();

      if (playerNumber > 0 && playerNumber < 101) {
        if (game.getPlayer(playerNumber) != null)
          game.kick(admin, game.getPlayer(playerNumber).getID());
        else {
          game.announce("Player doesn't exisit!", admin);
        }
      } else {
        game.announce("Kick Player Error: Enter value between 1 and 100", admin);
      }
    } catch (NoSuchElementException e) {
      game.announce("Failed: /kick <Player#> or /kickall to kick all players.", admin);
    }
  }

  private void processMaxUsers(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {

    if ((System.currentTimeMillis() - lastMaxUserChange) <= 3000) {
      game.announce("Max User Command Spam Detection...Please Wait!", admin);
      lastMaxUserChange = System.currentTimeMillis();
      return;
    } else {
      lastMaxUserChange = System.currentTimeMillis();
    }

    Scanner scanner = new Scanner(message).useDelimiter(" ");
    try {
      scanner.next();
      int num = scanner.nextInt();

      if (num > 0 && num < 101) {
        game.setMaxUsers(num);
        game.announce("Max Users has been set to " + num, null);
      } else {
        game.announce("Max Users Error: Enter value between 1 and 100", admin);
      }
    } catch (NoSuchElementException e) {
      game.announce("Failed: /maxusers <#>", admin);
    }
  }

  private void processMaxPing(
      String message,
      KailleraGameImpl game,
      KailleraUserImpl admin,
      V086ClientHandler clientHandler)
      throws ActionException, MessageFormatException {
    Scanner scanner = new Scanner(message).useDelimiter(" ");
    try {
      scanner.next();
      int num = scanner.nextInt();

      if (num > 0 && num < 1001) {
        game.setMaxPing(num);
        game.announce("Max Ping has been set to " + num, null);
      } else {
        game.announce("Max Ping Error: Enter value between 1 and 1000", admin);
      }
    } catch (NoSuchElementException e) {
      game.announce("Failed: /maxping <#>", admin);
    }
  }
}

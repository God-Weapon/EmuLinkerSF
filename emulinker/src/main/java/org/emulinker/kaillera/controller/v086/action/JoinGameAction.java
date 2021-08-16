package org.emulinker.kaillera.controller.v086.action;

import java.util.*;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.util.EmuLang;

public class JoinGameAction implements V086Action, V086GameEventHandler {
  private static Log log = LogFactory.getLog(JoinGameAction.class);
  private static final String desc = "JoinGameAction";
  private static JoinGameAction singleton = new JoinGameAction();

  public static JoinGameAction getInstance() {
    return singleton;
  }

  private int actionCount = 0;
  private int handledCount = 0;

  private JoinGameAction() {}

  public int getActionPerformedCount() {
    return actionCount;
  }

  public int getHandledEventCount() {
    return handledCount;
  }

  public String toString() {
    return desc;
  }

  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    if (!(message instanceof JoinGame_Request))
      throw new FatalActionException("Received incorrect instance of JoinGame: " + message);

    actionCount++;

    JoinGame_Request joinGameRequest = (JoinGame_Request) message;

    try {
      clientHandler.getUser().joinGame(joinGameRequest.getGameID());
    } catch (JoinGameException e) {
      try {
        clientHandler.send(
            new InformationMessage(
                clientHandler.getNextMessageNumber(),
                "server",
                EmuLang.getString("JoinGameAction.JoinGameDenied", e.getMessage())));
        clientHandler.send(
            new QuitGame_Notification(
                clientHandler.getNextMessageNumber(),
                clientHandler.getUser().getName(),
                clientHandler.getUser().getID()));
      } catch (MessageFormatException e2) {
        log.error("Failed to contruct new Message", e);
      }
    }
  }

  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    UserJoinedGameEvent userJoinedEvent = (UserJoinedGameEvent) event;
    KailleraUser thisUser = clientHandler.getUser();

    try {
      KailleraGame game = userJoinedEvent.getGame();
      KailleraUser user = userJoinedEvent.getUser();

      if (user.equals(thisUser)) {
        List<PlayerInformation.Player> players = new ArrayList<PlayerInformation.Player>();

        for (KailleraUser player : game.getPlayers()) {
          if (!player.equals(thisUser)) {
            if (player.getStealth() == false)
              players.add(
                  new PlayerInformation.Player(
                      player.getName(),
                      player.getPing(),
                      player.getID(),
                      player.getConnectionType()));
          }
        }

        clientHandler.send(new PlayerInformation(clientHandler.getNextMessageNumber(), players));
      }

      if (user.getStealth() == false)
        clientHandler.send(
            new JoinGame_Notification(
                clientHandler.getNextMessageNumber(),
                game.getID(),
                0,
                user.getName(),
                user.getPing(),
                user.getID(),
                user.getConnectionType()));
    } catch (MessageFormatException e) {
      log.error("Failed to contruct JoinGame_Notification message: " + e.getMessage(), e);
    }
  }
}

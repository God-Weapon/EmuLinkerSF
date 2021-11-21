package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.lookingforgame.TwitterBroadcaster;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.JoinGameException;
import org.emulinker.util.EmuLang;

@Singleton
public class JoinGameAction
    implements V086Action<JoinGame_Request>, V086GameEventHandler<UserJoinedGameEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "JoinGameAction";

  private int actionCount = 0;
  private int handledCount = 0;

  private final TwitterBroadcaster lookingForGameReporter;

  @Inject
  JoinGameAction(TwitterBroadcaster lookingForGameReporter) {
    this.lookingForGameReporter = lookingForGameReporter;
  }

  @Override
  public int getActionPerformedCount() {
    return actionCount;
  }

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void performAction(JoinGame_Request joinGameRequest, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().joinGame(joinGameRequest.gameId());
    } catch (JoinGameException e) {
      logger.atSevere().withCause(e).log("Failed to join game.");
      try {
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                EmuLang.getString("JoinGameAction.JoinGameDenied", e.getMessage())));
        clientHandler.send(
            QuitGame_Notification.create(
                clientHandler.getNextMessageNumber(),
                clientHandler.getUser().getName(),
                clientHandler.getUser().getID()));
      } catch (MessageFormatException e2) {
        logger.atSevere().withCause(e2).log("Failed to contruct new Message");
      }
    }
  }

  @Override
  public void handleEvent(UserJoinedGameEvent userJoinedEvent, V086ClientHandler clientHandler) {
    handledCount++;

    KailleraUser thisUser = clientHandler.getUser();

    try {
      KailleraGame game = userJoinedEvent.getGame();
      KailleraUser user = userJoinedEvent.getUser();

      if (user.equals(thisUser)) {
        List<PlayerInformation.Player> players = new ArrayList<PlayerInformation.Player>();

        for (KailleraUser player : game.getPlayers()) {
          if (!player.equals(thisUser)) {
            if (!player.getStealth())
              players.add(
                  PlayerInformation.Player.create(
                      player.getName(),
                      player.getPing(),
                      player.getID(),
                      player.getConnectionType()));
          }
        }

        clientHandler.send(PlayerInformation.create(clientHandler.getNextMessageNumber(), players));
      }

      if (!user.getStealth())
        clientHandler.send(
            JoinGame_Notification.create(
                clientHandler.getNextMessageNumber(),
                game.getID(),
                0,
                user.getName(),
                user.getPing(),
                user.getID(),
                user.getConnectionType()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct JoinGame_Notification message");
    }

    if (userJoinedEvent.getGame().getOwner().getID() != userJoinedEvent.getUser().getID()) {
      lookingForGameReporter.cancelActionsForGame(userJoinedEvent.getGame().getID());
    }
  }
}

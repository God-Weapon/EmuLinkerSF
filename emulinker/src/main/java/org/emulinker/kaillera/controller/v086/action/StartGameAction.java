package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.lookingforgame.TwitterBroadcaster;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.StartGameException;

@Singleton
public class StartGameAction
    implements V086Action<StartGame_Request>, V086GameEventHandler<GameStartedEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "StartGameAction";

  private int actionCount = 0;
  private int handledCount = 0;

  private final TwitterBroadcaster lookingForGameReporter;

  @Inject
  StartGameAction(TwitterBroadcaster lookingForGameReporter) {
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
  public void performAction(StartGame_Request message, V086ClientHandler clientHandler) {
    actionCount++;

    try {
      clientHandler.getUser().startGame();
    } catch (StartGameException e) {
      logger.atFine().withCause(e).log("Failed to start game");

      try {
        clientHandler.send(
            GameChat_Notification.create(
                clientHandler.getNextMessageNumber(), "Error", e.getMessage()));
      } catch (MessageFormatException ex) {
        logger.atSevere().withCause(ex).log("Failed to contruct GameChat_Notification message");
      }
    }
  }

  @Override
  public void handleEvent(GameStartedEvent gameStartedEvent, V086ClientHandler clientHandler) {
    handledCount++;

    try {
      KailleraGame game = gameStartedEvent.getGame();
      clientHandler.getUser().setTempDelay(game.getDelay() - clientHandler.getUser().getDelay());

      int delay;
      if (game.getSameDelay()) {
        delay = game.getDelay();
      } else {
        delay = clientHandler.getUser().getDelay();
      }

      int playerNumber = game.getPlayerNumber(clientHandler.getUser());
      clientHandler.send(
          StartGame_Notification.create(
              clientHandler.getNextMessageNumber(),
              (short) delay,
              (byte) playerNumber,
              (byte) game.getNumPlayers()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct StartGame_Notification message");
    }

    lookingForGameReporter.cancelActionsForGame(gameStartedEvent.getGame().getID());
  }
}

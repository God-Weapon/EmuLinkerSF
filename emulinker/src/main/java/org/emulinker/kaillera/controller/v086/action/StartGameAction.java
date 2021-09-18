package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.StartGameException;

@Singleton
public class StartGameAction implements V086Action, V086GameEventHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "StartGameAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  StartGameAction() {}

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
  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    if (!(message instanceof StartGame_Request))
      throw new FatalActionException("Received incorrect instance of StartGame: " + message);

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
  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    GameStartedEvent gameStartedEvent = (GameStartedEvent) event;

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
  }
}

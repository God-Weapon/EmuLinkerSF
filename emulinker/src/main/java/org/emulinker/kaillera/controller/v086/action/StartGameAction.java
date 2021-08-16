package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.StartGameException;

public class StartGameAction implements V086Action, V086GameEventHandler {
  private static Log log = LogFactory.getLog(StartGameAction.class);
  private static final String desc = "StartGameAction";
  private static StartGameAction singleton = new StartGameAction();

  public static StartGameAction getInstance() {
    return singleton;
  }

  private int actionCount = 0;
  private int handledCount = 0;

  private StartGameAction() {}

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
    return desc;
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
      log.debug("Failed to start game: " + e.getMessage());

      try {
        clientHandler.send(
            new GameChat_Notification(
                clientHandler.getNextMessageNumber(), "Error", e.getMessage()));
      } catch (MessageFormatException ex) {
        log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
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
          new StartGame_Notification(
              clientHandler.getNextMessageNumber(),
              (short) delay,
              (byte) playerNumber,
              (byte) game.getNumPlayers()));
    } catch (MessageFormatException e) {
      log.error("Failed to contruct StartGame_Notification message: " + e.getMessage(), e);
    }
  }
}

package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.util.EmuLang;

public class CreateGameAction implements V086Action, V086ServerEventHandler {
  private static Log log = LogFactory.getLog(CreateGameAction.class);
  private static final String desc = "CreateGameAction";
  private static CreateGameAction singleton = new CreateGameAction();

  public static CreateGameAction getInstance() {
    return singleton;
  }

  private int actionCount = 0;

  private int handledCount = 0;

  private CreateGameAction() {}

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
    if (!(message instanceof CreateGame_Request))
      throw new FatalActionException("Received incorrect instance of CreateGame: " + message);

    actionCount++;

    CreateGame createGameMessage = (CreateGame) message;

    try {
      clientHandler.getUser().createGame(createGameMessage.romName());
    } catch (CreateGameException e) {
      log.info(
          "Create Game Denied: " + clientHandler.getUser() + ": " + createGameMessage.romName());

      try {
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                EmuLang.getString("CreateGameAction.CreateGameDenied", e.getMessage())));
        clientHandler.send(
            QuitGame_Notification.create(
                clientHandler.getNextMessageNumber(),
                clientHandler.getUser().getName(),
                clientHandler.getUser().getID()));
      } catch (MessageFormatException e2) {
        log.error("Failed to contruct message: " + e.getMessage(), e);
      }
    } catch (FloodException e) {
      log.info(
          "Create Game Denied: " + clientHandler.getUser() + ": " + createGameMessage.romName());

      try {
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                EmuLang.getString("CreateGameAction.CreateGameDeniedFloodControl")));
        clientHandler.send(
            QuitGame_Notification.create(
                clientHandler.getNextMessageNumber(),
                clientHandler.getUser().getName(),
                clientHandler.getUser().getID()));
      } catch (MessageFormatException e2) {
        log.error("Failed to contruct message: " + e.getMessage(), e);
      }
    }
  }

  @Override
  public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    GameCreatedEvent gameCreatedEvent = (GameCreatedEvent) event;

    try {
      KailleraGame game = gameCreatedEvent.getGame();
      KailleraUser owner = game.getOwner();
      clientHandler.send(
          CreateGame_Notification.create(
              clientHandler.getNextMessageNumber(),
              owner.getName(),
              game.getRomName(),
              owner.getClientType(),
              game.getID(),
              (short) 0));
    } catch (MessageFormatException e) {
      log.error("Failed to contruct CreateGame_Notification message: " + e.getMessage(), e);
    }
  }
}

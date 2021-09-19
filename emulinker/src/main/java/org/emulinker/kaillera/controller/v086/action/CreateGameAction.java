package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.util.EmuLang;

@Singleton
public class CreateGameAction
    implements V086Action<CreateGame>, V086ServerEventHandler<GameCreatedEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "CreateGameAction";

  private int actionCount = 0;

  private int handledCount = 0;

  @Inject
  CreateGameAction() {}

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
  public void performAction(CreateGame createGameMessage, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().createGame(createGameMessage.romName());
    } catch (CreateGameException e) {
      logger.atInfo().withCause(e).log(
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
        logger.atSevere().withCause(e2).log("Failed to contruct message");
      }
    } catch (FloodException e) {
      logger.atInfo().withCause(e).log(
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
        logger.atSevere().withCause(e2).log("Failed to contruct message");
      }
    }
  }

  @Override
  public void handleEvent(GameCreatedEvent gameCreatedEvent, V086ClientHandler clientHandler) {
    handledCount++;

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
      logger.atSevere().withCause(e).log("Failed to contruct CreateGame_Notification message");
    }
  }
}

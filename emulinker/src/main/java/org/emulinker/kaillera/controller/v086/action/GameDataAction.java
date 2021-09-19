package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.GameDataException;

@Singleton
public class GameDataAction implements V086Action<GameData>, V086GameEventHandler<GameDataEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "GameDataAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  GameDataAction() {}

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
  public void performAction(GameData message, V086ClientHandler clientHandler)
      throws FatalActionException {
    try {
      KailleraUser user = clientHandler.getUser();
      byte[] data = message.gameData();

      clientHandler.getClientGameDataCache().add(data);
      user.addGameData(data);
    } catch (GameDataException e) {
      logger.atFine().withCause(e).log("Game data error");

      if (e.hasResponse()) {
        try {
          clientHandler.send(
              GameData.create(clientHandler.getNextMessageNumber(), e.getResponse()));
        } catch (MessageFormatException e2) {
          logger.atSevere().withCause(e2).log("Failed to contruct GameData message");
        }
      }
    }
  }

  @Override
  public void handleEvent(GameDataEvent event, V086ClientHandler clientHandler) {
    byte[] data = event.getData();
    int key = clientHandler.getServerGameDataCache().indexOf(data);

    if (key < 0) {
      clientHandler.getServerGameDataCache().add(data);

      try {
        clientHandler.send(GameData.create(clientHandler.getNextMessageNumber(), data));
      } catch (MessageFormatException e) {
        logger.atSevere().withCause(e).log("Failed to contruct GameData message");
      }
    } else {
      try {
        clientHandler.send(CachedGameData.create(clientHandler.getNextMessageNumber(), key));
      } catch (MessageFormatException e) {
        logger.atSevere().withCause(e).log("Failed to contruct CachedGameData message");
      }
    }
  }
}

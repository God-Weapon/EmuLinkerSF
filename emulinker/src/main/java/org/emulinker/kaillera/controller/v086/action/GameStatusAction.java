package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameStatus;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;

@Singleton
public class GameStatusAction implements V086ServerEventHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "GameStatusAction";

  private int handledCount = 0;

  @Inject
  GameStatusAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    GameStatusChangedEvent statusChangeEvent = (GameStatusChangedEvent) event;

    try {
      KailleraGame game = statusChangeEvent.getGame();
      int num = 0;
      for (KailleraUser user : game.getPlayers()) {
        if (!user.getStealth()) num++;
      }
      clientHandler.send(
          GameStatus.create(
              clientHandler.getNextMessageNumber(),
              game.getID(),
              (short) 0,
              (byte) game.getStatus(),
              (byte) num,
              (byte) game.getMaxUsers()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct CreateGame_Notification message");
    }
  }
}

package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;

@Singleton
public class GameTimeoutAction implements V086GameEventHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "GameTimeoutAction";

  private int handledCount = 0;

  @Inject
  GameTimeoutAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    GameTimeoutEvent timeoutEvent = (GameTimeoutEvent) event;
    KailleraUser player = timeoutEvent.getUser();
    KailleraUser user = clientHandler.getUser();

    if (player.equals(user)) {
      logger.atFine().log(
          user
              + " received timeout event "
              + timeoutEvent.getTimeoutNumber()
              + " for "
              + timeoutEvent.getGame()
              + ": resending messages...");
      clientHandler.resend(timeoutEvent.getTimeoutNumber());
    } else {
      logger.atFine().log(
          user
              + " received timeout event "
              + timeoutEvent.getTimeoutNumber()
              + " from "
              + player
              + " for "
              + timeoutEvent.getGame());
    }
  }
}

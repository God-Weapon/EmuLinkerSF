package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.UserReadyException;

@Singleton
public class UserReadyAction implements V086Action<AllReady>, V086GameEventHandler<GameEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "UserReadyAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  UserReadyAction() {}

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
  public void performAction(AllReady message, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().playerReady();
    } catch (UserReadyException e) {
      logger.atFine().withCause(e).log("Ready signal failed");
    }
  }

  @Override
  public void handleEvent(GameEvent event, V086ClientHandler clientHandler) {
    handledCount++;

    clientHandler.resetGameDataCache();

    try {
      clientHandler.send(AllReady.create(clientHandler.getNextMessageNumber()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct AllReady message");
    }
  }
}

package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.exception.GameKickException;

@Singleton
public class GameKickAction implements V086Action<GameKick> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "GameKickAction";

  private int actionCount = 0;

  @Inject
  GameKickAction() {}

  @Override
  public int getActionPerformedCount() {
    return actionCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void performAction(GameKick kickRequest, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().gameKick(kickRequest.userId());
    } catch (GameKickException e) {
      logger.atSevere().withCause(e).log("Failed to kick");
      // new SF MOD - kick errors notifications
      try {
        clientHandler.send(
            GameChat_Notification.create(
                clientHandler.getNextMessageNumber(), "Error", e.getMessage()));
      } catch (MessageFormatException ex) {
        logger.atSevere().withCause(ex).log("Failed to contruct GameChat_Notification message");
      }
    }
  }
}

package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.DropGameException;

@Singleton
public class DropGameAction
    implements V086Action<PlayerDrop_Request>, V086GameEventHandler<UserDroppedGameEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "DropGameAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  DropGameAction() {}

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
  public void performAction(PlayerDrop_Request message, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().dropGame();
    } catch (DropGameException e) {
      logger.atFine().withCause(e).log("Failed to drop game");
    }
  }

  @Override
  public void handleEvent(UserDroppedGameEvent userDroppedEvent, V086ClientHandler clientHandler) {
    handledCount++;

    try {
      KailleraUser user = userDroppedEvent.getUser();
      int playerNumber = userDroppedEvent.getPlayerNumber();
      //			clientHandler.send(PlayerDrop_Notification.create(clientHandler.getNextMessageNumber(),
      // user.getName(), (byte) game.getPlayerNumber(user)));
      if (user.getStealth() == false)
        clientHandler.send(
            PlayerDrop_Notification.create(
                clientHandler.getNextMessageNumber(), user.getName(), (byte) playerNumber));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct PlayerDrop_Notification message");
    }
  }
}

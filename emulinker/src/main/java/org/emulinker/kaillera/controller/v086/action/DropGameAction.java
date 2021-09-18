package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.DropGameException;

@Singleton
public class DropGameAction implements V086Action, V086GameEventHandler {
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
  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    if (!(message instanceof PlayerDrop_Request))
      throw new FatalActionException("Received incorrect instance of PlayerDrop: " + message);

    actionCount++;

    try {
      clientHandler.getUser().dropGame();
    } catch (DropGameException e) {
      logger.atFine().withCause(e).log("Failed to drop game");
    }
  }

  @Override
  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    UserDroppedGameEvent userDroppedEvent = (UserDroppedGameEvent) event;

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

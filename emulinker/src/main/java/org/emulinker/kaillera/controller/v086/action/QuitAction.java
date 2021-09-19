package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.ActionException;

@Singleton
public class QuitAction implements V086Action<Quit_Request>, V086ServerEventHandler<UserQuitEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "QuitAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  QuitAction() {}

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
  public void performAction(Quit_Request quitRequest, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().quit(quitRequest.message());
    } catch (ActionException e) {
      throw new FatalActionException("Failed to quit: " + e.getMessage());
    }
  }

  @Override
  public void handleEvent(UserQuitEvent userQuitEvent, V086ClientHandler clientHandler) {
    handledCount++;

    try {
      KailleraUser user = userQuitEvent.getUser();
      clientHandler.send(
          Quit_Notification.create(
              clientHandler.getNextMessageNumber(),
              user.getName(),
              user.getID(),
              userQuitEvent.getMessage()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct Quit_Notification message");
    }
  }
}

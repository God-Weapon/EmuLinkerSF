package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.ActionException;

@Singleton
public class QuitAction implements V086Action, V086ServerEventHandler {
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
  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    if (!(message instanceof Quit_Request))
      throw new FatalActionException("Received incorrect instance of Quit: " + message);

    actionCount++;

    Quit_Request quitRequest = (Quit_Request) message;

    try {
      clientHandler.getUser().quit(quitRequest.message());
    } catch (ActionException e) {
      throw new FatalActionException("Failed to quit: " + e.getMessage());
    }
  }

  @Override
  public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    UserQuitEvent userQuitEvent = (UserQuitEvent) event;

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

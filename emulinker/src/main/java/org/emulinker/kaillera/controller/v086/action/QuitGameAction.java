package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;

@Singleton
public class QuitGameAction implements V086Action, V086GameEventHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "QuitGameAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  QuitGameAction() {}

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
    if (!(message instanceof QuitGame_Request))
      throw new FatalActionException("Received incorrect instance of QuitGame: " + message);

    actionCount++;

    try {
      clientHandler.getUser().quitGame();
    } catch (DropGameException | QuitGameException | CloseGameException e) {
      logger.atSevere().withCause(e).log("Action failed");
    }

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("Sleep Interrupted!");
    }
  }

  @Override
  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    UserQuitGameEvent userQuitEvent = (UserQuitGameEvent) event;
    KailleraUser thisUser = clientHandler.getUser();

    try {
      KailleraUser user = userQuitEvent.getUser();

      if (user.getStealth() == false)
        clientHandler.send(
            QuitGame_Notification.create(
                clientHandler.getNextMessageNumber(), user.getName(), user.getID()));

      if (thisUser == user) {
        if (user.getStealth() == true)
          clientHandler.send(
              QuitGame_Notification.create(
                  clientHandler.getNextMessageNumber(), user.getName(), user.getID()));
      }
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct QuitGame_Notification message");
    }
  }
}

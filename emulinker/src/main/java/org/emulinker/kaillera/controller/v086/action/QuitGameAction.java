package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.lookingforgame.TwitterBroadcaster;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.*;

@Singleton
public class QuitGameAction
    implements V086Action<QuitGame_Request>, V086GameEventHandler<UserQuitGameEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "QuitGameAction";

  private int actionCount = 0;
  private int handledCount = 0;

  private final TwitterBroadcaster lookingForGameReporter;

  @Inject
  QuitGameAction(TwitterBroadcaster lookingForGameReporter) {
    this.lookingForGameReporter = lookingForGameReporter;
  }

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
  public void performAction(QuitGame_Request message, V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    try {
      clientHandler.getUser().quitGame();
      lookingForGameReporter.cancelActionsForUser(clientHandler.getUser().getID());
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
  public void handleEvent(UserQuitGameEvent userQuitEvent, V086ClientHandler clientHandler) {
    handledCount++;
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

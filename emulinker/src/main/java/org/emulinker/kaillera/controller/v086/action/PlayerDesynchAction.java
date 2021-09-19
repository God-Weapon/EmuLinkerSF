package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.util.EmuLang;

@Singleton
public class PlayerDesynchAction implements V086GameEventHandler<PlayerDesynchEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = PlayerDesynchAction.class.getSimpleName();

  private int handledCount = 0;

  @Inject
  PlayerDesynchAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void handleEvent(PlayerDesynchEvent desynchEvent, V086ClientHandler clientHandler) {
    handledCount++;

    try {
      clientHandler.send(
          GameChat_Notification.create(
              clientHandler.getNextMessageNumber(),
              EmuLang.getString("PlayerDesynchAction.DesynchDetected"),
              desynchEvent.getMessage()));
      // if (clientHandler.getUser().getStatus() == KailleraUser.STATUS_PLAYING)
      //	clientHandler.getUser().dropGame();
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct GameChat_Notification message");
    }
    // catch (DropGameException e)
    // {
    //	logger.atSevere().withCause(e).log("Failed to drop game during desynch");
    // }
  }
}

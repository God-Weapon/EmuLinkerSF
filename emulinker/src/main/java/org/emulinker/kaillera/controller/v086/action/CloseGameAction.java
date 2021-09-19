package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.CloseGame;
import org.emulinker.kaillera.model.event.*;

@Singleton
public class CloseGameAction implements V086ServerEventHandler<GameClosedEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "CloseGameAction";

  private int handledCount;

  @Inject
  CloseGameAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void handleEvent(GameClosedEvent gameClosedEvent, V086ClientHandler clientHandler) {
    handledCount++;

    try {
      clientHandler.send(
          CloseGame.create(
              clientHandler.getNextMessageNumber(), gameClosedEvent.getGame().getID(), (short) 0));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct CloseGame_Notification message");
    }
  }
}

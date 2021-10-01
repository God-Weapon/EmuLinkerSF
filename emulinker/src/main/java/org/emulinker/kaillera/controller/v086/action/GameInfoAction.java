package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.model.event.*;

@Singleton
public class GameInfoAction implements V086GameEventHandler<GameInfoEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "GameInfoAction";

  private int handledCount = 0;

  @Inject
  GameInfoAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void handleEvent(GameInfoEvent infoEvent, V086ClientHandler clientHandler) {
    handledCount++;

    if (infoEvent.getUser() != null) {
      if (infoEvent.getUser() != clientHandler.getUser()) return;
    }

    try {
      clientHandler.send(
          GameChat_Notification.create(
              clientHandler.getNextMessageNumber(), "Server", infoEvent.getMessage()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct GameChat_Notification message");
    }
  }
}

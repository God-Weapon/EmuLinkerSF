package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameStatus;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.event.*;

public class GameStatusAction implements V086ServerEventHandler {
  private static Log log = LogFactory.getLog(GameStatusAction.class);
  private static final String desc = "GameStatusAction";
  private static GameStatusAction singleton = new GameStatusAction();

  public static GameStatusAction getInstance() {
    return singleton;
  }

  private int handledCount = 0;

  private GameStatusAction() {}

  public int getHandledEventCount() {
    return handledCount;
  }

  public String toString() {
    return desc;
  }

  public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    GameStatusChangedEvent statusChangeEvent = (GameStatusChangedEvent) event;

    try {
      KailleraGame game = statusChangeEvent.getGame();
      int num = 0;
      for (KailleraUser user : game.getPlayers()) {
        if (!user.getStealth()) num++;
      }
      clientHandler.send(
          new GameStatus(
              clientHandler.getNextMessageNumber(),
              game.getID(),
              (short) 0,
              (byte) game.getStatus(),
              (byte) num,
              (byte) game.getMaxUsers()));
    } catch (MessageFormatException e) {
      log.error("Failed to contruct CreateGame_Notification message: " + e.getMessage(), e);
    }
  }
}

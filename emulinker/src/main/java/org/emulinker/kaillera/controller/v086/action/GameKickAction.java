package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.exception.GameKickException;

public class GameKickAction implements V086Action {
  private static Log log = LogFactory.getLog(GameKickAction.class);
  private static final String desc = "GameKickAction";
  private static GameKickAction singleton = new GameKickAction();

  public static GameKickAction getInstance() {
    return singleton;
  }

  private int actionCount = 0;

  private GameKickAction() {}

  @Override
  public int getActionPerformedCount() {
    return actionCount;
  }

  @Override
  public String toString() {
    return desc;
  }

  @Override
  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    GameKick kickRequest = (GameKick) message;

    try {
      clientHandler.getUser().gameKick(kickRequest.getUserID());
    } catch (GameKickException e) {
      log.debug("Failed to kick: " + e.getMessage());
      // new SF MOD - kick errors notifications
      try {
        clientHandler.send(
            new GameChat_Notification(
                clientHandler.getNextMessageNumber(), "Error", e.getMessage()));
      } catch (MessageFormatException ex) {
        log.error("Failed to contruct GameChat_Notification message: " + e.getMessage(), e);
      }
    }
  }
}

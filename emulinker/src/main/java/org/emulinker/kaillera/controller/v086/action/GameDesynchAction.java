package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.GameChat_Notification;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.util.EmuLang;

public class GameDesynchAction implements V086GameEventHandler {
  private static Log log = LogFactory.getLog(GameDesynchAction.class);
  private static final String desc = "GameDesynchAction"; // $NON-NLS-1$
  private static GameDesynchAction singleton = new GameDesynchAction();

  public static GameDesynchAction getInstance() {
    return singleton;
  }

  private int handledCount = 0;

  private GameDesynchAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return desc;
  }

  @Override
  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    GameDesynchEvent desynchEvent = (GameDesynchEvent) event;

    try {
      clientHandler.send(
          new GameChat_Notification(
              clientHandler.getNextMessageNumber(),
              EmuLang.getString("GameDesynchAction.DesynchDetected"),
              desynchEvent.getMessage())); // $NON-NLS-1$
      // if (clientHandler.getUser().getStatus() == KailleraUser.STATUS_PLAYING)
      //	clientHandler.getUser().dropGame();
    } catch (MessageFormatException e) {
      log.error(
          "Failed to contruct GameChat_Notification message: " + e.getMessage(), e); // $NON-NLS-1$
    }
    // catch (DropGameException e)
    // {
    //	log.error("Failed to drop game during desynch: " + e.getMessage(), e);
    // }
  }
}

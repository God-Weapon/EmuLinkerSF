package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.model.event.GameEvent;

public interface V086GameEventHandler {
  @Override
  public String toString();

  public void handleEvent(GameEvent event, V086Controller.V086ClientHandler clientHandler);

  public int getHandledEventCount();
}

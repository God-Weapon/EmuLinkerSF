package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.model.event.GameEvent;

public interface V086GameEventHandler<T extends GameEvent> {
  @Override
  public String toString();

  public void handleEvent(T event, V086ClientHandler clientHandler);

  public int getHandledEventCount();
}

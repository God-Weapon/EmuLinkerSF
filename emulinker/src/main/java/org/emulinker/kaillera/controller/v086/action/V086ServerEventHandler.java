package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.model.event.ServerEvent;

public interface V086ServerEventHandler<T extends ServerEvent> {
  @Override
  public String toString();

  public void handleEvent(T event, V086ClientHandler clientHandler);

  public int getHandledEventCount();
}

package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086ClientHandler;
import org.emulinker.kaillera.model.event.UserEvent;

public interface V086UserEventHandler<T extends UserEvent> {
  @Override
  public String toString();

  public void handleEvent(T event, V086ClientHandler clientHandler);

  public int getHandledEventCount();
}

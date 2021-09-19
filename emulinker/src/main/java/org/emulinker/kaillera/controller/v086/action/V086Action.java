package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;

public interface V086Action<T extends V086Message> {
  @Override
  public String toString();

  public void performAction(T message, V086ClientHandler clientHandler) throws FatalActionException;

  public int getActionPerformedCount();
}

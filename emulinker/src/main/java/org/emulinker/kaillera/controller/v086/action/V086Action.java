package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.V086Message;

public interface V086Action {
  @Override
  public String toString();

  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException;

  public int getActionPerformedCount();
}

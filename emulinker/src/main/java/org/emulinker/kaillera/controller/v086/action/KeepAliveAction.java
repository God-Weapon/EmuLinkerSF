package org.emulinker.kaillera.controller.v086.action;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;

@Singleton
public class KeepAliveAction implements V086Action {
  private static final String DESC = "KeepAliveAction";

  private int actionCount = 0;

  @Inject
  KeepAliveAction() {}

  @Override
  public int getActionPerformedCount() {
    return actionCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    if (clientHandler.getUser() == null)
      throw new FatalActionException("User does not exist: KeepAliveAction!");

    clientHandler.getUser().updateLastKeepAlive();
  }
}

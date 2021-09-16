package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;

public class KeepAliveAction implements V086Action {
  private static final String desc = "KeepAliveAction";
  private static KeepAliveAction singleton = new KeepAliveAction();

  public static KeepAliveAction getInstance() {
    return singleton;
  }

  private int actionCount = 0;

  private KeepAliveAction() {}

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

    if (clientHandler.getUser() == null)
      throw new FatalActionException("User does not exist: KeepAliveAction!");

    clientHandler.getUser().updateLastKeepAlive();
  }
}

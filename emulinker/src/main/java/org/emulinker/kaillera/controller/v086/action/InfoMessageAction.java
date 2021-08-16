package org.emulinker.kaillera.controller.v086.action;

import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage;
import org.emulinker.kaillera.model.event.*;

public class InfoMessageAction implements V086UserEventHandler {
  private static Log log = LogFactory.getLog(InfoMessageAction.class);
  private static final String desc = "InfoMessageAction";
  private static InfoMessageAction singleton = new InfoMessageAction();

  public static InfoMessageAction getInstance() {
    return singleton;
  }

  private int handledCount = 0;

  private InfoMessageAction() {}

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return desc;
  }

  @Override
  public void handleEvent(UserEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    InfoMessageEvent infoEvent = (InfoMessageEvent) event;

    try {
      clientHandler.send(
          new InformationMessage(
              clientHandler.getNextMessageNumber(), "server", infoEvent.getMessage()));
    } catch (MessageFormatException e) {
      log.error("Failed to contruct InformationMessage message: " + e.getMessage(), e);
    }
  }
}

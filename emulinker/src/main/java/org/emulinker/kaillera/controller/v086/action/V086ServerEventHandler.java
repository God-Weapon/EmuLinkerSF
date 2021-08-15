package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.model.event.ServerEvent;

public interface V086ServerEventHandler
{
	public String toString();

	public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler);

	public int getHandledEventCount();
}

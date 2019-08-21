package org.emulinker.kaillera.controller.v086.action;

import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.model.event.UserEvent;

public interface V086UserEventHandler
{
	public String toString();

	public void handleEvent(UserEvent event, V086Controller.V086ClientHandler clientHandler);

	public int getHandledEventCount();
}

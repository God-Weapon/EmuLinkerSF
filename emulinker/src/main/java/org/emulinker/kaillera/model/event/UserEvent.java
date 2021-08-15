package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraUser;

public interface UserEvent extends KailleraEvent
{
	public KailleraUser getUser();
}

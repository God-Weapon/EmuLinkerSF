package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraServer;

public interface ServerEvent extends KailleraEvent {
  public KailleraServer getServer();
}

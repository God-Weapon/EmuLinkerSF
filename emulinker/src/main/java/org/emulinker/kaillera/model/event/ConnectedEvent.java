package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class ConnectedEvent implements UserEvent {
  private KailleraUser user;
  private KailleraServer server;

  public ConnectedEvent(KailleraServer server, KailleraUser user) {
    this.server = server;
    this.user = user;
  }

  public String toString() {
    return "ConnectedEvent";
  }

  public KailleraUser getUser() {
    return user;
  }

  public KailleraServer getServer() {
    return server;
  }
}

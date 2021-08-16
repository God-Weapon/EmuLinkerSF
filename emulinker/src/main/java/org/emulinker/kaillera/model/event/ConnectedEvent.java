package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class ConnectedEvent implements UserEvent {
  private KailleraUser user;
  private KailleraServer server;

  public ConnectedEvent(KailleraServer server, KailleraUser user) {
    this.server = server;
    this.user = user;
  }

  @Override
  public String toString() {
    return "ConnectedEvent";
  }

  @Override
  public KailleraUser getUser() {
    return user;
  }

  public KailleraServer getServer() {
    return server;
  }
}

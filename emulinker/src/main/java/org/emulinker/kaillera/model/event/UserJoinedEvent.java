package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class UserJoinedEvent implements ServerEvent {
  private KailleraServer server;
  private KailleraUser user;

  public UserJoinedEvent(KailleraServer server, KailleraUser user) {
    this.server = server;
    this.user = user;
  }

  @Override
  public String toString() {
    return "UserJoinedEvent";
  }

  @Override
  public KailleraServer getServer() {
    return server;
  }

  public KailleraUser getUser() {
    return user;
  }
}

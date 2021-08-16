package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class UserQuitEvent implements ServerEvent {
  private KailleraServer server;
  private KailleraUser user;
  private String message;

  public UserQuitEvent(KailleraServer server, KailleraUser user, String message) {
    this.server = server;
    this.user = user;
    this.message = message;
  }

  @Override
  public String toString() {
    return "UserQuitEvent";
  }

  @Override
  public KailleraServer getServer() {
    return server;
  }

  public KailleraUser getUser() {
    return user;
  }

  public String getMessage() {
    return message;
  }
}

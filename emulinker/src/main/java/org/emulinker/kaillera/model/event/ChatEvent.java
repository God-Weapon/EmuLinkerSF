package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class ChatEvent implements ServerEvent {
  private KailleraServer server;
  private KailleraUser user;
  private String message;

  public ChatEvent(KailleraServer server, KailleraUser user, String message) {
    this.server = server;
    this.user = user;
    this.message = message;
  }

  @Override
  public String toString() {
    return "ChatEvent";
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

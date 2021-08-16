package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraUser;

public class InfoMessageEvent implements UserEvent {
  private KailleraUser user;
  private String message;

  public InfoMessageEvent(KailleraUser user, String message) {
    this.user = user;
    this.message = message;
  }

  public String toString() {
    return "InfoMessageEvent";
  }

  public KailleraUser getUser() {
    return user;
  }

  public String getMessage() {
    return message;
  }
}

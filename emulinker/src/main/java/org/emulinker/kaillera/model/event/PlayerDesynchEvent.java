package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class PlayerDesynchEvent implements GameEvent {
  private KailleraGame game;
  private KailleraUser user;
  private String message;

  public PlayerDesynchEvent(KailleraGame game, KailleraUser user, String message) {
    this.game = game;
    this.user = user;
    this.message = message;
  }

  public String toString() {
    return "GameDesynchEvent";
  }

  public KailleraGame getGame() {
    return game;
  }

  public KailleraUser getUser() {
    return user;
  }

  public String getMessage() {
    return message;
  }
}

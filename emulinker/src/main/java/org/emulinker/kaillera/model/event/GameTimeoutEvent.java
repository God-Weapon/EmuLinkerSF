package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class GameTimeoutEvent implements GameEvent {
  private KailleraGame game;
  private KailleraUser user;
  private int timeoutNumber;

  public GameTimeoutEvent(KailleraGame game, KailleraUser user, int timeoutNumber) {
    this.game = game;
    this.user = user;
    this.timeoutNumber = timeoutNumber;
  }

  public String toString() {
    return "GameTimeoutEvent";
  }

  public KailleraGame getGame() {
    return game;
  }

  public KailleraUser getUser() {
    return user;
  }

  public int getTimeoutNumber() {
    return timeoutNumber;
  }
}

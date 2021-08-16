package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class UserQuitGameEvent implements GameEvent {
  private KailleraGame game;
  private KailleraUser user;

  public UserQuitGameEvent(KailleraGame game, KailleraUser user) {
    this.game = game;
    this.user = user;
  }

  @Override
  public String toString() {
    return "UserQuitGameEvent";
  }

  @Override
  public KailleraGame getGame() {
    return game;
  }

  public KailleraUser getUser() {
    return user;
  }
}

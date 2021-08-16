package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class UserDroppedGameEvent implements GameEvent {
  private KailleraGame game;
  private KailleraUser user;
  private int playerNumber;

  public UserDroppedGameEvent(KailleraGame game, KailleraUser user, int playerNumber) {
    this.game = game;
    this.user = user;
    this.playerNumber = playerNumber;
  }

  @Override
  public String toString() {
    return "UserDroppedGameEvent";
  }

  @Override
  public KailleraGame getGame() {
    return game;
  }

  public KailleraUser getUser() {
    return user;
  }

  public int getPlayerNumber() {
    return playerNumber;
  }
}

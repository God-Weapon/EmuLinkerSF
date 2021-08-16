package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class GameClosedEvent implements ServerEvent {
  private KailleraServer server;
  private KailleraGame game;

  public GameClosedEvent(KailleraServer server, KailleraGame game) {
    this.server = server;
    this.game = game;
  }

  @Override
  public String toString() {
    return "GameClosedEvent";
  }

  @Override
  public KailleraServer getServer() {
    return server;
  }

  public KailleraGame getGame() {
    return game;
  }
}

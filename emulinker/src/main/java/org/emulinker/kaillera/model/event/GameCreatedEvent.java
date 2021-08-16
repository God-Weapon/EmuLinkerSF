package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.*;

public class GameCreatedEvent implements ServerEvent {
  private KailleraServer server;
  private KailleraGame game;

  public GameCreatedEvent(KailleraServer server, KailleraGame game) {
    this.server = server;
    this.game = game;
  }

  @Override
  public String toString() {
    return "GameCreatedEvent";
  }

  @Override
  public KailleraServer getServer() {
    return server;
  }

  public KailleraGame getGame() {
    return game;
  }
}

package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraGame;

public class GameDesynchEvent implements GameEvent {
  private KailleraGame game;
  private String message;

  public GameDesynchEvent(KailleraGame game, String message) {
    this.game = game;
    this.message = message;
  }

  @Override
  public String toString() {
    return "GameDesynchEvent";
  }

  @Override
  public KailleraGame getGame() {
    return game;
  }

  public String getMessage() {
    return message;
  }
}

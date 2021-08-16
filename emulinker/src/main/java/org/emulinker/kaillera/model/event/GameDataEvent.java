package org.emulinker.kaillera.model.event;

import org.emulinker.kaillera.model.KailleraGame;

public class GameDataEvent implements GameEvent {
  private KailleraGame game;
  private byte[] data;

  public GameDataEvent(KailleraGame game, byte[] data) {
    this.game = game;
    this.data = data;
  }

  @Override
  public String toString() {
    return "GameDataEvent";
  }

  @Override
  public KailleraGame getGame() {
    return game;
  }

  public byte[] getData() {
    return data;
  }
}

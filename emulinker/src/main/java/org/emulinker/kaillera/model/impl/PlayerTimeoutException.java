package org.emulinker.kaillera.model.impl;

public class PlayerTimeoutException extends Exception {
  private int playerNumber;
  private KailleraUserImpl player;
  private int timeoutNumber = -1;

  public PlayerTimeoutException(int playerNumber, KailleraUserImpl player) {
    this.playerNumber = playerNumber;
    this.player = player;
  }

  public PlayerTimeoutException(int playerNumber, int timeoutNumber) {
    this.playerNumber = playerNumber;
    this.timeoutNumber = timeoutNumber;
  }

  public int getPlayerNumber() {
    return playerNumber;
  }

  public KailleraUserImpl getPlayer() {
    return player;
  }

  public int getTimeoutNumber() {
    return timeoutNumber;
  }

  public void setTimeoutNumber(int timeoutNumber) {
    this.timeoutNumber = timeoutNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (o != null && o instanceof PlayerTimeoutException) {
      PlayerTimeoutException e = (PlayerTimeoutException) o;
      if (e.getPlayerNumber() == getPlayerNumber() && e.getTimeoutNumber() == getTimeoutNumber())
        return true;
    }

    return false;
  }
}

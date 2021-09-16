package org.emulinker.util;

import java.util.concurrent.*;

public class GameDataQueue {
  private int gameID;
  private int numPlayers;
  private PlayerDataQueue[] playerQueues;
  private int timeoutMillis;
  private int retries;
  private boolean gameDesynched = false;

  public GameDataQueue(int gameID, int numPlayers, int timeoutMillis, int retries) {
    this.gameID = gameID;
    this.numPlayers = numPlayers;
    this.timeoutMillis = timeoutMillis;
    this.retries = retries;

    playerQueues = new PlayerDataQueue[numPlayers];
    for (int i = 0; i < playerQueues.length; i++) playerQueues[i] = new PlayerDataQueue((i + 1));
  }

  public int getGameID() {
    return gameID;
  }

  public int getNumPlayers() {
    return numPlayers;
  }

  public int getTimeoutMillis() {
    return timeoutMillis;
  }

  public int getRetries() {
    return retries;
  }

  public void setGameDesynched() {
    gameDesynched = true;
  }

  public void addData(int playerNumber, byte data[]) {
    for (int i = 0; i < data.length; i++) addData(playerNumber, data[i]);
  }

  public void addData(int playerNumber, byte data) {
    for (int i = 0; i < numPlayers; i++) playerQueues[i].addData(playerNumber, data);
  }

  public byte[] getData(int playerNumber, int byteCount, int bytesPerAction)
      throws PlayerTimeoutException, DesynchException {
    return playerQueues[(playerNumber - 1)].getData(byteCount, bytesPerAction);
  }

  private class PlayerDataQueue {
    private CircularBlockingByteQueue[] queues;
    private int lastI = 0;
    private int lastJ = 0;
    private byte[] lastData = null;
    private int timeoutCounter = 0;

    private PlayerDataQueue(int playerNumber) {
      queues = new CircularBlockingByteQueue[numPlayers];
      for (int i = 0; i < queues.length; i++)
        queues[i] = new CircularBlockingByteQueue(((numPlayers * 6) * 4));
    }

    private void addData(int playerNumber, byte data) {
      queues[(playerNumber - 1)].put(data);
    }

    private byte[] getData(int byteCount, int bytesPerAction)
        throws PlayerTimeoutException, DesynchException {
      byte[] data = null;

      if (lastData != null) {
        data = lastData;
        lastData = null;
        //				log.debug("Player " + thisPlayerNumber + ": getData with i=" + lastI + ", j=" +
        // lastJ);
      } else data = new byte[(byteCount * numPlayers)];

      for (int i = lastI; i < ((byteCount / bytesPerAction) * numPlayers); i++) {
        for (int j = lastJ; j < bytesPerAction; j++) {
          try {
            data[((i * bytesPerAction) + j)] =
                queues[(i % numPlayers)].get(timeoutMillis, TimeUnit.MILLISECONDS);
          } catch (TimeoutException e) {
            lastI = i;
            lastJ = j;
            lastData = data;

            if (++timeoutCounter > retries)
              throw new DesynchException(
                  "Player " + ((i % numPlayers) + 1) + " is lagged!", ((i % numPlayers) + 1), e);
            else throw new PlayerTimeoutException(((i % numPlayers) + 1), timeoutCounter, e);
          }
        }
      }

      lastI = lastJ = 0;
      lastData = null;
      timeoutCounter = 0;

      return data;
    }
  }

  public static class PlayerTimeoutException extends Exception {
    private int playerNumber;
    private int timeoutNumber;

    private PlayerTimeoutException(int playerNumber, int timeoutNumber, TimeoutException e) {
      super(e);
      this.playerNumber = playerNumber;
      this.timeoutNumber = timeoutNumber;
    }

    public int getPlayerNumber() {
      return playerNumber;
    }

    public int getTimeoutNumber() {
      return timeoutNumber;
    }
  }

  public static class DesynchException extends Exception {
    private int playerNumber;

    private DesynchException(String msg, int playerNumber, TimeoutException e) {
      super(msg, e);
      this.playerNumber = playerNumber;
    }

    public int getPlayerNumber() {
      return playerNumber;
    }
  }
}

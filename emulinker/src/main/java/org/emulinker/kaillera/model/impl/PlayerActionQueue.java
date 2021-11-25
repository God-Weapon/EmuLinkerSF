package org.emulinker.kaillera.model.impl;

public class PlayerActionQueue {
  private int gameBufferSize;
  private int gameTimeoutMillis;

  private int thisPlayerNumber;
  private KailleraUserImpl thisPlayer;
  private boolean synched = false;
  private PlayerTimeoutException lastTimeout;

  private byte[] array;
  private int[] heads;
  private int tail = 0;

  //	private OutputStream			os;
  //	private InputStream				is;

  public PlayerActionQueue(
      int playerNumber,
      KailleraUserImpl player,
      int numPlayers,
      int gameBufferSize,
      int gameTimeoutMillis,
      boolean capture) {
    this.thisPlayerNumber = playerNumber;
    this.thisPlayer = player;
    this.gameBufferSize = gameBufferSize;
    this.gameTimeoutMillis = gameTimeoutMillis;

    array = new byte[gameBufferSize];
    heads = new int[numPlayers];
    /*
    		if(capture)
    		{
    			try
    			{
    				os = new BufferedOutputStream(new FileOutputStream("test.cap"));
    			}
    			catch(Exception e)
    			{
    				e.printStackTrace();
    			}
    		}
    */
  }

  public int getPlayerNumber() {
    return thisPlayerNumber;
  }

  public KailleraUserImpl getPlayer() {
    return thisPlayer;
  }

  public void setSynched(boolean synched) {
    this.synched = synched;

    if (!synched) {
      synchronized (this) {
        notifyAll();
      }
      /*
      			try
      			{
      				os.flush();
      				os.close();
      			}
      			catch(Exception e)
      			{
      				e.printStackTrace();
      			}
      */
    }
  }

  public boolean isSynched() {
    return synched;
  }

  public void setLastTimeout(PlayerTimeoutException e) {
    this.lastTimeout = e;
  }

  public PlayerTimeoutException getLastTimeout() {
    return lastTimeout;
  }

  public void addActions(byte[] actions) {
    if (!synched) return;

    for (int i = 0; i < actions.length; i++) {
      array[tail] = actions[i];
      // tail = ((tail + 1) % gameBufferSize);
      tail++;
      if (tail == gameBufferSize) tail = 0;
    }

    synchronized (this) {
      notifyAll();
    }

    lastTimeout = null;
  }

  public void getAction(int playerNumber, byte[] actions, int location, int actionLength)
      throws PlayerTimeoutException {

    synchronized (this) {
      if (getSize(playerNumber) < actionLength && synched) {
        try {
          wait(gameTimeoutMillis);
        } catch (InterruptedException e) {
        }
      }
    }

    if (getSize(playerNumber) >= actionLength) {
      for (int i = 0; i < actionLength; i++) {
        actions[(location + i)] = array[heads[(playerNumber - 1)]];
        // heads[(playerNumber - 1)] = ((heads[(playerNumber - 1)] + 1) % gameBufferSize);
        heads[(playerNumber - 1)]++;
        if (heads[(playerNumber - 1)] == gameBufferSize) heads[(playerNumber - 1)] = 0;
      }
      return;
    }

    if (!synched) return;
    /*
    		if(capture)
    		{
    			try
    			{
    				os.write(actions,0,actions.length);
    				System.out.println("write " + actions.length + " bytes");
    			}
    			catch(Exception e)
    			{
    				e.printStackTrace();
    			}
    		}
    */
    throw new PlayerTimeoutException(thisPlayerNumber, /* timeoutNumber= */ -1, thisPlayer);
  }

  private int getSize(int playerNumber) {
    return (tail + gameBufferSize - heads[playerNumber - 1]) % gameBufferSize;
  }
}

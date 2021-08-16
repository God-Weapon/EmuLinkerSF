package org.emulinker.kaillera.model.exception;

public class GameDataException extends ActionException {
  //	private boolean	reflectData	= true;
  //	private int numBytes = 0;

  //	private static byte[] DESYNCH_DATA = new byte[1000];

  private byte[] response;

  /*
  	public GameDataException(String message)
  	{
  		super(message);
  	}

  	public GameDataException(String message, boolean reflectData)
  	{
  		super(message);
  		this.reflectData = reflectData;
  	}

  	public GameDataException(String message, Exception source)
  	{
  		super(message, source);
  	}

  	public GameDataException(String message, Exception source, boolean reflectData)
  	{
  		super(message, source);
  		this.reflectData = reflectData;
  	}
  	public GameDataException(String message, int numBytes)
  	{
  		super(message);
  		this.numBytes = numBytes;
  	}
  */

  public GameDataException(String message) {
    super(message);
  }

  public GameDataException(
      String message, byte[] data, int actionsPerMessage, int playerNumber, int numPlayers) {
    super(message);

    int bytesPerAction = (data.length / actionsPerMessage);
    int arraySize = (numPlayers * actionsPerMessage * bytesPerAction);
    response = new byte[arraySize];
    for (int actionCounter = 0; actionCounter < actionsPerMessage; actionCounter++) {
      System.arraycopy(
          data,
          0,
          response,
          ((actionCounter * (numPlayers * bytesPerAction)) + ((playerNumber - 1) * bytesPerAction)),
          bytesPerAction);
    }
  }

  /*
  	public boolean getReflectData()
  	{
  		return reflectData;
  	}

  	public void setReflectData(boolean reflectData)
  	{
  		this.reflectData = reflectData;
  	}
  */

  public boolean hasResponse() {
    return (response != null);
  }

  public byte[] getResponse() {
    if (!hasResponse()) return null;

    return response;
  }
}

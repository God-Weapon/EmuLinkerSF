package org.emulinker.kaillera.model.exception

class GameDataException : ActionException {
  //	private boolean	reflectData	= true;
  //	private int numBytes = 0;
  //	private static byte[] DESYNCH_DATA = new byte[1000];
  var response: ByteArray? = null
    private set

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
  constructor(message: String?) : super(message)

  constructor(
      message: String?, data: ByteArray, actionsPerMessage: Int, playerNumber: Int, numPlayers: Int
  ) : super(message) {
    val bytesPerAction = data.size / actionsPerMessage
    val arraySize = numPlayers * actionsPerMessage * bytesPerAction
    response = ByteArray(arraySize)
    for (actionCounter in 0 until actionsPerMessage) {
      System.arraycopy(
          data,
          0,
          response,
          actionCounter * (numPlayers * bytesPerAction) + (playerNumber - 1) * bytesPerAction,
          bytesPerAction)
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
}

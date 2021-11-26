package org.emulinker.kaillera.model.impl

import java.lang.InterruptedException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.Throws

class PlayerActionQueue(
    val playerNumber: Int,
    val player: KailleraUserImpl,
    numPlayers: Int,
    private val gameBufferSize: Int,
    private val gameTimeoutMillis: Int,
    capture: Boolean
) {
  var lastTimeout: PlayerTimeoutException? = null
  private val array: ByteArray
  private val heads: IntArray
  private var tail = 0

  private val lock = ReentrantLock()

  var synched = false
    set(value) {
      field = value
      if (!value) {
        val condition = lock.newCondition()
        synchronized(lock) { condition.signalAll() }
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

  fun addActions(actions: ByteArray) {
    if (!synched) return
    for (i in actions.indices) {
      array[tail] = actions[i]
      // tail = ((tail + 1) % gameBufferSize);
      tail++
      if (tail == gameBufferSize) tail = 0
    }
    val condition = lock.newCondition()
    lock.lock()
    synchronized(lock) { condition.signalAll() }
    lastTimeout = null
  }

  @Throws(PlayerTimeoutException::class)
  fun getAction(playerNumber: Int, actions: ByteArray, location: Int, actionLength: Int) {
    lock.lock()
    synchronized(lock) {
      if (getSize(playerNumber) < actionLength && synched) {
        try {
          val condition = lock.newCondition()
          condition.await(gameTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {}
      }
    }
    if (getSize(playerNumber) >= actionLength) {
      for (i in 0 until actionLength) {
        actions[location + i] = array[heads[playerNumber - 1]]
        // heads[(playerNumber - 1)] = ((heads[(playerNumber - 1)] + 1) % gameBufferSize);
        heads[playerNumber - 1]++
        if (heads[playerNumber - 1] == gameBufferSize) heads[playerNumber - 1] = 0
      }
      return
    }
    if (!synched) return
    throw PlayerTimeoutException(this.playerNumber, /* timeoutNumber= */ -1, player)
  }

  private fun getSize(playerNumber: Int): Int {
    return (tail + gameBufferSize - heads[playerNumber - 1]) % gameBufferSize
  }

  //	private OutputStream			os;
  //	private InputStream				is;
  init {
    array = ByteArray(gameBufferSize)
    heads = IntArray(numPlayers)
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
}

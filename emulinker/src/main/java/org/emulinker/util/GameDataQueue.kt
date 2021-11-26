package org.emulinker.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class GameDataQueue(
    val gameID: Int, val numPlayers: Int, val timeoutMillis: Int, val retries: Int
) {
  private val playerQueues: Array<PlayerDataQueue?> = arrayOfNulls(numPlayers)
  private var gameDesynched = false

  fun setGameDesynched() {
    gameDesynched = true
  }

  fun addData(playerNumber: Int, data: ByteArray) {
    for (i in data.indices) addData(playerNumber, data[i])
  }

  fun addData(playerNumber: Int, data: Byte) {
    for (i in 0 until numPlayers) playerQueues[i]!!.addData(playerNumber, data)
  }

  @Throws(PlayerTimeoutException::class, DesynchException::class)
  fun getData(playerNumber: Int, byteCount: Int, bytesPerAction: Int): ByteArray? {
    return playerQueues[playerNumber - 1]!!.getData(byteCount, bytesPerAction)
  }

  private inner class PlayerDataQueue internal constructor(playerNumber: Int) {
    private val queues: Array<CircularBlockingByteQueue?> = arrayOfNulls(numPlayers)
    private var lastI = 0
    private var lastJ = 0
    private var lastData: ByteArray? = null
    private var timeoutCounter = 0

    internal fun addData(playerNumber: Int, data: Byte) {
      queues[playerNumber - 1]!!.put(data)
    }

    @Throws(PlayerTimeoutException::class, DesynchException::class)
    fun getData(byteCount: Int, bytesPerAction: Int): ByteArray? {
      val data: ByteArray?
      if (lastData != null) {
        data = lastData
        lastData = null
        //				logger.atFine().log("Player " + thisPlayerNumber + ": getData with i=" + lastI + ",
        // j=" +
        // lastJ);
      } else data = ByteArray(byteCount * numPlayers)
      for (i in lastI until byteCount / bytesPerAction * numPlayers) {
        for (j in lastJ until bytesPerAction) {
          try {
            data!![i * bytesPerAction + j] =
                queues[i % numPlayers]!![timeoutMillis.toLong(), TimeUnit.MILLISECONDS]
          } catch (e: TimeoutException) {
            lastI = i
            lastJ = j
            lastData = data
            if (++timeoutCounter > retries)
                throw DesynchException(
                    "Player " + (i % numPlayers + 1) + " is lagged!", i % numPlayers + 1, e)
            else throw PlayerTimeoutException(i % numPlayers + 1, timeoutCounter, e)
          }
        }
      }
      lastJ = 0
      lastI = lastJ
      lastData = null
      timeoutCounter = 0
      return data
    }

    init {
      for (i in queues.indices) queues[i] = CircularBlockingByteQueue(numPlayers * 6 * 4)
    }
  }

  class PlayerTimeoutException(val playerNumber: Int, val timeoutNumber: Int, e: TimeoutException) :
      Exception(e)
  class DesynchException(msg: String, val playerNumber: Int, e: TimeoutException) :
      Exception(msg, e)

  init {
    for (i in playerQueues.indices) playerQueues[i] = PlayerDataQueue(i + 1)
  }
}

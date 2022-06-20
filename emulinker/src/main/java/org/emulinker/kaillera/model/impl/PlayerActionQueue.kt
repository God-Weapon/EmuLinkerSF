package org.emulinker.kaillera.model.impl

import java.lang.InterruptedException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.Throws
import kotlin.concurrent.withLock

class PlayerActionQueue(
    val playerNumber: Int,
    val player: KailleraUserImpl,
    numPlayers: Int,
    private val gameBufferSize: Int,
    private val gameTimeoutMillis: Int,
    capture: Boolean
) {
  var lastTimeout: PlayerTimeoutException? = null
  private val array = ByteArray(gameBufferSize)
  private val heads = IntArray(numPlayers)
  private var tail = 0

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  var synched = false
    set(value) {
      field = value
      if (!value) {
        lock.withLock { condition.signalAll() }
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
    lock.withLock { condition.signalAll() }
    lastTimeout = null
  }

  @Throws(PlayerTimeoutException::class)
  fun getAction(playerNumber: Int, actions: ByteArray, location: Int, actionLength: Int) {
    lock.withLock {
      if (getSize(playerNumber) < actionLength && synched) {
        try {
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
}

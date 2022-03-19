package org.emulinker.kaillera.controller.messaging

import java.nio.Buffer
import java.nio.ByteBuffer

abstract class ByteBufferMessage {
  private var buffer: ByteBuffer? = null

  abstract val length: Int

  private fun initBuffer() {
    initBuffer(length)
  }

  private fun initBuffer(size: Int) {
    buffer = getBuffer(size)
  }

  fun releaseBuffer() {}
  fun toBuffer(): ByteBuffer? {
    initBuffer()
    writeTo(buffer)
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    (buffer as Buffer?)!!.flip()
    return buffer
  }

  abstract fun writeTo(buffer: ByteBuffer?)

  companion object {
    fun getBuffer(size: Int): ByteBuffer {
      return ByteBuffer.allocateDirect(size)
    }

    fun releaseBuffer(buffer: ByteBuffer?) {
      // nothing to do since we aren't caching buffers anymore
    }
  }
}

package org.emulinker.kaillera.controller.v086.protocol

import java.nio.ByteBuffer
import org.emulinker.util.UnsignedUtil

abstract class ACK : V086Message() {
  abstract val val1: Long
  abstract val val2: Long
  abstract val val3: Long
  abstract val val4: Long

  override val bodyLength = 17

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    UnsignedUtil.putUnsignedInt(buffer, val1)
    UnsignedUtil.putUnsignedInt(buffer, val2)
    UnsignedUtil.putUnsignedInt(buffer, val3)
    UnsignedUtil.putUnsignedInt(buffer, val4)
  }
}

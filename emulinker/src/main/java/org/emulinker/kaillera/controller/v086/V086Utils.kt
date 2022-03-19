package org.emulinker.kaillera.controller.v086

import java.lang.StringBuilder
import java.nio.Buffer
import java.nio.ByteBuffer
import org.emulinker.kaillera.pico.AppModule

/** Util methods mostly for dealing ByteBuffers. */
object V086Utils {
  private const val HEX_STRING = "0123456789abcdef"

  object Bytes {
    const val SHORT = 2
    const val INTEGER = 4
    const val SINGLE_BYTE = 1
  }

  fun hexStringToByteBuffer(hex: String): ByteBuffer {
    var hex = hex
    hex = hex.replace(" ", "")
    val bytes = hexStringToByteArray2(hex)
    val buffer = ByteBuffer.allocate(bytes.size)
    buffer.put(bytes)
    buffer.position(0)
    return buffer
  }

  fun hexStringToByteArray2(s: String): ByteArray {
    val len = s.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
      data[i / 2] = ((s[i].digitToInt(16) shl 4) + s[i + 1].digitToInt(16)).toByte()
      i += 2
    }
    return data
  }

  fun bytesToHex(bytes: ByteArray): String {
    val hexArray = HEX_STRING.toCharArray()
    val hexChars = CharArray(bytes.size * 2)
    for (j in bytes.indices) {
      val v: Int = bytes[j].toInt() and 0xFF
      hexChars[j * 2] = hexArray[v ushr 4]
      hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
  }

  fun toHex(bb: ByteBuffer): String {
    val sb = StringBuilder()
    while (bb.hasRemaining()) {
      sb.append(String.format("%02X", bb.get()))
    }
    return sb.toString()
  }

  fun clone(original: ByteBuffer): ByteBuffer {
    val position = original.position()
    val clone = ByteBuffer.allocate(original.capacity())
    original.rewind()
    clone.put(original)
    original.rewind()
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    (clone as Buffer).flip()
    original.position(position)
    return clone
  }

  /** Gets the number of bytes to represent the string in the charset defined in emulinker.config */
  @Deprecated("You probably want getNumBytesPlusStopByte instead")
  fun getNumBytes(s: String): Int {
    return s.toByteArray(AppModule.charsetDoNotUse).size
  }

  /**
   * Gets the number of bytes to represent the string in the charset defined in emulinker.config,
   * plus one for the stop byte.
   */
  fun getNumBytesPlusStopByte(s: String): Int {
    return s.toByteArray(AppModule.charsetDoNotUse).size + 1
  }
}

package org.emulinker.util

import java.nio.ByteBuffer
import kotlin.jvm.JvmOverloads

object UnsignedUtil {
  fun getUnsignedByte(bb: ByteBuffer): Short = (bb.get().toInt() and 0xff).toShort()

  fun putUnsignedByte(bb: ByteBuffer, value: Int) {
    bb.put((value and 0xff).toByte())
  }

  fun getUnsignedByte(bb: ByteBuffer, position: Int): Short =
      (bb[position].toInt() and 0xff).toShort()

  fun putUnsignedByte(bb: ByteBuffer, position: Int, value: Int) {
    bb.put(position, (value and 0xff).toByte())
  }

  // ---------------------------------------------------------------
  fun getUnsignedShort(bb: ByteBuffer): Int {
    return bb.short.toInt() and 0xffff
  }

  fun putUnsignedShort(bb: ByteBuffer, value: Int) {
    bb.putShort((value and 0xffff).toShort())
  }

  fun getUnsignedShort(bb: ByteBuffer, position: Int): Int {
    return bb.getShort(position).toInt() and 0xffff
  }

  fun putUnsignedShort(bb: ByteBuffer, position: Int, value: Int) {
    bb.putShort(position, (value and 0xffff).toShort())
  }

  // ---------------------------------------------------------------
  fun getUnsignedInt(bb: ByteBuffer): Long {
    return bb.int.toLong() and 0xffffffffL
  }

  fun putUnsignedInt(bb: ByteBuffer, value: Long) {
    bb.putInt((value and 0xffffffffL).toInt())
  }

  fun getUnsignedInt(bb: ByteBuffer, position: Int): Long =
      bb.getInt(position).toLong() and 0xffffffffL

  fun putUnsignedInt(bb: ByteBuffer, position: Int, value: Long) {
    bb.putInt(position, (value and 0xffffffffL).toInt())
  }

  // -----------------
  fun readUnsignedByte(bytes: ByteArray, offset: Int): Short =
      (bytes[offset].toInt() and 0xFF).toShort()

  fun writeUnsignedByte(s: Short, bytes: ByteArray, offset: Int) {
    bytes[offset] = (s.toInt() and 0xFF).toByte()
  }

  @JvmOverloads
  fun readUnsignedShort(bytes: ByteArray, offset: Int, littleEndian: Boolean = false): Int =
      if (littleEndian)
          (bytes[offset + 1].toInt() and 0xFF shl 8) + (bytes[offset].toInt() and 0xFF)
      else (bytes[offset].toInt() and 0xFF shl 8) + (bytes[offset + 1].toInt() and 0xFF)

  fun writeUnsignedShort(s: Int, bytes: ByteArray?, offset: Int) {
    writeUnsignedShort(s, bytes, offset)
  }

  fun writeUnsignedShort(s: Int, bytes: ByteArray, offset: Int, littleEndian: Boolean) {
    if (littleEndian) {
      bytes[offset] = (s and 0xFF).toByte()
      bytes[offset + 1] = (s ushr 8 and 0xFF).toByte()
    } else {
      bytes[offset] = (s ushr 8 and 0xFF).toByte()
      bytes[offset + 1] = (s and 0xFF).toByte()
    }
  }

  @JvmOverloads
  fun readUnsignedInt(bytes: ByteArray, offset: Int, littleEndian: Boolean = false): Long {
    val i1: Int = bytes[offset + 0].toInt() and 0xFF
    val i2: Int = bytes[offset + 1].toInt() and 0xFF
    val i3: Int = bytes[offset + 2].toInt() and 0xFF
    val i4: Int = bytes[offset + 3].toInt() and 0xFF
    return if (littleEndian) ((i4 shl 24) + (i3 shl 16) + (i2 shl 8) + i1).toLong()
    else ((i1 shl 24) + (i2 shl 16) + (i3 shl 8) + i4).toLong()
  }

  @JvmOverloads
  fun writeUnsignedInt(i: Long, bytes: ByteArray, offset: Int, littleEndian: Boolean = false) {
    if (littleEndian) {
      bytes[offset + 0] = (i and 0xFF).toByte()
      bytes[offset + 1] = (i ushr 8 and 0xFF).toByte()
      bytes[offset + 2] = (i ushr 16 and 0xFF).toByte()
      bytes[offset + 3] = (i ushr 24 and 0xFF).toByte()
    } else {
      bytes[offset + 0] = (i ushr 24 and 0xFF).toByte()
      bytes[offset + 1] = (i ushr 16 and 0xFF).toByte()
      bytes[offset + 2] = (i ushr 8 and 0xFF).toByte()
      bytes[offset + 3] = (i and 0xFF).toByte()
    }
  }
}

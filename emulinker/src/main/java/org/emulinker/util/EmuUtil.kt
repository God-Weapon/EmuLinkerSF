package org.emulinker.util

import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.lang.InstantiationException
import java.lang.NumberFormatException
import java.lang.StringBuilder
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlin.Throws
import kotlin.jvm.JvmOverloads

object EmuUtil {
  private val HEX_CHARS =
      charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
  @JvmField val LB = System.getProperty("line.separator")
  @JvmField var DATE_FORMAT: DateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
  fun systemIsWindows(): Boolean {
    return File.separatorChar == '\\'
  }

  fun loadProperties(filename: String): Properties? {
    return try {
      val file = File(filename)
      loadProperties(file)
    } catch (e: Exception) {
      // log some kind of error here
      null
    }
  }

  fun loadProperties(file: File): Properties? {
    var p: Properties? = null
    try {
      val `in` = FileInputStream(file)
      p = Properties()
      p.load(`in`)
      `in`.close()
    } catch (e: Throwable) {
      // log the error
    }
    return p
  }

  @JvmOverloads
  fun formatBytes(data: ByteArray?, allHex: Boolean = false): String {
    if (data == null) return "null"
    if (data.isEmpty()) return ""
    if (allHex) return bytesToHex(data, ',')
    val len = data.size
    val sb = StringBuilder()
    for (i in 0 until len) {
      if (Character.isLetterOrDigit(data[i].toInt().toChar()) || data[i] in 32..126)
          sb.append(data[i].toInt().toChar())
      else sb.append(byteToHex(data[i]))
      if (i < len - 1) sb.append(',')
    }
    return sb.toString()
  }

  fun bytesToHex(data: ByteArray, sep: Char): String {
    val len = data.size
    val sb = StringBuilder(len * 3)
    for (i in 0 until len) {
      sb.append(byteToHex(data[i]))
      if (i < len - 1) sb.append(sep)
    }
    return sb.toString()
  }

  fun bytesToHex(data: ByteArray?): String {
    if (data == null) return "null"
    val len = data.size
    val sb = StringBuilder(len * 3)
    for (i in 0 until len) {
      sb.append(byteToHex(data[i]))
    }
    return sb.toString()
  }

  fun bytesToHex(data: ByteArray?, pos: Int, len: Int): String {
    if (data == null) return "null"
    val sb = StringBuilder(len * 2)
    for (i in pos until pos + len) {
      sb.append(byteToHex(data[i]))
    }
    return sb.toString()
  }

  fun byteToHex(b: Byte): String {
    return (HEX_CHARS[b.toInt() and 0xf0 shr 4].toString() +
        HEX_CHARS[b.toInt() and 0xf].toString())
  }

  @Throws(NumberFormatException::class)
  fun hexToByteArray(hex: String): ByteArray {
    if (hex.length % 2 != 0)
        throw NumberFormatException(
            "The string has the wrong length, not pairs of hex representations.")
    val len = hex.length / 2
    val ba = ByteArray(len)
    var pos = 0
    for (i in 0 until len) {
      ba[i] = hexToByte(hex.substring(pos, pos + 2).toCharArray())
      pos += 2
    }
    return ba
  }

  @Throws(NumberFormatException::class)
  fun hexToByte(hex: CharArray): Byte {
    if (hex.size != 2) throw NumberFormatException("Invalid number of digits in " + String(hex))
    var i = 0
    var nibble: Byte =
        if (hex[i] in '0'..'9') {
          (hex[i] - '0' shl 4).toByte()
        } else if (hex[i] in 'A'..'F') {
          ((hex[i] - ('A'.code - 0x0A)).code shl 4).toByte()
        } else if (hex[i] in 'a'..'f') {
          ((hex[i] - ('a'.code - 0x0A)).code shl 4).toByte()
        } else {
          throw NumberFormatException(hex[i].toString() + " is not a hexadecimal string.")
        }
    i++
    nibble =
        if (hex[i] in '0'..'9') {
          (nibble.toInt() or hex[i] - '0').toByte()
        } else if (hex[i] in 'A'..'F') {
          (nibble.toInt() or (hex[i] - ('A'.code - 0x0A)).code).toByte()
        } else if (hex[i] in 'a'..'f') {
          (nibble.toInt() or (hex[i] - ('a'.code - 0x0A)).code).toByte()
        } else {
          throw NumberFormatException(hex[i].toString() + " is not a hexadecimal string.")
        }
    return nibble
  }

  fun arrayToString(array: IntArray, sep: Char): String {
    val sb = StringBuilder()
    for (i in array.indices) {
      sb.append(array[i])
      if (i < array.size - 1) sb.append(sep)
    }
    return sb.toString()
  }

  fun arrayToString(array: ByteArray, sep: Char): String {
    val sb = StringBuilder()
    for (i in array.indices) {
      sb.append(array[i])
      if (i < array.size - 1) sb.append(sep)
    }
    return sb.toString()
  }

  fun formatSocketAddress(sa: SocketAddress): String {
    return ((sa as InetSocketAddress).address.hostAddress + ":" + sa.port)
  }

  @JvmOverloads
  fun dumpBuffer(buffer: ByteBuffer, allHex: Boolean = false): String {
    val sb = StringBuilder()
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    (buffer as Buffer).mark()
    while (buffer.hasRemaining()) {
      val b = buffer.get()
      if (!allHex && Character.isLetterOrDigit(Char(b.toUShort()))) sb.append(Char(b.toUShort()))
      else sb.append(byteToHex(b))
      if (buffer.hasRemaining()) sb.append(",")
    }
    buffer.reset()
    return sb.toString()
  }

  fun readString(buffer: ByteBuffer, stopByte: Int, charset: Charset): String {
    val tempBuffer = ByteBuffer.allocate(buffer.remaining())
    //		char[] tempArray = new char[buffer.remaining()];
    //		byte b;
    //		int  i;
    while (buffer.hasRemaining()) // 		for(i=0; i<tempArray.length; i++)
    {
      var b: Byte
      if (buffer.get().also { b = it }.toInt() == stopByte) break
      tempBuffer.put(b)
      //			tempArray[i] = (char)b;
    }
    // Cast to avoid issue with java version mismatch: https://stackoverflow.com/a/61267496/2875073
    return charset.decode((tempBuffer as Buffer).flip() as ByteBuffer).toString()
    //		return new String(tempArray, 0, i);
  }

  fun writeString(buffer: ByteBuffer, s: String, stopByte: Int, charset: Charset) {
    buffer.put(charset.encode(s))
    //		char[] tempArray = s.toCharArray();
    //		for(int i=0; i<tempArray.length; i++)
    //			buffer.put((byte) tempArray[i]);
    buffer.put(stopByte.toByte())
  }

  @Throws(InstantiationException::class)
  fun construct(className: String, args: Array<Any>): Any {
    return try {
      val c = Class.forName(className)
      val constructorArgs: Array<Class<*>?> = arrayOfNulls(args.size)
      for (i in args.indices) constructorArgs[i] = args[i].javaClass
      val constructor = c.getConstructor(*constructorArgs)
      constructor.newInstance(*args)
    } catch (e: Exception) {
      throw InstantiationException("Problem constructing new " + className + ": " + e.message)
    }
  }

  fun toSimpleUtcDatetime(instant: Instant?): String {
    return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(instant)
  }
}

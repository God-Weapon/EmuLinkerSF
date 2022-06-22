package org.emulinker.kaillera.model

enum class UserStatus(val byteValue: Byte, private val readableName: String) {
  PLAYING(0, "Playing"),
  IDLE(1, "Idle"),
  CONNECTING(2, "Connecting");

  override fun toString() = readableName

  companion object {
    fun fromByteValue(byteValue: Byte): UserStatus {
      return values().find { it.byteValue == byteValue }
          ?: throw IllegalArgumentException("Invalid byte value: $byteValue")
    }
  }
}

enum class GameStatus(val byteValue: Byte, private val readableName: String) {
  WAITING(0, "Waiting"),
  SYNCHRONIZING(1, "Synchronizing"),
  PLAYING(2, "Playing");

  override fun toString() = readableName

  companion object {
    fun fromByteValue(byteValue: Byte): GameStatus {
      return values().find { it.byteValue == byteValue }
          ?: throw IllegalArgumentException("Invalid byte value: $byteValue")
    }
  }
}

enum class ConnectionType(
    /**
     * ID for the connection type used over the wire, but has a more concrete meaning in that it
     * determines the number of updates per second while inside a game: 60/byteValue qps.
     */
    val byteValue: Byte,
    val readableName: String
) {
  DISABLED(0, "DISABLED"),
  LAN(1, "LAN"),
  EXCELLENT(2, "Excellent"),
  GOOD(3, "Good"),
  AVERAGE(4, "Average"),
  LOW(5, "Low"),
  BAD(6, "Bad");

  override fun toString() = readableName

  val updatesPerSecond = if (byteValue == 0.toByte()) 0 else 60 / byteValue

  companion object {
    fun fromByteValue(byteValue: Byte): ConnectionType {
      return values().find { it.byteValue == byteValue }
          ?: throw IllegalArgumentException("Invalid byte value: $byteValue")
    }
  }
}

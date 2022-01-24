package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.base.Strings
import java.nio.ByteBuffer
import java.util.function.Consumer
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.controller.v086.V086Utils
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser
import org.emulinker.kaillera.model.KailleraUser.Companion.CONNECTION_TYPE_NAMES
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

data class ServerStatus
    @Throws(MessageFormatException::class)
    constructor(override val messageNumber: Int, val users: List<User>, val games: List<Game>) :
    V086Message() {

  override val shortName = DESC
  override val messageId = ID

  // TODO(nue): Get rid of this.
  override fun toString(): String {
    val sb = StringBuilder()
    sb.append(infoString + "[users=" + users.size + " games=" + games.size + "]")
    if (users.isNotEmpty()) {
      sb.append(EmuUtil.LB)
    }
    for (u in users) {
      sb.append("\t" + u)
      sb.append(EmuUtil.LB)
    }
    if (games.isNotEmpty()) {
      sb.append(EmuUtil.LB)
    }
    for (g in games) {
      sb.append("\t")
      sb.append(g)
      sb.append(EmuUtil.LB)
    }
    return sb.toString()
  }

  override val bodyLength: Int
    get() {
      var len = 9
      len += users.stream().mapToInt { u: User -> u.numBytes }.sum()
      len += games.stream().mapToInt { g: Game -> g.numBytes }.sum()
      return len
    }

  public override fun writeBodyTo(buffer: ByteBuffer) {
    buffer.put(0x00.toByte())
    buffer.putInt(users.size)
    buffer.putInt(games.size)
    users.forEach(Consumer { u: User -> u.writeTo(buffer) })
    games.forEach(Consumer { g: Game -> g.writeTo(buffer) })
  }

  // TODO(nue): this User and Game class should not be here.
  data class User
      constructor(
          val username: String,
          val ping: Long,
          val status: Byte,
          val userId: Int,
          val connectionType: Byte
      ) {

    init {
      if (ping < 0 || ping > 2048)
          throw MessageFormatException("Invalid $DESC format: ping out of acceptable range: $ping")
      if (status < 0 || status > 2)
          throw MessageFormatException(
              "Invalid $DESC format: status out of acceptable range: $status")
      if (userId < 0 || userId > 65535)
          throw MessageFormatException(
              "Invalid $DESC format: userID out of acceptable range: $userId")
      if (connectionType < 1 || connectionType > 6)
          throw MessageFormatException(
              "Invalid " +
                  DESC +
                  " format: connectionType out of acceptable range: " +
                  connectionType)
    }

    // TODO(nue): Get rid of this.
    override fun toString(): String {
      return String.format(
          "[userName=%s ping=%d status=%s userID=%d connectionType=%s]",
          username,
          ping,
          KailleraUser.STATUS_NAMES[status.toInt()],
          userId,
          CONNECTION_TYPE_NAMES[connectionType.toInt()])
    }

    val numBytes: Int
      get() = V086Utils.getNumBytes(username) + 9

    fun writeTo(buffer: ByteBuffer) {
      EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
      UnsignedUtil.putUnsignedInt(buffer, ping)
      buffer.put(status)
      UnsignedUtil.putUnsignedShort(buffer, userId)
      buffer.put(connectionType)
    }
  }

  data class Game
      constructor(
          val romName: String,
          val gameId: Int,
          val clientType: String,
          val username: String,
          /**
           * Formatted like "2/4", showing the number of players present out of the max allowed in
           * the room.
           */
          val playerCountOutOfMax: String,
          val status: Byte
      ) {

    init {
      if (Strings.isNullOrEmpty(romName))
          throw MessageFormatException("Invalid $DESC format: romName.length == 0")
      if (gameId < 0 || gameId > 0xFFFF)
          throw MessageFormatException(
              "Invalid $DESC format: gameID out of acceptable range: $gameId")
      if (Strings.isNullOrEmpty(clientType))
          throw MessageFormatException("Invalid $DESC format: clientType.length == 0")
      if (Strings.isNullOrEmpty(username))
          throw MessageFormatException("Invalid $DESC format: userName.length == 0")
      if (status < 0 || status > 2)
          throw MessageFormatException(
              "Invalid $DESC format: gameStatus out of acceptable range: $status")
    }

    // TODO(nue): Get rid of this.
    override fun toString(): String {
      return ("[romName=$romName gameID=$gameId clientType=$clientType userName=$username players=$playerCountOutOfMax status=${KailleraGame.STATUS_NAMES[status.toInt()]}]")
    }

    val numBytes: Int
      get() =
          (V086Utils.getNumBytes(romName) +
              1 +
              4 +
              V086Utils.getNumBytes(clientType) +
              1 +
              V086Utils.getNumBytes(username) +
              1 +
              playerCountOutOfMax.length +
              1 +
              1)

    fun writeTo(buffer: ByteBuffer) {
      EmuUtil.writeString(buffer, romName, 0x00, AppModule.charsetDoNotUse)
      buffer.putInt(gameId)
      EmuUtil.writeString(buffer, clientType, 0x00, AppModule.charsetDoNotUse)
      EmuUtil.writeString(buffer, username, 0x00, AppModule.charsetDoNotUse)
      EmuUtil.writeString(buffer, playerCountOutOfMax, 0x00, AppModule.charsetDoNotUse)
      buffer.put(status)
    }
  }

  init {
    validateMessageNumber(messageNumber, DESC)
  }

  companion object {
    const val ID: Byte = 0x04
    private const val DESC = "Server Status"

    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, buffer: ByteBuffer): ServerStatus {
      if (buffer.remaining() < 9) {
        throw ParseException("Failed byte count validation!")
      }
      val b = buffer.get()
      if (b.toInt() != 0x00) {
        throw MessageFormatException(
            "Invalid " + DESC + " format: byte 0 = " + EmuUtil.byteToHex(b))
      }
      val numUsers = buffer.int
      val numGames = buffer.int
      val minLen = numUsers * 10 + numGames * 13
      if (buffer.remaining() < minLen) throw ParseException("Failed byte count validation!")
      val users: MutableList<User> = ArrayList(numUsers)
      for (j in 0 until numUsers) {
        if (buffer.remaining() < 9) throw ParseException("Failed byte count validation!")
        val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
        if (buffer.remaining() < 8) throw ParseException("Failed byte count validation!")
        val ping = UnsignedUtil.getUnsignedInt(buffer)
        val status = buffer.get()
        val userID = UnsignedUtil.getUnsignedShort(buffer)
        val connectionType = buffer.get()
        users.add(User(userName, ping, status, userID, connectionType))
      }
      val games: MutableList<Game> = ArrayList(numGames)
      for (j in 0 until numGames) {
        if (buffer.remaining() < 13) throw ParseException("Failed byte count validation!")
        val romName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
        if (buffer.remaining() < 10) throw ParseException("Failed byte count validation!")
        val gameID = buffer.int
        val clientType = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
        if (buffer.remaining() < 5) throw ParseException("Failed byte count validation!")
        val userName = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
        if (buffer.remaining() < 3) throw ParseException("Failed byte count validation!")
        val players = EmuUtil.readString(buffer, 0x00, AppModule.charsetDoNotUse)
        if (buffer.remaining() < 1) throw ParseException("Failed byte count validation!")
        val status = buffer.get()
        games.add(Game(romName.toString(), gameID, clientType, userName, players, status))
      }
      return ServerStatus(messageNumber, users, games)
    }
  }
}

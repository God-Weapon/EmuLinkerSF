package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.GameChat
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraUser.Companion.CONNECTION_TYPE_NAMES
import org.emulinker.kaillera.model.exception.ActionException
import org.emulinker.kaillera.model.impl.KailleraGameImpl
import org.emulinker.kaillera.model.impl.KailleraUserImpl
import org.emulinker.util.EmuLang

@Singleton
class GameOwnerCommandAction @Inject internal constructor() : V086Action<GameChat> {
  override val actionPerformedCount = 0
  override fun toString(): String {
    return DESC
  }

  fun isValidCommand(chat: String?): Boolean {
    if (chat!!.startsWith(COMMAND_HELP)) {
      return true
    } else if (chat.startsWith(COMMAND_DETECTAUTOFIRE)) {
      return true
    } else if (chat.startsWith(COMMAND_MAXUSERS)) {
      return true
    } else if (chat.startsWith(COMMAND_MAXPING)) {
      return true
    } else if (chat == COMMAND_START) {
      return true
    } else if (chat.startsWith(COMMAND_STARTN)) {
      return true
    } else if (chat.startsWith(COMMAND_MUTE)) {
      return true
    } else if (chat.startsWith(COMMAND_EMU)) {
      return true
    } else if (chat.startsWith(COMMAND_CONN)) {
      return true
    } else if (chat.startsWith(COMMAND_UNMUTE)) {
      return true
    } else if (chat.startsWith(COMMAND_SWAP)) {
      return true
    } else if (chat.startsWith(COMMAND_KICK)) {
      return true
    } else if (chat.startsWith(COMMAND_SAMEDELAY)) {
      return true
    } else if (chat.startsWith(COMMAND_LAGSTAT)) {
      return true
    } else if (chat.startsWith(COMMAND_NUM)) {
      return true
    }
    return false
  }

  @Throws(FatalActionException::class)
  override fun performAction(chatMessage: GameChat, clientHandler: V086ClientHandler?) {
    val chat = chatMessage.message
    val user = clientHandler!!.user as KailleraUserImpl
    val game =
        user.game ?: throw FatalActionException("GameOwner Command Failed: Not in a game: $chat")
    if (!user.equals(game.owner) && user.access < AccessManager.ACCESS_SUPERADMIN) {
      if (chat!!.startsWith(COMMAND_HELP)) {} else {
        logger.atWarning().log("GameOwner Command Denied: Not game owner: $game: $user: $chat")
        game.announce("GameOwner Command Error: You are not an owner!", user)
        return
      }
    }
    try {
      if (chat!!.startsWith(COMMAND_HELP)) {
        processHelp(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_DETECTAUTOFIRE)) {
        processDetectAutoFire(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_MAXUSERS)) {
        processMaxUsers(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_MAXPING)) {
        processMaxPing(chat, game, user, clientHandler)
      } else if (chat == COMMAND_START) {
        processStart(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_STARTN)) {
        processStartN(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_MUTE)) {
        processMute(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_EMU)) {
        processEmu(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_CONN)) {
        processConn(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_UNMUTE)) {
        processUnmute(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_SWAP)) {
        processSwap(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_KICK)) {
        processKick(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_LAGSTAT)) {
        processLagstat(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_SAMEDELAY)) {
        processSameDelay(chat, game, user, clientHandler)
      } else if (chat.startsWith(COMMAND_NUM)) {
        processNum(chat, game, user, clientHandler)
      } else {
        game.announce("Unknown Command: $chat", user)
        logger.atInfo().log("Unknown GameOwner Command: $game: $user: $chat")
      }
    } catch (e: ActionException) {
      logger.atInfo().withCause(e).log("GameOwner Command Failed: $game: $user: $chat")
      game.announce(EmuLang.getString("GameOwnerCommandAction.CommandFailed", e.message), user)
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to contruct message")
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processHelp(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (!admin.equals(game.owner) && admin.access < AccessManager.ACCESS_SUPERADMIN) return
    // game.setIndividualGameAnnounce(admin.getPlayerNumber());
    // game.announce(EmuLang.getString("GameOwnerCommandAction.AvailableCommands"));
    // try { Thread.sleep(20); } catch(Exception e) {}
    game.announce(EmuLang.getString("GameOwnerCommandAction.SetAutofireDetection"), admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/maxusers <#> to set capacity of room", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/maxping <#> to set maximum ping for room", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/start or /startn <#> start game when n players are joined.", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/mute /unmute  <UserID> or /muteall or /unmuteall to mute player(s).", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce(
        "/swap <order> eg. 123..n {n = total # of players; Each slot = new player#}", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/kick <Player#> or /kickall to kick a player(s).", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/setemu To restrict the gameroom to this emulator!", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce("/setconn To restrict the gameroom to this connection type!", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce(
        "/lagstat To check who has the most lag spikes or /lagreset to reset lagstat!", admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce(
        "/samedelay {true | false} to play at the same delay as player with highest ping. Default is false.",
        admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
  }

  private fun autoFireHelp(game: KailleraGameImpl, admin: KailleraUserImpl) {
    val cur = game.autoFireDetector!!.sensitivity
    game.announce(EmuLang.getString("GameOwnerCommandAction.HelpSensitivity"), admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce(EmuLang.getString("GameOwnerCommandAction.HelpDisable"), admin)
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    game.announce(
        EmuLang.getString("GameOwnerCommandAction.HelpCurrentSensitivity", cur) +
            if (cur == 0) EmuLang.getString("GameOwnerCommandAction.HelpDisabled") else "",
        admin)
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processDetectAutoFire(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (game.status != KailleraGame.STATUS_WAITING.toInt()) {
      game.announce(EmuLang.getString("GameOwnerCommandAction.AutoFireChangeDeniedInGame"), admin)
      return
    }
    val st = StringTokenizer(message, " ")
    if (st.countTokens() != 2) {
      autoFireHelp(game, admin)
      return
    }
    val command = st.nextToken()
    val sensitivityStr = st.nextToken()
    var sensitivity = -1
    try {
      sensitivity = sensitivityStr.toInt()
    } catch (e: NumberFormatException) {}
    if (sensitivity > 5 || sensitivity < 0) {
      autoFireHelp(game, admin)
      return
    }
    game.autoFireDetector!!.sensitivity = sensitivity
    game.announce(
        EmuLang.getString("GameOwnerCommandAction.HelpCurrentSensitivity", sensitivity) +
            if (sensitivity == 0) EmuLang.getString("GameOwnerCommandAction.HelpDisabled") else "",
        null)
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processEmu(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    var emu = game.owner.clientType
    if (message == "/setemu any") {
      emu = "any"
    }
    admin.game!!.aEmulator = emu!!
    admin.game!!.announce("Owner has restricted the emulator to: $emu", null)
    return
  }

  // new gameowner command /setconn
  @Throws(ActionException::class, MessageFormatException::class)
  private fun processConn(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    var conn = CONNECTION_TYPE_NAMES[game.owner.connectionType.toInt()]
    if (message == "/setconn any") {
      conn = "any"
    }
    admin.game!!.aConnection = conn
    admin.game!!.announce("Owner has restricted the connection type to: $conn", null)
    return
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processNum(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    admin.game!!.announce(game.numPlayers.toString() + " in the room!", admin)
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processLagstat(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (game.status != KailleraGame.STATUS_PLAYING.toInt())
        game.announce("Lagstat is only available during gameplay!", admin)
    if (message == "/lagstat") {
      var str = ""
      for (player in game.players) {
        if (!player!!.stealth) str = str + "P" + player.playerNumber + ": " + player.timeouts + ", "
      }
      if (str.length > 0) {
        str = str.substring(0, str.length - ", ".length)
        game.announce("$str lag spikes", null)
      }
    } else if (message == "/lagreset") {
      for (player in game.players) {
        player!!.timeouts = 0
      }
      game.announce("LagStat has been reset!", null)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processSameDelay(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (message == "/samedelay true") {
      game.sameDelay = true
      admin.game!!.announce("Players will have the same delay when game starts (restarts)!", null)
    } else {
      game.sameDelay = false
      admin.game!!.announce(
          "Players will have independent delays when game starts (restarts)!", null)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processMute(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      val str = scanner.next()
      if (str == "/muteall") {
        for (w in 1..game.players.size) {
          // do not mute owner or admin
          if (game.getPlayer(w)!!.access < AccessManager.ACCESS_ADMIN &&
              game.getPlayer(w) != game.owner) {
            game.getPlayer(w)!!.mute = true
            game.mutedUsers.add(game.getPlayer(w)!!.connectSocketAddress.address.hostAddress)
          }
        }
        admin.game!!.announce("All players have been muted!", null)
        return
      }
      val userID = scanner.nextInt()
      val user = clientHandler!!.user!!.server.getUser(userID) as KailleraUserImpl
      if (user == null) {
        admin.game!!.announce("Player doesn't exist!", admin)
        return
      }
      if (user === clientHandler.user) {
        user.game!!.announce("You can't mute yourself!", admin)
        return
      }
      if (user.access >= AccessManager.ACCESS_ADMIN &&
          admin.access != AccessManager.ACCESS_SUPERADMIN) {
        user.game!!.announce("You can't mute an Admin", admin)
        return
      }

      // mute by IP
      game.mutedUsers.add(user.connectSocketAddress.address.hostAddress)
      user.mute = true
      val user1 = clientHandler.user as KailleraUserImpl
      user1.game!!.announce(user.name + " has been muted!", null)
    } catch (e: NoSuchElementException) {
      val user = clientHandler!!.user as KailleraUserImpl
      user.game!!.announce("Mute Player Error: /mute <UserID>", admin)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processUnmute(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      val str = scanner.next()
      if (str == "/unmuteall") {
        for (w in 1..game.players.size) {
          game.getPlayer(w)!!.mute = false
          game.mutedUsers.remove(game.getPlayer(w)!!.connectSocketAddress.address.hostAddress)
        }
        admin.game!!.announce("All players have been unmuted!", null)
        return
      }
      val userID = scanner.nextInt()
      val user = clientHandler!!.user!!.server.getUser(userID) as KailleraUserImpl
      if (user == null) {
        admin.game!!.announce("Player doesn't exist!", admin)
        return
      }
      if (user === clientHandler.user) {
        user.game!!.announce("You can't unmute yourself!", admin)
        return
      }
      if (user.access >= AccessManager.ACCESS_ADMIN &&
          admin.access != AccessManager.ACCESS_SUPERADMIN) {
        user.game!!.announce("You can't unmute an Admin", admin)
        return
      }
      game.mutedUsers.remove(user.connectSocketAddress.address.hostAddress)
      user.mute = false
      val user1 = clientHandler.user as KailleraUserImpl
      user1.game!!.announce(user.name + " has been unmuted!", null)
    } catch (e: NoSuchElementException) {
      val user = clientHandler!!.user as KailleraUserImpl
      user.game!!.announce("Unmute Player Error: /unmute <UserID>", admin)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processStartN(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val num = scanner.nextInt()
      if (num > 0 && num < 101) {
        game.startN = num.toByte().toInt()
        game.announce("This game will start when $num players have joined.", null)
      } else {
        game.announce("StartN Error: Enter value between 1 and 100.", admin)
      }
    } catch (e: NoSuchElementException) {
      game.announce("Failed: /startn <#>", admin)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processSwap(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    /*if(game.getStatus() != KailleraGame.STATUS_PLAYING){
    	game.announce("Failed: wap Players can only be used during gameplay!", admin);
    	return;
    }*/
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      var i = 1
      val str: String
      scanner.next()
      val test = scanner.nextInt()
      str = Integer.toString(test)
      if (game.players.size < str.length) {
        game.announce("Failed: You can't swap more than the # of players in the room.", admin)
        return
      }
      if (test > 0) {
        var numCount = 0
        val num = IntArray(str.length)
        // before swap check numbers to prevent errors due to incorrectly entered numbers
        i = 0
        while (i < num.size) {
          num[i] = str[i].toString().toInt()
          numCount = 1
          if (num[i] == 0 || num[i] > game.players.size) break
          for (j in num.indices) {
            if (num[i] != num[j]) numCount++
          }
          i++
        }
        if (numCount == game.players.size) {
          game.swap = true
          // PlayerActionQueue temp = game.getPlayerActionQueue()[0];
          i = 0
          while (i < str.length) {
            val player = game.players[i] as KailleraUserImpl
            player.playerNumber = num[i]
            /*if(num[i] == 1){
            	game.getPlayerActionQueue()[i] = temp;
            }
            else{
            	game.getPlayerActionQueue()[i] = game.getPlayerActionQueue()[num[i]-1];
            }*/ game.announce(
                player.name + " is now Player#: " + player.playerNumber, null)
            i++
          }
        } else
            game.announce(
                "Swap Player Error: /swap <order> eg. 123..n {n = total # of players; Each slot = new player#}",
                admin)
      }
    } catch (e: NoSuchElementException) {
      game.announce(
          "Swap Player Error: /swap <order> eg. 123..n {n = total # of players; Each slot = new player#}",
          admin)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processStart(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    game.start(admin)
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processKick(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      val str = scanner.next()
      if (str == "/kickall") {
        // start kick players from last to first and don't kick owner or admin
        for (w in game.players.size downTo 1) {
          if (game.getPlayer(w)!!.access < AccessManager.ACCESS_ADMIN &&
              game.getPlayer(w) != game.owner)
              game.kick(admin, game.getPlayer(w)!!.id)
        }
        admin.game!!.announce("All players have been kicked!", null)
        return
      }
      val playerNumber = scanner.nextInt()
      if (playerNumber > 0 && playerNumber < 101) {
        if (game.getPlayer(playerNumber) != null)
            game.kick(admin, game.getPlayer(playerNumber)!!.id)
        else {
          game.announce("Player doesn't exisit!", admin)
        }
      } else {
        game.announce("Kick Player Error: Enter value between 1 and 100", admin)
      }
    } catch (e: NoSuchElementException) {
      game.announce("Failed: /kick <Player#> or /kickall to kick all players.", admin)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processMaxUsers(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (System.currentTimeMillis() - lastMaxUserChange <= 3000) {
      game.announce("Max User Command Spam Detection...Please Wait!", admin)
      lastMaxUserChange = System.currentTimeMillis()
      return
    } else {
      lastMaxUserChange = System.currentTimeMillis()
    }
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val num = scanner.nextInt()
      if (num > 0 && num < 101) {
        game.maxUsers = num
        game.announce("Max Users has been set to $num", null)
      } else {
        game.announce("Max Users Error: Enter value between 1 and 100", admin)
      }
    } catch (e: NoSuchElementException) {
      game.announce("Failed: /maxusers <#>", admin)
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processMaxPing(
      message: String?,
      game: KailleraGameImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val num = scanner.nextInt()
      if (num > 0 && num < 1001) {
        game.maxPing = num
        game.announce("Max Ping has been set to $num", null)
      } else {
        game.announce("Max Ping Error: Enter value between 1 and 1000", admin)
      }
    } catch (e: NoSuchElementException) {
      game.announce("Failed: /maxping <#>", admin)
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    const val COMMAND_HELP = "/help"
    const val COMMAND_DETECTAUTOFIRE = "/detectautofire"

    // SF MOD
    const val COMMAND_LAGSTAT = "/lag"
    const val COMMAND_MAXUSERS = "/maxusers"
    const val COMMAND_MAXPING = "/maxping"
    const val COMMAND_START = "/start"
    const val COMMAND_STARTN = "/startn"
    const val COMMAND_MUTE = "/mute"
    const val COMMAND_UNMUTE = "/unmute"
    const val COMMAND_SWAP = "/swap"
    const val COMMAND_KICK = "/kick"
    const val COMMAND_EMU = "/setemu"
    const val COMMAND_CONN = "/setconn"
    const val COMMAND_SAMEDELAY = "/samedelay"
    const val COMMAND_NUM = "/num"
    private var lastMaxUserChange: Long = 0
    private const val DESC = "GameOwnerCommandAction"
  }
}

package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import java.lang.Exception
import java.lang.Runtime
import java.lang.StringBuilder
import java.net.InetAddress
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Throws
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.Chat
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage
import org.emulinker.kaillera.model.exception.ActionException
import org.emulinker.kaillera.model.impl.KailleraGameImpl
import org.emulinker.kaillera.model.impl.KailleraServerImpl
import org.emulinker.kaillera.model.impl.KailleraUserImpl
import org.emulinker.kaillera.model.impl.Trivia
import org.emulinker.util.EmuLang
import org.emulinker.util.EmuUtil
import org.emulinker.util.WildcardStringPattern

private val logger = FluentLogger.forEnclosingClass()

private const val COMMAND_ANNOUNCE = "/announce"

private const val COMMAND_ANNOUNCEALL = "/announceall"

private const val COMMAND_ANNOUNCEGAME = "/announcegame"

private const val COMMAND_BAN = "/ban"

private const val COMMAND_CLEAR = "/clear"

private const val COMMAND_CLOSEGAME = "/closegame"

private const val COMMAND_FINDGAME = "/findgame"

private const val COMMAND_FINDUSER = "/finduser"

private const val COMMAND_HELP = "/help"

private const val COMMAND_KICK = "/kick"

private const val COMMAND_SILENCE = "/silence"

private const val COMMAND_TEMPADMIN = "/tempadmin"

private const val COMMAND_TRIVIA = "/trivia"

private const val COMMAND_VERSION = "/version"

// SF MOD
private const val COMMAND_STEALTH = "/stealth"

private const val COMMAND_TEMPELEVATED = "/tempelevated"

private const val COMMAND_TEMPMODERATOR = "/tempmoderator"

private const val DESC = "AdminCommandAction"

@Singleton
class AdminCommandAction @Inject internal constructor() : V086Action<Chat> {
  override val actionPerformedCount = 0
  override fun toString(): String {
    return DESC
  }

  fun isValidCommand(chat: String): Boolean {
    return when {
      chat.startsWith(COMMAND_ANNOUNCE) ||
          chat.startsWith(COMMAND_ANNOUNCEGAME) ||
          chat.startsWith(COMMAND_BAN) ||
          chat.startsWith(COMMAND_CLEAR) ||
          chat.startsWith(COMMAND_CLOSEGAME) ||
          chat.startsWith(COMMAND_FINDGAME) ||
          chat.startsWith(COMMAND_FINDUSER) ||
          chat.startsWith(COMMAND_HELP) ||
          chat.startsWith(COMMAND_KICK) ||
          chat.startsWith(COMMAND_SILENCE) ||
          chat.startsWith(COMMAND_STEALTH) ||
          chat.startsWith(COMMAND_TEMPADMIN) ||
          chat.startsWith(COMMAND_TEMPELEVATED) ||
          chat.startsWith(COMMAND_TEMPMODERATOR) ||
          chat.startsWith(COMMAND_TRIVIA) ||
          chat.startsWith(COMMAND_VERSION) -> true
      else -> false
    }
  }

  @Throws(FatalActionException::class)
  override fun performAction(chatMessage: Chat, clientHandler: V086ClientHandler) {
    val chat: String = chatMessage.message
    val server = clientHandler.controller.server as KailleraServerImpl
    val accessManager = server.accessManager
    val user = clientHandler.user as KailleraUserImpl
    if (accessManager.getAccess(clientHandler.remoteInetAddress) < AccessManager.ACCESS_ADMIN) {
      if (chat.startsWith(COMMAND_SILENCE) ||
          chat.startsWith(COMMAND_KICK) ||
          chat.startsWith(COMMAND_HELP) ||
          chat.startsWith(COMMAND_FINDUSER) ||
          (chat.startsWith(COMMAND_VERSION) &&
              accessManager.getAccess(clientHandler.remoteInetAddress) >
                  AccessManager.ACCESS_ELEVATED)) {
        // SF MOD - Moderators can silence and Kick
        // DO NOTHING
      } else {
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  "Admin Command Error: You are not an admin!"))
        } catch (e: MessageFormatException) {}
        throw FatalActionException("Admin Command Denied: $user does not have Admin access: $chat")
      }
    }
    logger.atInfo().log("$user: Admin Command: $chat")
    try {
      when {
        chat.startsWith(COMMAND_HELP) -> {
          processHelp(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_FINDUSER) -> {
          processFindUser(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_FINDGAME) -> {
          processFindGame(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_CLOSEGAME) -> {
          processCloseGame(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_KICK) -> {
          processKick(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_BAN) -> {
          processBan(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_TEMPELEVATED) -> {
          processTempElevated(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_TEMPMODERATOR) -> {
          processTempModerator(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_SILENCE) -> {
          processSilence(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_ANNOUNCEGAME) -> {
          processGameAnnounce(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_ANNOUNCE) -> {
          processAnnounce(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_TEMPADMIN) -> {
          processTempAdmin(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_VERSION) -> {
          processVersion(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_CLEAR) -> {
          processClear(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_STEALTH) -> {
          processStealth(chat, server, user, clientHandler)
        }
        chat.startsWith(COMMAND_TRIVIA) -> {
          processTrivia(chat, server, user, clientHandler)
        }
        else -> throw ActionException("Invalid Command: $chat")
      }
    } catch (e: ActionException) {
      logger.atSevere().withCause(e).log("Admin Command Failed: $user: $chat")
      try {
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("AdminCommandAction.Failed", e.message)))
      } catch (e2: MessageFormatException) {
        logger.atSevere().withCause(e2).log("Failed to construct InformationMessage message")
      }
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct message")
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processHelp(
      message: String?,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (admin.access == AccessManager.ACCESS_MODERATOR) return
    // clientHandler.send(InformationMessage(clientHandler.getNextMessageNumber(), "server",
    // EmuLang.getString("AdminCommandAction.AdminCommands")));
    // try { Thread.sleep(20); } catch(Exception e) {}
    clientHandler!!.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpVersion")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpKick")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpSilence")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpBan")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    if (admin.access == AccessManager.ACCESS_ADMIN) {
      clientHandler.send(
          InformationMessage(
              clientHandler.nextMessageNumber,
              "server",
              EmuLang.getString("AdminCommandAction.HelpClear")))
      try {
        Thread.sleep(20)
      } catch (e: Exception) {}
    }
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpCloseGame")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpAnnounce")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpAnnounceAll")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpAnnounceGame")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpFindUser")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            EmuLang.getString("AdminCommandAction.HelpFindGame")))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            "/triviaon to start the trivia bot- /triviapause to pause the bot- /triviaresume to resume the bot after pause- /triviasave to save the bot's scores- /triviatime <#> to change the question delay"))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            "/triviaoff to stop the bot- /triviascores to show top 3 scores- /triviawin to show a winner- /triviaupdate <IP Address> <New IP Address> to update ip address"))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    clientHandler.send(
        InformationMessage(
            clientHandler.nextMessageNumber,
            "server",
            "/stealthon /stealthoff to join a room invisibly."))
    try {
      Thread.sleep(20)
    } catch (e: Exception) {}
    if (admin.access == AccessManager.ACCESS_SUPERADMIN) {
      clientHandler.send(
          InformationMessage(
              clientHandler.nextMessageNumber,
              "server",
              "/tempelevated <UserID> <min> to give a user temporary elevated access."))
      try {
        Thread.sleep(20)
      } catch (e: Exception) {}
      clientHandler.send(
          InformationMessage(
              clientHandler.nextMessageNumber,
              "server",
              "/tempmoderator <UserID> <min> to give a user temporary moderator access."))
      try {
        Thread.sleep(20)
      } catch (e: Exception) {}
      clientHandler.send(
          InformationMessage(
              clientHandler.nextMessageNumber,
              "server",
              EmuLang.getString("AdminCommandAction.HelpTempAdmin")))
      try {
        Thread.sleep(20)
      } catch (e: Exception) {}
      clientHandler.send(
          InformationMessage(
              clientHandler.nextMessageNumber,
              "server",
              "/clear <IP Address> to remove any temp ban, silence, elevated, moderator or admin."))
      try {
        Thread.sleep(20)
      } catch (e: Exception) {}
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processFindUser(
      message: String?,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val space = message!!.indexOf(' ')
    if (space < 0) throw ActionException(EmuLang.getString("AdminCommandAction.FindUserError"))
    var foundCount = 0
    val str = message.substring(space + 1)
    // WildcardStringPattern pattern = new WildcardStringPattern
    for (user in server.users) {
      if (!user.loggedIn) continue
      if (user.name!!.lowercase(Locale.getDefault()).contains(str.lowercase(Locale.getDefault()))) {
        var msg =
            "UserID: ${user.id}, IP: ${user.connectSocketAddress.address.hostAddress}, Nick: <${user.name}>, Access: ${user.accessStr}"
        msg +=
            if (user.game == null) ""
            else ", GameID: ${user.game!!.id}, Game: ${user.game!!.romName}"

        clientHandler!!.send(InformationMessage(clientHandler.nextMessageNumber, "server", msg))
        foundCount++
      }
    }
    if (foundCount == 0)
        clientHandler!!.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("AdminCommandAction.NoUsersFound")))
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processFindGame(
      message: String?,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val space = message!!.indexOf(' ')
    if (space < 0) throw ActionException(EmuLang.getString("AdminCommandAction.FindGameError"))
    var foundCount = 0
    val pattern = WildcardStringPattern(message.substring(space + 1))
    for (game in server.games) {
      if (pattern.match(game.romName)) {
        val sb = StringBuilder()
        sb.append("GameID: ")
        sb.append(game.id)
        sb.append(", Owner: <")
        sb.append(game.owner.name)
        sb.append(">, Game: ")
        sb.append(game.romName)
        clientHandler!!.send(
            InformationMessage(clientHandler.nextMessageNumber, "server", sb.toString()))
        foundCount++
      }
    }
    if (foundCount == 0)
        clientHandler!!.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("AdminCommandAction.NoGamesFound")))
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processSilence(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val userID = scanner.nextInt()
      val minutes = scanner.nextInt()
      val user =
          server.getUser(userID) as KailleraUserImpl
              ?: throw ActionException(
                  EmuLang.getString("AdminCommandAction.UserNotFound", +userID))
      if (user.id == admin.id)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotSilenceSelf"))
      val access = server.accessManager.getAccess(user.connectSocketAddress.address)
      if (access >= AccessManager.ACCESS_ADMIN && admin.access != AccessManager.ACCESS_SUPERADMIN)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotSilenceAdmin"))
      if (access == AccessManager.ACCESS_MODERATOR &&
          admin.access == AccessManager.ACCESS_MODERATOR)
          throw ActionException("You cannot silence a moderator if you're not an admin!")
      if (admin.access == AccessManager.ACCESS_MODERATOR) {
        if (server.accessManager.isSilenced(user.socketAddress!!.address))
            throw ActionException(
                "This User has already been Silenced.  Please wait until his time expires.")
        if (minutes > 15) throw ActionException("Moderators can only silence up to 15 minutes!")
      }
      server.accessManager.addSilenced(user.connectSocketAddress.address.hostAddress, minutes)
      server.announce(
          EmuLang.getString("AdminCommandAction.Silenced", minutes, user.name), false, null)
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.SilenceError"))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processKick(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val userID = scanner.nextInt()
      val user =
          server.getUser(userID) as KailleraUserImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID))
      if (user.id == admin.id)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotKickSelf"))
      val access = server.accessManager.getAccess(user.connectSocketAddress.address)
      if (access == AccessManager.ACCESS_MODERATOR &&
          admin.access == AccessManager.ACCESS_MODERATOR)
          throw ActionException("You cannot kick a moderator if you're not an admin!")
      if (access >= AccessManager.ACCESS_ADMIN && admin.access != AccessManager.ACCESS_SUPERADMIN)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotKickAdmin"))
      user.quit(EmuLang.getString("AdminCommandAction.QuitKicked"))
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.KickError"))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processCloseGame(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val gameID = scanner.nextInt()
      val game =
          server.getGame(gameID) as KailleraGameImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.GameNotFound", gameID))
      val owner = game.owner
      val access = server.accessManager.getAccess(owner.connectSocketAddress.address)
      if (access >= AccessManager.ACCESS_ADMIN &&
          admin.access != AccessManager.ACCESS_SUPERADMIN &&
          owner.loggedIn)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotCloseAdminGame"))
      owner.quitGame()
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.CloseGameError"))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processBan(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val userID = scanner.nextInt()
      val minutes = scanner.nextInt()
      val user =
          server.getUser(userID) as KailleraUserImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID))
      if (user.id == admin.id)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotBanSelf"))
      val access = server.accessManager.getAccess(user.connectSocketAddress.address)
      if (access >= AccessManager.ACCESS_ADMIN && admin.access != AccessManager.ACCESS_SUPERADMIN)
          throw ActionException(EmuLang.getString("AdminCommandAction.CanNotBanAdmin"))
      server.announce(
          EmuLang.getString("AdminCommandAction.Banned", minutes, user.name), false, null)
      user.quit(EmuLang.getString("AdminCommandAction.QuitBanned"))
      server.accessManager.addTempBan(user.connectSocketAddress.address.hostAddress, minutes)
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.BanError"))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processTempElevated(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (admin.access != AccessManager.ACCESS_SUPERADMIN) {
      throw ActionException("Only SUPER ADMIN's can give Temp Elevated Status!")
    }
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val userID = scanner.nextInt()
      val minutes = scanner.nextInt()
      val user =
          server.getUser(userID) as KailleraUserImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID))
      if (user.id == admin.id)
          throw ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin"))
      val access = server.accessManager.getAccess(user.connectSocketAddress.address)
      if (access >= AccessManager.ACCESS_ADMIN && admin.access != AccessManager.ACCESS_SUPERADMIN)
          throw ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin"))
      else if (access == AccessManager.ACCESS_ELEVATED)
          throw ActionException("User is already elevated.")
      server.accessManager.addTempElevated(user.connectSocketAddress.address.hostAddress, minutes)
      server.announce(
          "Temp Elevated Granted: " + user.name + " for " + minutes + "min", false, null)
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("Temp Elevated Error."))
    }
  }

  // new superadmin command /tempmoderator
  @Throws(ActionException::class, MessageFormatException::class)
  private fun processTempModerator(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (admin.access != AccessManager.ACCESS_SUPERADMIN) {
      throw ActionException("Only SUPER ADMIN's can give Temp Moderator Status!")
    }
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val userID = scanner.nextInt()
      val minutes = scanner.nextInt()
      val user =
          server.getUser(userID) as KailleraUserImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID))
      if (user.id == admin.id)
          throw ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin"))
      val access = server.accessManager.getAccess(user.connectSocketAddress.address)
      if (access >= AccessManager.ACCESS_ADMIN && admin.access != AccessManager.ACCESS_SUPERADMIN)
          throw ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin"))
      else if (access == AccessManager.ACCESS_MODERATOR)
          throw ActionException("User is already moderator.")
      server.accessManager.addTempModerator(user.connectSocketAddress.address.hostAddress, minutes)
      server.announce(
          "Temp Moderator Granted: " + user.name + " for " + minutes + "min.", false, null)
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("Temp Moderator Error."))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processTempAdmin(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (admin.access != AccessManager.ACCESS_SUPERADMIN) {
      throw ActionException("Only SUPER ADMIN's can give Temp Admin Status!")
    }
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val userID = scanner.nextInt()
      val minutes = scanner.nextInt()
      val user =
          server.getUser(userID) as KailleraUserImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.UserNotFound", userID))
      if (user.id == admin.id)
          throw ActionException(EmuLang.getString("AdminCommandAction.AlreadyAdmin"))
      val access = server.accessManager.getAccess(user.connectSocketAddress.address)
      if (access >= AccessManager.ACCESS_ADMIN && admin.access != AccessManager.ACCESS_SUPERADMIN)
          throw ActionException(EmuLang.getString("AdminCommandAction.UserAlreadyAdmin"))
      server.accessManager.addTempAdmin(user.connectSocketAddress.address.hostAddress, minutes)
      server.announce(
          EmuLang.getString("AdminCommandAction.TempAdminGranted", minutes, user.name), false, null)
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.TempAdminError"))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processStealth(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (admin.game != null) throw ActionException("Can't use /stealth while in a gameroom.")
    if (message == "/stealthon") {
      admin.stealth = true
      clientHandler!!.send(
          InformationMessage(clientHandler.nextMessageNumber, "server", "Stealth Mode is on."))
    } else if (message == "/stealthoff") {
      admin.stealth = false
      clientHandler!!.send(
          InformationMessage(clientHandler.nextMessageNumber, "server", "Stealth Mode is off."))
    } else throw ActionException("Stealth Mode Error: /stealthon /stealthoff")
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processTrivia(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    if (message == "/triviareset") {
      if (server.switchTrivia) {
        server.trivia!!.saveScores(true)
        server.triviaThread!!.stop()
      }
      server.announce("<Trivia> " + "SupraTrivia has been reset!", false, null)
      val trivia = Trivia(server)
      val triviaThread = Thread(trivia)
      triviaThread.start()
      server.triviaThread = triviaThread
      server.trivia = trivia
      trivia.setTriviaPaused(false)
    } else if (message == "/triviaon") {
      if (server.switchTrivia) throw ActionException("Trivia already started!")
      server.announce("SupraTrivia has been started!", false, null)
      val trivia = Trivia(server)
      val triviaThread = Thread(trivia)
      triviaThread.start()
      server.triviaThread = triviaThread
      server.trivia = trivia
      trivia.setTriviaPaused(false)
    } else if (message == "/triviaoff") {
      if (server.trivia == null) throw ActionException("Trivia needs to be started first!")
      server.announce("SupraTrivia has been stopped!", false, null)
      server.trivia!!.saveScores(false)
      server.triviaThread!!.stop()
      server.switchTrivia = false
      server.trivia = null
    } else if (message == "/triviapause") {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      server.trivia!!.setTriviaPaused(true)
      server.announce("<Trivia> " + "SupraTrivia will be paused after this question!", false, null)
    } else if (message == "/triviaresume") {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      server.trivia!!.setTriviaPaused(false)
      server.announce("<Trivia> " + "SupraTrivia has been resumed!", false, null)
    } else if (message == "/triviasave") {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      server.trivia!!.saveScores(true)
    } else if (message == "/triviascores") {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      server.trivia!!.displayHighScores(false)
    } else if (message == "/triviawin") {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      server.trivia!!.displayHighScores(true)
    } else if (message.startsWith("/triviaupdate")) {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      val scanner = Scanner(message).useDelimiter(" ")
      try {
        scanner.next()
        val ip = scanner.next()
        val ipUpdate = scanner.next()
        if (server.trivia!!.updateIP(ip, ipUpdate)) {
          server.announce(
              "<Trivia> ${ipUpdate.subSequence(0, 4)}.... Trivia IP was updated!", false, admin)
        } else {
          server.announce(
              "<Trivia> ${ip.subSequence(0, 4)} was not found!  Error updating score!",
              false,
              admin)
        }
      } catch (e: Exception) {
        throw ActionException("Invalid Trivia Score Update!")
      }
    } else if (message.startsWith("/triviatime")) {
      if (server.trivia == null) {
        throw ActionException("Trivia needs to be started first!")
      }
      val scanner = Scanner(message).useDelimiter(" ")
      try {
        scanner.next()
        val questionTime = scanner.nextInt()
        server.trivia!!.setQuestionTime(questionTime * 1000)
        server.announce(
            "<Trivia> " + "SupraTrivia's question delay has been changed to " + questionTime + "s!",
            false,
            admin)
      } catch (e: Exception) {
        throw ActionException("Invalid Trivia Time!")
      }
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processAnnounce(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val space = message.indexOf(' ')
    if (space < 0) throw ActionException(EmuLang.getString("AdminCommandAction.AnnounceError"))
    var all = false
    if (message.startsWith(COMMAND_ANNOUNCEALL)) {
      all = true
    }
    var announcement = message.substring(space + 1)
    if (announcement.startsWith(":"))
        announcement =
            announcement.substring(
                1) // this protects against people screwing up the emulinker supraclient
    server.announce(announcement, all, null)
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processGameAnnounce(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val scanner = Scanner(message).useDelimiter(" ")
    try {
      scanner.next()
      val gameID = scanner.nextInt()
      val sb = StringBuilder()
      while (scanner.hasNext()) {
        sb.append(scanner.next())
        sb.append(" ")
      }
      val game =
          server.getGame(gameID) as KailleraGameImpl
              ?: throw ActionException(EmuLang.getString("AdminCommandAction.GameNotFound", gameID))
      game.announce(sb.toString(), null)
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.AnnounceGameError"))
    }
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processClear(
      message: String,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    val space = message.indexOf(' ')
    if (space < 0) throw ActionException(EmuLang.getString("AdminCommandAction.ClearError"))
    val addressStr = message.substring(space + 1)
    val inetAddr: InetAddress =
        try {
          InetAddress.getByName(addressStr)
        } catch (e: Exception) {
          throw ActionException(EmuLang.getString("AdminCommandAction.ClearAddressFormatError"))
        }
    if (admin.access == AccessManager.ACCESS_SUPERADMIN &&
        server.accessManager.clearTemp(inetAddr, true) ||
        admin.access == AccessManager.ACCESS_ADMIN &&
            server.accessManager.clearTemp(inetAddr, false))
        clientHandler!!.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("AdminCommandAction.ClearSuccess")))
    else
        clientHandler!!.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("AdminCommandAction.ClearNotFound")))
  }

  @Throws(ActionException::class, MessageFormatException::class)
  private fun processVersion(
      message: String?,
      server: KailleraServerImpl,
      admin: KailleraUserImpl,
      clientHandler: V086ClientHandler?
  ) {
    try {
      val releaseInfo = server.releaseInfo
      clientHandler!!.send(
          InformationMessage(
              clientHandler.nextMessageNumber,
              "server",
              "VERSION: " +
                  releaseInfo.productName +
                  ": " +
                  releaseInfo.versionString +
                  ": " +
                  EmuUtil.toSimpleUtcDatetime(releaseInfo.buildDate)))
      sleep(20.milliseconds)
      if (admin.access >= AccessManager.ACCESS_ADMIN) {
        val props = System.getProperties()
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                "JAVAVER: " + props.getProperty("java.version")))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                "JAVAVEND: " + props.getProperty("java.vendor")))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                "OSNAME: " + props.getProperty("os.name")))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                "OSARCH: " + props.getProperty("os.arch")))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                "OSVER: " + props.getProperty("os.version")))
        sleep(20.milliseconds)
        val runtime = Runtime.getRuntime()
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                "NUMPROCS: " + runtime.availableProcessors()))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber, "server", "FREEMEM: " + runtime.freeMemory()))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber, "server", "MAXMEM: " + runtime.maxMemory()))
        sleep(20.milliseconds)
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber, "server", "TOTMEM: " + runtime.totalMemory()))
        sleep(20.milliseconds)
        val env = System.getenv()
        if (EmuUtil.systemIsWindows()) {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "COMPNAME: " + env["COMPUTERNAME"]))
          sleep(20.milliseconds)
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "USER: " + env["USERNAME"]))
          sleep(20.milliseconds)
        } else {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "COMPNAME: " + env["HOSTNAME"]))
          sleep(20.milliseconds)
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "USER: " + env["USERNAME"]))
          sleep(20.milliseconds)
        }
      }
    } catch (e: NoSuchElementException) {
      throw ActionException(EmuLang.getString("AdminCommandAction.VersionError"))
    }
  }

  private fun sleep(d: Duration) {
    try {
      Thread.sleep(d.inWholeMilliseconds)
    } catch (e: Exception) {}
  }
}

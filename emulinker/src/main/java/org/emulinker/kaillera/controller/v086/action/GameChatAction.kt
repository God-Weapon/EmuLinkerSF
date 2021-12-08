package org.emulinker.kaillera.controller.v086.action

import com.google.common.base.Strings
import com.google.common.flogger.FluentLogger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.*
import org.emulinker.kaillera.lookingforgame.TwitterBroadcaster
import org.emulinker.kaillera.model.event.GameChatEvent
import org.emulinker.kaillera.model.exception.ActionException
import org.emulinker.kaillera.model.exception.GameChatException
import org.emulinker.kaillera.model.impl.KailleraUserImpl

private const val ADMIN_COMMAND_ESCAPE_STRING = "/"

private const val DESC = "GameChatAction"

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class GameChatAction
    @Inject
    internal constructor(
        private val gameOwnerCommandAction: GameOwnerCommandAction,
        private val lookingForGameReporter: TwitterBroadcaster
    ) : V086Action<GameChat_Request>, V086GameEventHandler<GameChatEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString() = DESC

  @Throws(FatalActionException::class)
  override fun performAction(message: GameChat_Request, clientHandler: V086ClientHandler) {
    if (clientHandler.user == null) {
      throw FatalActionException("User does not exist: GameChatAction $message")
    }
    if (clientHandler.user!!.game == null) return
    if (message.message.startsWith(ADMIN_COMMAND_ESCAPE_STRING)) {
      // if(clientHandler.getUser().getAccess() >= AccessManager.ACCESS_ADMIN ||
      // clientHandler.getUser().equals(clientHandler.getUser().getGame().getOwner())){
      try {
        if (gameOwnerCommandAction.isValidCommand((message as GameChat).message)) {
          gameOwnerCommandAction.performAction(message, clientHandler)
          if ((message as GameChat).message == "/help") checkCommands(message, clientHandler)
        } else checkCommands(message, clientHandler)
        return
      } catch (e: FatalActionException) {
        logger.atWarning().withCause(e).log("GameOwner command failed")
      }

      // }
    }
    actionPerformedCount++
    try {
      clientHandler.user!!.gameChat(message.message, message.messageNumber)
    } catch (e: GameChatException) {
      logger.atSevere().withCause(e).log("Failed to send game chat message")
    }
  }

  @Throws(FatalActionException::class)
  private fun checkCommands(message: V086Message, clientHandler: V086ClientHandler?) {
    var doCommand = true
    if (clientHandler!!.user!!.access < AccessManager.ACCESS_ELEVATED) {
      try {
        clientHandler.user!!.chat(":USER_COMMAND")
      } catch (e: ActionException) {
        doCommand = false
      }
    }
    if (doCommand) {
      if ((message as GameChat).message == "/msgon") {
        val user = clientHandler.user as KailleraUserImpl
        try {
          clientHandler.user!!.msg = true
          user.game!!.announce("Private messages are now on.", user)
        } catch (e: Exception) {}
        return
      } else if (message.message == "/msgoff") {
        val user = clientHandler.user as KailleraUserImpl
        try {
          clientHandler.user!!.msg = false
          user.game!!.announce("Private messages are now off.", user)
        } catch (e: Exception) {}
        return
      } else if (message.message.startsWith("/p2p")) {
        val user = clientHandler.user as KailleraUserImpl
        if (message.message == "/p2pon") {
          if (clientHandler.user!!.game!!.owner.equals(clientHandler.user)) {
            clientHandler.user!!.game!!.p2P = true
            for (u in clientHandler.user!!.game!!.players) {
              u.p2P = true
              if (u.loggedIn) {
                u.game!!.announce(
                    "This game will NOT receive any server activity during gameplay!", u)
              }
            }
          } else {
            clientHandler.user!!.p2P = true
            for (u in clientHandler.user!!.game!!.players) {
              if (u.loggedIn) {
                u.game!!.announce(
                    "${clientHandler.user!!.name} will NOT receive any server activity during gameplay!",
                    u)
              }
            }
          }
        } else if (message.message == "/p2poff") {
          if (clientHandler.user!!.game!!.owner == clientHandler.user) {
            clientHandler.user!!.game!!.p2P = false
            for (u in clientHandler.user!!.game!!.players) {
              u.p2P = false
              if (u.loggedIn) {
                u.game!!.announce(
                    "This game will NOW receive ALL server activity during gameplay!", u)
              }
            }
          } else {
            clientHandler.user!!.p2P = false
            for (u in clientHandler.user!!.game!!.players) {
              if (u.loggedIn) {
                u.game!!.announce(
                    clientHandler.user!!.name +
                        " will NOW receive ALL server activity during gameplay!",
                    u)
              }
            }
          }
        } else {
          user.game!!.announce("Failed P2P: /p2pon or /p2poff", user)
        }
        return
      } else if (message.message.startsWith("/msg")) {
        val user1 = clientHandler.user as KailleraUserImpl
        val scanner = Scanner(message.message).useDelimiter(" ")
        val access =
            clientHandler.user!!.server.accessManager.getAccess(
                clientHandler.user!!.socketAddress!!.address)
        if (access < AccessManager.ACCESS_SUPERADMIN &&
            clientHandler.user!!.server.accessManager.isSilenced(
                clientHandler.user!!.socketAddress!!.address)) {
          user1.game!!.announce("You are silenced!", user1)
          return
        }
        try {
          scanner.next()
          val userID = scanner.nextInt()
          val user = clientHandler.user!!.server.getUser(userID)
          val sb = StringBuilder()
          while (scanner.hasNext()) {
            sb.append(scanner.next())
            sb.append(" ")
          }
          if (user == null) {
            user1.game!!.announce("User not found!", user1)
            return
          }
          if (user.game != user1.game) {
            user1.game!!.announce("User not in this game!", user1)
            return
          }
          if (user === clientHandler.user) {
            user1.game!!.announce("You can't private message yourself!", user1)
            return
          }
          if (!user.msg ||
              user.searchIgnoredUsers(
                  clientHandler.user!!.connectSocketAddress.address.hostAddress)) {
            user1.game!!.announce("<" + user.name + "> Is not accepting private messages!", user1)
            return
          }
          var m = sb.toString()
          m = m.trim { it <= ' ' }
          if (Strings.isNullOrEmpty(m) || m.startsWith("�") || m.startsWith("�")) return
          if (access == AccessManager.ACCESS_NORMAL) {
            val chars = m.toCharArray()
            for (i in chars.indices) {
              if (chars[i].code < 32) {
                logger.atWarning().log("$user /msg denied: Illegal characters in message")
                user1.game!!.announce(
                    "Private Message Denied: Illegal characters in message", user1)
                return
              }
            }
            if (m.length > 320) {
              logger.atWarning().log("$user /msg denied: Message Length > 320")
              user1.game!!.announce("Private Message Denied: Message Too Long", user1)
              return
            }
          }
          user1.lastMsgID = user.id
          user.lastMsgID = user1.id

          // user1.getServer().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" +
          // clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " +
          // m, false, user1);
          // user.getServer().announce("<" + clientHandler.getUser().getName() + "> (" +
          // clientHandler.getUser().getID() + "): " + m, false, user);
          if (user1.game != null) {
            user1.game!!.announce(
                "TO: <" +
                    user.name +
                    ">(" +
                    user.id +
                    ") <" +
                    clientHandler.user!!.name +
                    "> (" +
                    clientHandler.user!!.id +
                    "): " +
                    m,
                user1)
          }
          if (user.game != null) {
            user.game!!.announce(
                "<" + clientHandler.user!!.name + "> (" + clientHandler.user!!.id + "): " + m, user)
          }
          return
        } catch (e: NoSuchElementException) {
          if (user1.lastMsgID != -1) {
            try {
              val user = clientHandler.user!!.server.getUser(user1.lastMsgID) as KailleraUserImpl
              val sb = StringBuilder()
              while (scanner.hasNext()) {
                sb.append(scanner.next())
                sb.append(" ")
              }
              if (user == null) {
                user1.game!!.announce("User not found!", user1)
                return
              }
              if (user.game != user1.game) {
                user1.game!!.announce("User not in this game!", user1)
                return
              }
              if (user === clientHandler.user) {
                user1.game!!.announce("You can't private message yourself!", user1)
                return
              }
              if (!user.msg) {
                user1.game!!.announce("<${user.name}> Is not accepting private messages!", user1)
                return
              }
              var m = sb.toString()
              m = m.trim { it <= ' ' }
              if (Strings.isNullOrEmpty(m) || m.startsWith("�") || m.startsWith("�")) return
              if (access == AccessManager.ACCESS_NORMAL) {
                val chars = m.toCharArray()
                var i = 0
                while (i < chars.size) {
                  if (chars[i].code < 32) {
                    logger.atWarning().log("$user /msg denied: Illegal characters in message")
                    user1.game!!.announce(
                        "Private Message Denied: Illegal characters in message", user1)
                    return
                  }
                  i++
                }
                if (m.length > 320) {
                  logger.atWarning().log("$user /msg denied: Message Length > 320")
                  user1.game!!.announce("Private Message Denied: Message Too Long", user1)
                  return
                }
              }

              // user1.getServer().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" +
              // clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): "
              // + m, false, user1);
              // user.getServer().announce("<" + clientHandler.getUser().getName() + "> (" +
              // clientHandler.getUser().getID() + "): " + m, false, user);
              if (user1.game != null) {
                user1.game!!.announce(
                    "TO: <${user.name}>(${user.id}) <${clientHandler.user!!.name}> (${clientHandler.user!!.id}): $m",
                    user1)
              }
              if (user.game != null) {
                user.game!!.announce(
                    "<${clientHandler.user!!.name}> (${clientHandler.user!!.id}): $m", user)
              }
              return
            } catch (e1: Exception) {
              user1.game!!.announce("Private Message Error: /msg <UserID> <message>", user1)
              return
            }
          } else {
            user1.game!!.announce("Private Message Error: /msg <UserID> <message>", user1)
            return
          }
        }
      } else if (message.message == "/ignoreall") {
        val user = clientHandler.user as KailleraUserImpl
        try {
          clientHandler.user!!.ignoreAll = true
          user.server.announce(
              clientHandler.user!!.name + " is now ignoring everyone!", false, null)
        } catch (e: Exception) {}
        return
      } else if (message.message == "/unignoreall") {
        val user = clientHandler.user as KailleraUserImpl
        try {
          clientHandler.user!!.ignoreAll = false
          user.server.announce(
              clientHandler.user!!.name + " is now unignoring everyone!", false, null)
        } catch (e: Exception) {}
        return
      } else if (message.message.startsWith("/ignore")) {
        val user1 = clientHandler.user as KailleraUserImpl
        val scanner = Scanner(message.message).useDelimiter(" ")
        try {
          scanner.next()
          val userID = scanner.nextInt()
          val user = clientHandler.user!!.server.getUser(userID)
          if (user == null) {
            user1.game!!.announce("User not found!", user1)
            return
          }
          if (user === clientHandler.user) {
            user1.game!!.announce("You can't ignore yourself!", user1)
            return
          }
          if (clientHandler.user!!.findIgnoredUser(user.connectSocketAddress.address.hostAddress)) {
            user1.game!!.announce("You can't ignore a user that is already ignored!", user1)
            return
          }
          if (user.access >= AccessManager.ACCESS_MODERATOR) {
            user1.game!!.announce("You cannot ignore a moderator or admin!", user1)
            return
          }
          clientHandler.user!!.addIgnoredUser(user.connectSocketAddress.address.hostAddress)
          user.server.announce(
              "${clientHandler.user!!.name} is now ignoring <${user.name}> ID: ${user.id}",
              false,
              null)
          return
        } catch (e: NoSuchElementException) {
          val user = clientHandler.user as KailleraUserImpl
          user.game!!.announce("Ignore User Error: /ignore <UserID>", user)
          logger
              .atInfo()
              .withCause(e)
              .log(
                  "IGNORE USER ERROR: ${user.name}: ${clientHandler.remoteSocketAddress!!.hostName}")
          return
        }
      } else if (message.message.startsWith("/unignore")) {
        val user1 = clientHandler.user as KailleraUserImpl
        val scanner = Scanner(message.message).useDelimiter(" ")
        try {
          scanner.next()
          val userID = scanner.nextInt()
          val user = clientHandler.user!!.server.getUser(userID)
          if (user == null) {
            user1.game!!.announce("User Not Found!", user1)
            return
          }
          if (!clientHandler.user!!.findIgnoredUser(
              user.connectSocketAddress.address.hostAddress)) {
            user1.game!!.announce("You can't unignore a user that isn't ignored", user1)
            return
          }
          if (clientHandler.user!!.removeIgnoredUser(
              user.connectSocketAddress.address.hostAddress, false))
              user.server.announce(
                  "${clientHandler.user!!.name} is now unignoring <${user.name}> ID: ${user.id}",
                  false,
                  null)
          else
              try {
                clientHandler.send(
                    InformationMessage(
                        clientHandler.nextMessageNumber, "server", "User Not Found!"))
              } catch (e: Exception) {}
          return
        } catch (e: NoSuchElementException) {
          val user = clientHandler.user as KailleraUserImpl
          user.game!!.announce("Unignore User Error: /ignore <UserID>", user)
          logger
              .atInfo()
              .withCause(e)
              .log(
                  "UNIGNORE USER ERROR: ${user.name}: ${clientHandler.remoteSocketAddress!!.hostName}")
          return
        }
      } else if (message.message.startsWith("/me")) {
        val space = message.message.indexOf(' ')
        if (space < 0) {
          clientHandler.user!!.game!!.announce("Invalid # of Fields!", clientHandler.user)
          return
        }
        var announcement = message.message.substring(space + 1)
        if (announcement.startsWith(":"))
            announcement =
                announcement.substring(
                    1) // this protects against people screwing up the emulinker supraclient
        val access =
            clientHandler.user!!.server.accessManager.getAccess(
                clientHandler.user!!.socketAddress!!.address)
        if (access < AccessManager.ACCESS_SUPERADMIN &&
            clientHandler.user!!.server.accessManager.isSilenced(
                clientHandler.user!!.socketAddress!!.address)) {
          clientHandler.user!!.game!!.announce("You are silenced!", clientHandler.user)
          return
        }
        if (clientHandler.user!!.server.checkMe(clientHandler.user, announcement)) {
          val m = announcement
          announcement = "*" + clientHandler.user!!.name + " " + m
          for (user in clientHandler.user!!.game!!.players) {
            user.game!!.announce(announcement, user)
          }
          return
        }
      } else if (message.message == "/help") {
        val user = clientHandler.user as KailleraUserImpl?
        user!!.game!!.announce(
            "/me <message> to make personal message eg. /me is bored ...SupraFast is bored.", user)
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        user.game!!.announce(
            "/msg <UserID> <msg> to PM somebody. /msgoff or /msgon to turn pm off | on.", user)
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        user.game!!.announce(
            "/ignore <UserID> or /unignore <UserID> or /ignoreall or /unignoreall to ignore users.",
            user)
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        user.game!!.announce(
            "/p2pon or /p2poff this option ignores all server activity during gameplay.", user)
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
      } else if (message.message == "/stop") {
        val user = clientHandler.user as KailleraUserImpl
        if (lookingForGameReporter.cancelActionsForUser(user.id)) {
          user.game!!.announce("Canceled pending tweet.", user)
        } else {
          user.game!!.announce("No pending tweets.", user)
        }
      } else
          clientHandler.user!!.game!!.announce(
              "Unknown Command: " + message.message, clientHandler.user)
    } else {
      clientHandler.user!!.game!!.announce("Denied: Flood Control", clientHandler.user)
    }
  }

  override fun handleEvent(gameChatEvent: GameChatEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    try {
      if (clientHandler.user!!.searchIgnoredUsers(
          gameChatEvent.user.connectSocketAddress.address.hostAddress))
          return
      else if (clientHandler.user!!.ignoreAll) {
        if (gameChatEvent.user.access < AccessManager.ACCESS_ADMIN &&
            gameChatEvent.user !== clientHandler.user)
            return
      }
      val m = gameChatEvent.message
      clientHandler.send(
          GameChat_Notification(clientHandler.nextMessageNumber, gameChatEvent.user.name!!, m))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct GameChat_Notification message")
    }
  }
}

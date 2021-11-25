package org.emulinker.kaillera.controller.v086.action

import com.google.common.base.Strings
import com.google.common.flogger.FluentLogger
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Throws
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.Chat_Notification
import org.emulinker.kaillera.controller.v086.protocol.Chat_Request
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage
import org.emulinker.kaillera.model.event.ChatEvent
import org.emulinker.kaillera.model.exception.ActionException
import org.emulinker.kaillera.model.impl.KailleraUserImpl
import org.emulinker.util.EmuLang
import org.emulinker.util.EmuUtil

@Singleton
class ChatAction @Inject internal constructor(private val adminCommandAction: AdminCommandAction) :
    V086Action<Chat_Request>, V086ServerEventHandler<ChatEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  @Throws(FatalActionException::class)
  override fun performAction(message: Chat_Request, clientHandler: V086ClientHandler?) {
    if (message.message!!.startsWith(ADMIN_COMMAND_ESCAPE_STRING)) {
      if (clientHandler!!.user.access > AccessManager.ACCESS_ELEVATED) {
        try {
          if (adminCommandAction.isValidCommand(message.message)) {
            adminCommandAction.performAction(message, clientHandler)
            if (message.message == "/help") checkCommands(message, clientHandler)
          } else checkCommands(message, clientHandler)
        } catch (e: FatalActionException) {
          logger.atWarning().withCause(e).log("Admin command failed")
        }
        return
      }
      checkCommands(message, clientHandler)
      return
    }
    actionPerformedCount++
    try {
      clientHandler!!.user.chat(message.message)
    } catch (e: ActionException) {
      logger
          .atInfo()
          .withCause(e)
          .log("Chat Denied: " + clientHandler!!.user + ": " + message.message)
      try {
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("ChatAction.ChatDenied", e.message)))
      } catch (e2: MessageFormatException) {
        logger.atSevere().withCause(e2).log("Failed to contruct InformationMessage message")
      }
    }
  }

  @Throws(FatalActionException::class)
  private fun checkCommands(chatMessage: Chat_Request, clientHandler: V086ClientHandler?) {
    var doCommand = true
    val userN = clientHandler!!.user as KailleraUserImpl
    if (userN.access < AccessManager.ACCESS_ELEVATED) {
      try {
        clientHandler.user.chat(":USER_COMMAND")
      } catch (e: ActionException) {
        doCommand = false
      }
    }
    if (doCommand) {
      // SF MOD - User Commands
      if (chatMessage.message == "/alivecheck") {
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  ":ALIVECHECK=EmulinkerSF Alive Check: You are still logged in."))
        } catch (e: Exception) {}
      } else if (chatMessage.message == "/version" &&
          clientHandler.user.access < AccessManager.ACCESS_ADMIN) {
        val releaseInfo = clientHandler.user.server.releaseInfo
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  "VERSION: " +
                      releaseInfo.productName +
                      ": " +
                      releaseInfo.versionString +
                      ": " +
                      EmuUtil.toSimpleUtcDatetime(releaseInfo.buildDate)))
        } catch (e: Exception) {}
      } else if (chatMessage.message == "/myip") {
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  "Your IP Address is: " +
                      clientHandler.user.connectSocketAddress.address.hostAddress))
        } catch (e: Exception) {}
      } else if (chatMessage.message == "/msgon") {
        clientHandler.user.msg = true
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "Private messages are now on."))
        } catch (e: Exception) {}
      } else if (chatMessage.message == "/msgoff") {
        clientHandler.user.msg = false
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "Private messages are now off."))
        } catch (e: Exception) {}
      } else if (chatMessage.message!!.startsWith("/me")) {
        val space = chatMessage.message!!.indexOf(' ')
        if (space < 0) {
          try {
            clientHandler.send(
                InformationMessage(
                    clientHandler.nextMessageNumber, "server", "Invalid # of Fields!"))
          } catch (e: Exception) {}
          return
        }
        var announcement = chatMessage.message!!.substring(space + 1)
        if (announcement.startsWith(":"))
            announcement =
                announcement.substring(
                    1) // this protects against people screwing up the emulinker supraclient
        val access =
            clientHandler.user.server.accessManager.getAccess(
                clientHandler.user.socketAddress!!.address)
        if (access < AccessManager.ACCESS_SUPERADMIN &&
            clientHandler.user.server.accessManager.isSilenced(
                clientHandler.user.socketAddress!!.address)) {
          try {
            clientHandler.send(
                InformationMessage(clientHandler.nextMessageNumber, "server", "You are silenced!"))
          } catch (e: Exception) {}
          return
        }
        if (clientHandler.user.server.checkMe(clientHandler.user, announcement)) {
          val m = announcement
          announcement = "*" + clientHandler.user.name + " " + m
          val user1 = clientHandler.user as KailleraUserImpl
          clientHandler.user.server.announce(announcement, true, user1)
        }
      } else if (chatMessage.message!!.startsWith("/msg")) {
        val user1 = clientHandler.user as KailleraUserImpl
        val scanner = Scanner(chatMessage.message).useDelimiter(" ")
        val access =
            clientHandler.user.server.accessManager.getAccess(
                clientHandler.user.socketAddress!!.address)
        if (access < AccessManager.ACCESS_SUPERADMIN &&
            clientHandler.user.server.accessManager.isSilenced(
                clientHandler.user.socketAddress!!.address)) {
          try {
            clientHandler.send(
                InformationMessage(clientHandler.nextMessageNumber, "server", "You are silenced!"))
          } catch (e: Exception) {}
          return
        }
        try {
          scanner.next()
          val userID = scanner.nextInt()
          val user = clientHandler.user.server.getUser(userID) as KailleraUserImpl
          val sb = StringBuilder()
          while (scanner.hasNext()) {
            sb.append(scanner.next())
            sb.append(" ")
          }
          if (user == null) {
            try {
              clientHandler.send(
                  InformationMessage(clientHandler.nextMessageNumber, "server", "User Not Found!"))
            } catch (e: Exception) {}
            return
          }
          if (user === clientHandler.user) {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber,
                      "server",
                      "You can't private message yourself!"))
            } catch (e: Exception) {}
            return
          }
          if (user.msg == false ||
              user.searchIgnoredUsers(
                  clientHandler.user.connectSocketAddress.address.hostAddress) == true) {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber,
                      "server",
                      "<" + user.name + "> Is not accepting private messages!"))
            } catch (e: Exception) {}
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
                try {
                  clientHandler.send(
                      InformationMessage(
                          clientHandler.nextMessageNumber,
                          "server",
                          "Private Message Denied: Illegal characters in message"))
                } catch (e: Exception) {}
                return
              }
            }
            if (m.length > 320) {
              logger.atWarning().log("$user /msg denied: Message Length > 320")
              try {
                clientHandler.send(
                    InformationMessage(
                        clientHandler.nextMessageNumber,
                        "server",
                        "Private Message Denied: Message Too Long"))
              } catch (e: Exception) {}
              return
            }
          }
          user1.lastMsgID = user.id
          user.lastMsgID = user1.id
          user1.server.announce(
              "TO: <" +
                  user.name +
                  ">(" +
                  user.id +
                  ") <" +
                  clientHandler.user.name +
                  "> (" +
                  clientHandler.user.id +
                  "): " +
                  m,
              false,
              user1)
          user.server.announce(
              "<" + clientHandler.user.name + "> (" + clientHandler.user.id + "): " + m,
              false,
              user)

          /*if(user1.getGame() != null){
          	user1.getGame().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user1);
          }

          if(user.getGame() != null){
          	user.getGame().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user);
          }*/
        } catch (e: NoSuchElementException) {
          if (user1.lastMsgID != -1) {
            try {
              val user = clientHandler.user.server.getUser(user1.lastMsgID) as KailleraUserImpl
              val sb = StringBuilder()
              while (scanner.hasNext()) {
                sb.append(scanner.next())
                sb.append(" ")
              }
              if (user == null) {
                try {
                  clientHandler.send(
                      InformationMessage(
                          clientHandler.nextMessageNumber, "server", "User Not Found!"))
                } catch (e1: Exception) {}
                return
              }
              if (user === clientHandler.user) {
                try {
                  clientHandler.send(
                      InformationMessage(
                          clientHandler.nextMessageNumber,
                          "server",
                          "You can't private message yourself!"))
                } catch (e1: Exception) {}
                return
              }
              if (user.msg == false) {
                try {
                  clientHandler.send(
                      InformationMessage(
                          clientHandler.nextMessageNumber,
                          "server",
                          "<" + user.name + "> Is not accepting private messages!"))
                } catch (e1: Exception) {}
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
                    try {
                      clientHandler.send(
                          InformationMessage(
                              clientHandler.nextMessageNumber,
                              "server",
                              "Private Message Denied: Illegal characters in message"))
                    } catch (e1: Exception) {}
                    return
                  }
                  i++
                }
                if (m.length > 320) {
                  logger.atWarning().log("$user /msg denied: Message Length > 320")
                  try {
                    clientHandler.send(
                        InformationMessage(
                            clientHandler.nextMessageNumber,
                            "server",
                            "Private Message Denied: Message Too Long"))
                  } catch (e1: Exception) {}
                  return
                }
              }
              user1.server.announce(
                  "TO: <" +
                      user.name +
                      ">(" +
                      user.id +
                      ") <" +
                      clientHandler.user.name +
                      "> (" +
                      clientHandler.user.id +
                      "): " +
                      m,
                  false,
                  user1)
              user.server.announce(
                  "<" + clientHandler.user.name + "> (" + clientHandler.user.id + "): " + m,
                  false,
                  user)
              /*if(user1.getGame() != null){
              	user1.getGame().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user1);
              }

              if(user.getGame() != null){
              	user.getGame().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user);
              }*/
            } catch (e1: NoSuchElementException) {
              try {
                clientHandler.send(
                    InformationMessage(
                        clientHandler.nextMessageNumber,
                        "server",
                        "Private Message Error: /msg <UserID> <message>"))
              } catch (e2: Exception) {}
              return
            }
          } else {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber,
                      "server",
                      "Private Message Error: /msg <UserID> <message>"))
            } catch (e1: Exception) {}
            return
          }
        }
      } else if (chatMessage.message == "/ignoreall") {
        val user = clientHandler.user as KailleraUserImpl
        try {
          clientHandler.user.ignoreAll = true
          user.server.announce(clientHandler.user.name + " is now ignoring everyone!", false, null)
        } catch (e: Exception) {}
      } else if (chatMessage.message == "/unignoreall") {
        val user = clientHandler.user as KailleraUserImpl
        try {
          clientHandler.user.ignoreAll = false
          user.server.announce(
              clientHandler.user.name + " is now unignoring everyone!", false, null)
        } catch (e: Exception) {}
      } else if (chatMessage.message!!.startsWith("/ignore")) {
        val scanner = Scanner(chatMessage.message).useDelimiter(" ")
        try {
          scanner.next()
          val userID = scanner.nextInt()
          val user = clientHandler.user.server.getUser(userID) as KailleraUserImpl
          if (user == null) {
            try {
              clientHandler.send(
                  InformationMessage(clientHandler.nextMessageNumber, "server", "User Not Found!"))
            } catch (e: Exception) {}
            return
          }
          if (user === clientHandler.user) {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber, "server", "You can't ignore yourself!"))
            } catch (e: Exception) {}
            return
          }
          if (clientHandler.user.findIgnoredUser(user.connectSocketAddress.address.hostAddress)) {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber,
                      "server",
                      "You can't ignore a user that is already ignored!"))
            } catch (e: Exception) {}
            return
          }
          if (user.access >= AccessManager.ACCESS_MODERATOR) {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber,
                      "server",
                      "You cannot ignore a moderator or admin!"))
            } catch (e: Exception) {}
            return
          }
          clientHandler.user.addIgnoredUser(user.connectSocketAddress.address.hostAddress)
          user.server.announce(
              clientHandler.user.name + " is now ignoring <" + user.name + "> ID: " + user.id,
              false,
              null)
        } catch (e: NoSuchElementException) {
          val user = clientHandler.user as KailleraUserImpl
          user.server.announce("Ignore User Error: /ignore <UserID>", false, user)
          logger
              .atInfo()
              .log(
                  "IGNORE USER ERROR: " +
                      user.name +
                      ": " +
                      clientHandler.remoteSocketAddress!!.hostName)
          return
        }
      } else if (chatMessage.message!!.startsWith("/unignore")) {
        val scanner = Scanner(chatMessage.message).useDelimiter(" ")
        try {
          scanner.next()
          val userID = scanner.nextInt()
          val user = clientHandler.user.server.getUser(userID) as KailleraUserImpl
          if (user == null) {
            try {
              clientHandler.send(
                  InformationMessage(clientHandler.nextMessageNumber, "server", "User Not Found!"))
            } catch (e: Exception) {}
            return
          }
          if (!clientHandler.user.findIgnoredUser(user.connectSocketAddress.address.hostAddress)) {
            try {
              clientHandler.send(
                  InformationMessage(
                      clientHandler.nextMessageNumber,
                      "server",
                      "You can't unignore a user that isn't ignored!"))
            } catch (e: Exception) {}
            return
          }
          if (clientHandler.user.removeIgnoredUser(
              user.connectSocketAddress.address.hostAddress, false) == true)
              user.server.announce(
                  clientHandler.user.name + " is now unignoring <" + user.name + "> ID: " + user.id,
                  false,
                  null)
          else
              try {
                clientHandler.send(
                    InformationMessage(
                        clientHandler.nextMessageNumber, "server", "User Not Found!"))
              } catch (e: Exception) {}
        } catch (e: NoSuchElementException) {
          val user = clientHandler.user as KailleraUserImpl
          user.server.announce("Unignore User Error: /ignore <UserID>", false, user)
          logger
              .atInfo()
              .withCause(e)
              .log(
                  "UNIGNORE USER ERROR: " +
                      user.name +
                      ": " +
                      clientHandler.remoteSocketAddress!!.hostName)
          return
        }
      } else if (chatMessage.message == "/help") {
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  "/me <message> to make personal message eg. /me is bored ...SupraFast is bored."))
        } catch (e: Exception) {}
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  "/ignore <UserID> or /unignore <UserID> or /ignoreall or /unignoreall to ignore users."))
        } catch (e: Exception) {}
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber,
                  "server",
                  "/msg <UserID> <msg> to PM somebody. /msgoff or /msgon to turn pm off | on."))
        } catch (e: Exception) {}
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        try {
          clientHandler.send(
              InformationMessage(
                  clientHandler.nextMessageNumber, "server", "/myip to get your IP Address."))
        } catch (e: Exception) {}
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        if (clientHandler.user.access == AccessManager.ACCESS_MODERATOR) {
          try {
            clientHandler.send(
                InformationMessage(
                    clientHandler.nextMessageNumber,
                    "server",
                    "/silence <UserID> <min> to silence a user. 15min max."))
          } catch (e: Exception) {}
          try {
            Thread.sleep(20)
          } catch (e: Exception) {}
          try {
            clientHandler.send(
                InformationMessage(
                    clientHandler.nextMessageNumber, "server", "/kick <UserID> to kick a user."))
          } catch (e: Exception) {}
          try {
            Thread.sleep(20)
          } catch (e: Exception) {}
        }
        if (clientHandler.user.access < AccessManager.ACCESS_ADMIN) {
          try {
            clientHandler.send(
                InformationMessage(
                    clientHandler.nextMessageNumber, "server", "/version to get server version."))
          } catch (e: Exception) {}
          try {
            Thread.sleep(20)
          } catch (e: Exception) {}
          try {
            clientHandler.send(
                InformationMessage(
                    clientHandler.nextMessageNumber,
                    "server",
                    "/finduser <Nick> to get a user's info. eg. /finduser sup ...will return SupraFast info."))
          } catch (e: Exception) {}
          try {
            Thread.sleep(20)
          } catch (e: Exception) {}
          return
        }
      } else if (chatMessage.message!!.startsWith("/finduser") &&
          clientHandler.user.access < AccessManager.ACCESS_ADMIN) {
        val space = chatMessage.message!!.indexOf(' ')
        if (space < 0) {
          try {
            clientHandler.send(
                InformationMessage(
                    clientHandler.nextMessageNumber,
                    "server",
                    "Finduser Error: /finduser <nick> eg. /finduser sup ...will return SupraFast info."))
          } catch (e: Exception) {}
          return
        }
        var foundCount = 0
        val str = chatMessage.message!!.substring(space + 1)
        // WildcardStringPattern pattern = new WildcardStringPattern
        for (user in clientHandler.user.users!!) {
          if (!user!!.loggedIn) continue
          if (user.name!!
              .lowercase(Locale.getDefault())
              .contains(str.lowercase(Locale.getDefault()))) {
            val sb = StringBuilder()
            sb.append("UserID: ")
            sb.append(user.id)
            sb.append(", Nick: <")
            sb.append(user.name)
            sb.append(">")
            sb.append(", Access: ")
            if (user.accessStr == "SuperAdmin" || user.accessStr == "Admin") {
              sb.append("Normal")
            } else {
              sb.append(user.accessStr)
            }
            if (user.game != null) {
              sb.append(", GameID: ")
              sb.append(user.game!!.id)
              sb.append(", Game: ")
              sb.append(user.game!!.romName)
            }
            try {
              clientHandler.send(
                  InformationMessage(clientHandler.nextMessageNumber, "server", sb.toString()))
            } catch (e: Exception) {}
            foundCount++
          }
        }
        if (foundCount == 0)
            try {
              clientHandler.send(
                  InformationMessage(clientHandler.nextMessageNumber, "server", "No Users Found!"))
            } catch (e: Exception) {}
      } else userN.server.announce("Unknown Command: " + chatMessage.message, false, userN)
    } else {
      userN.server.announce("Denied: Flood Control", false, userN)
    }
  }

  override fun handleEvent(chatEvent: ChatEvent, clientHandler: V086ClientHandler?) {
    handledEventCount++
    try {
      if (clientHandler!!.user.searchIgnoredUsers(
          chatEvent.user.connectSocketAddress.address.hostAddress))
          return
      else if (clientHandler!!.user.ignoreAll == true) {
        if (chatEvent.user.access < AccessManager.ACCESS_ADMIN &&
            chatEvent.user !== clientHandler.user)
            return
      }
      val m = chatEvent.message
      clientHandler!!.send(
          Chat_Notification(clientHandler.nextMessageNumber, chatEvent.user.name, m))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to contruct Chat_Notification message")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    const val ADMIN_COMMAND_ESCAPE_STRING = "/"
    private const val DESC = "ChatAction"
  }
}

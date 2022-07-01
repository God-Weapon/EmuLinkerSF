package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.ClientACK
import org.emulinker.kaillera.controller.v086.protocol.ConnectionRejected
import org.emulinker.kaillera.controller.v086.protocol.ServerACK
import org.emulinker.kaillera.controller.v086.protocol.ServerStatus
import org.emulinker.kaillera.controller.v086.protocol.ServerStatus.Game
import org.emulinker.kaillera.model.UserStatus
import org.emulinker.kaillera.model.event.ConnectedEvent
import org.emulinker.kaillera.model.event.UserEvent
import org.emulinker.kaillera.model.exception.*

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class ACKAction @Inject internal constructor() :
    V086Action<ClientACK>, V086UserEventHandler<UserEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString() = "ACKAction"

  @Throws(FatalActionException::class)
  override fun performAction(message: ClientACK, clientHandler: V086ClientHandler) {
    actionPerformedCount++
    val user = clientHandler.user
    if (user!!.loggedIn) return
    clientHandler.addSpeedMeasurement()
    if (clientHandler.speedMeasurementCount > clientHandler.numAcksForSpeedTest) {
      user.ping = clientHandler.averageNetworkSpeed
      logger
          .atFine()
          .log(
              "Calculated $user ping time: average=${clientHandler.averageNetworkSpeed}, best=${clientHandler.bestNetworkSpeed}")
      try {
        user.login()
      } catch (e: LoginException) {
        try {
          clientHandler.send(
              ConnectionRejected(
                  clientHandler.nextMessageNumber, "server", user.id, e.message ?: ""))
        } catch (e2: MessageFormatException) {
          logger.atSevere().withCause(e2).log("Failed to construct new ConnectionRejected")
        }
        throw FatalActionException("Login failed: " + e.message)
      }
    } else {
      try {
        clientHandler.send(ServerACK(clientHandler.nextMessageNumber))
      } catch (e: MessageFormatException) {
        logger.atSevere().withCause(e).log("Failed to construct new ServerACK")
        return
      }
    }
  }

  override fun handleEvent(event: UserEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    val connectedEvent = event as ConnectedEvent
    val server = connectedEvent.server
    val thisUser = connectedEvent.user
    val users: MutableList<ServerStatus.User> = ArrayList()
    val games: MutableList<ServerStatus.Game> = ArrayList()
    try {
      for (user in server.users) {
        if (user.status != UserStatus.CONNECTING && user != thisUser)
            users.add(
                ServerStatus.User(
                    user.name!!, user.ping.toLong(), user.status, user.id, user.connectionType))
      }
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct new ServerStatus.User")
      return
    }
    try {
      for (game in server.games) {
        var num = 0
        for (user in game.players) {
          if (!user.inStealthMode) num++
        }
        games.add(
            ServerStatus.Game(
                game.romName,
                game.id,
                game.clientType!!,
                game.owner.name!!,
                "$num/${game.maxUsers}",
                game.status))
      }
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct new ServerStatus.User")
      return
    }

    // Here I am attempting to fix the inherent Kaillera protocol bug that occurs when there are a
    // large number of users
    // and/or games on the server.  The size of the ServerStatus packet can be very large, and
    // depending on the router
    // settings or os config, the packet size exceeds a UDP/IP limit and gets dropped.  This results
    // in the user getting
    // half logged-in, in a weird state.

    // I am attempting to fix this by breaking the ServerStatus message up into multiple packets.
    // I'm shooting for a max
    // packet size of 1500 bytes, but since kaillera sends 3 messages per packet, the max size for a
    // single message should be 500
    var counter = 0
    var sent = false
    var usersSubList: MutableList<ServerStatus.User> = ArrayList()
    var gamesSubList: MutableList<Game> = ArrayList()
    while (users.isNotEmpty()) {
      val user = users[0]
      users.removeAt(0)
      if (counter + user.numBytes >= 300) {
        sendServerStatus(clientHandler, usersSubList, gamesSubList, counter)
        usersSubList = ArrayList()
        gamesSubList = ArrayList()
        counter = 0
        sent = true
        try {
          Thread.sleep(100)
        } catch (e: Exception) {} // SF MOD
      }
      counter += user.numBytes
      usersSubList.add(user)
    }
    while (games.isNotEmpty()) {
      val game = games[0]
      games.removeAt(0)
      if (counter + game.numBytes >= 300) {
        sendServerStatus(clientHandler, usersSubList, gamesSubList, counter)
        usersSubList = ArrayList()
        gamesSubList = ArrayList()
        counter = 0
        sent = true
        try {
          Thread.sleep(100)
        } catch (e: Exception) {} // SF MOD
      }
      counter += game.numBytes
      gamesSubList.add(game)
    }
    if (usersSubList.size > 0 || gamesSubList.size > 0 || !sent)
        sendServerStatus(clientHandler, usersSubList, gamesSubList, counter)
  }

  private fun sendServerStatus(
      clientHandler: V086ClientHandler?,
      users: List<ServerStatus.User>,
      games: List<Game>,
      counter: Int
  ) {
    val sb = StringBuilder()
    for (game in games) {
      sb.append(game.gameId)
      sb.append(",")
    }
    logger
        .atFine()
        .log(
            "Sending ServerStatus to ${clientHandler!!.user}: ${users.size} users, ${games.size} games in $counter bytes, games: $sb")
    try {
      clientHandler.send(ServerStatus(clientHandler.nextMessageNumber, users, games))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct new ServerStatus for users")
    }
  }
}

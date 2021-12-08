package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.*
import org.emulinker.kaillera.controller.v086.protocol.PlayerInformation.Player
import org.emulinker.kaillera.lookingforgame.TwitterBroadcaster
import org.emulinker.kaillera.model.event.UserJoinedGameEvent
import org.emulinker.kaillera.model.exception.JoinGameException
import org.emulinker.util.EmuLang

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class JoinGameAction
    @Inject
    internal constructor(private val lookingForGameReporter: TwitterBroadcaster) :
    V086Action<JoinGame_Request>, V086GameEventHandler<UserJoinedGameEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString() = DESC

  @Throws(FatalActionException::class)
  override fun performAction(message: JoinGame_Request, clientHandler: V086ClientHandler) {
    actionPerformedCount++
    try {
      clientHandler.user!!.joinGame(message.gameId)
    } catch (e: JoinGameException) {
      logger.atSevere().withCause(e).log("Failed to join game.")
      try {
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("JoinGameAction.JoinGameDenied", e.message)))
        clientHandler.send(
            QuitGame_Notification(
                clientHandler.nextMessageNumber,
                clientHandler.user!!.name!!,
                clientHandler.user!!.id))
      } catch (e2: MessageFormatException) {
        logger.atSevere().withCause(e2).log("Failed to construct new Message")
      }
    }
  }

  override fun handleEvent(userJoinedEvent: UserJoinedGameEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    val thisUser = clientHandler.user
    try {
      val game = userJoinedEvent.game
      val user = userJoinedEvent.user
      if (user == thisUser) {
        val players: MutableList<Player> = ArrayList()
        for (player in game.players) {
          if (player != thisUser) {
            if (!player.stealth)
                players.add(
                    PlayerInformation.Player(
                        player.name!!, player.ping.toLong(), player.id, player.connectionType))
          }
        }
        clientHandler.send(PlayerInformation(clientHandler.nextMessageNumber, players))
      }
      if (!user.stealth)
          clientHandler.send(
              JoinGame_Notification(
                  clientHandler.nextMessageNumber,
                  game.id,
                  0,
                  user.name!!,
                  user.ping.toLong(),
                  user.id,
                  user.connectionType))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct JoinGame_Notification message")
    }
    if (userJoinedEvent.game.owner!!.id != userJoinedEvent.user.id) {
      lookingForGameReporter.cancelActionsForGame(userJoinedEvent.game.id)
    }
  }

  companion object {
    private const val DESC = "JoinGameAction"
  }
}

package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.CreateGame
import org.emulinker.kaillera.controller.v086.protocol.CreateGame_Notification
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage
import org.emulinker.kaillera.controller.v086.protocol.QuitGame_Notification
import org.emulinker.kaillera.model.event.GameCreatedEvent
import org.emulinker.kaillera.model.exception.CreateGameException
import org.emulinker.kaillera.model.exception.FloodException
import org.emulinker.util.EmuLang

@Singleton
class CreateGameAction @Inject internal constructor() :
    V086Action<CreateGame>, V086ServerEventHandler<GameCreatedEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  @Throws(FatalActionException::class)
  override fun performAction(createGameMessage: CreateGame, clientHandler: V086ClientHandler?) {
    actionPerformedCount++
    try {
      clientHandler!!.user.createGame(createGameMessage.romName)
    } catch (e: CreateGameException) {
      logger
          .atInfo()
          .withCause(e)
          .log("Create Game Denied: " + clientHandler!!.user + ": " + createGameMessage.romName)
      try {
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("CreateGameAction.CreateGameDenied", e.message)))
        clientHandler.send(
            QuitGame_Notification(
                clientHandler.nextMessageNumber, clientHandler.user.name, clientHandler.user.id))
      } catch (e2: MessageFormatException) {
        logger.atSevere().withCause(e2).log("Failed to contruct message")
      }
    } catch (e: FloodException) {
      logger
          .atInfo()
          .withCause(e)
          .log("Create Game Denied: " + clientHandler!!.user + ": " + createGameMessage.romName)
      try {
        clientHandler.send(
            InformationMessage(
                clientHandler.nextMessageNumber,
                "server",
                EmuLang.getString("CreateGameAction.CreateGameDeniedFloodControl")))
        clientHandler.send(
            QuitGame_Notification(
                clientHandler.nextMessageNumber, clientHandler.user.name, clientHandler.user.id))
      } catch (e2: MessageFormatException) {
        logger.atSevere().withCause(e2).log("Failed to contruct message")
      }
    }
  }

  override fun handleEvent(gameCreatedEvent: GameCreatedEvent, clientHandler: V086ClientHandler?) {
    handledEventCount++
    try {
      val game = gameCreatedEvent.game
      val owner = game.owner
      clientHandler!!.send(
          CreateGame_Notification(
              clientHandler.nextMessageNumber,
              owner!!.name,
              game.romName,
              owner.clientType,
              game.id,
              0.toShort().toInt()))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to contruct CreateGame_Notification message")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val DESC = "CreateGameAction"
  }
}
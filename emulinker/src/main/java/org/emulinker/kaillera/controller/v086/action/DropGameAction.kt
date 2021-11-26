package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.PlayerDrop_Notification
import org.emulinker.kaillera.controller.v086.protocol.PlayerDrop_Request
import org.emulinker.kaillera.model.event.UserDroppedGameEvent
import org.emulinker.kaillera.model.exception.DropGameException

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class DropGameAction @Inject internal constructor() :
    V086Action<PlayerDrop_Request>, V086GameEventHandler<UserDroppedGameEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  @Throws(FatalActionException::class)
  override fun performAction(message: PlayerDrop_Request, clientHandler: V086ClientHandler) {
    actionPerformedCount++
    try {
      clientHandler.user!!.dropGame()
    } catch (e: DropGameException) {
      logger.atFine().withCause(e).log("Failed to drop game")
    }
  }

  override fun handleEvent(event: UserDroppedGameEvent, clientHandler: V086ClientHandler) {
    handledEventCount++
    try {
      val user = event.user
      val playerNumber = event.playerNumber
      //			clientHandler.send(PlayerDrop_Notification.create(clientHandler.getNextMessageNumber(),
      // user.getName(), (byte) game.getPlayerNumber(user)));
      if (!user.stealth)
          clientHandler.send(
              PlayerDrop_Notification(
                  clientHandler.nextMessageNumber, user.name!!, playerNumber.toByte()))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct PlayerDrop_Notification message")
    }
  }

  companion object {
    private const val DESC = "DropGameAction"
  }
}

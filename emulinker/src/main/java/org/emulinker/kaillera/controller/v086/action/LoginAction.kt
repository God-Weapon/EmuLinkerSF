package org.emulinker.kaillera.controller.v086.action

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.kaillera.access.AccessManager
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.v086.V086ClientHandler
import org.emulinker.kaillera.controller.v086.protocol.InformationMessage
import org.emulinker.kaillera.controller.v086.protocol.ServerACK
import org.emulinker.kaillera.controller.v086.protocol.UserInformation
import org.emulinker.kaillera.controller.v086.protocol.UserJoined
import org.emulinker.kaillera.model.KailleraUser
import org.emulinker.kaillera.model.event.UserJoinedEvent
import org.emulinker.kaillera.model.impl.KailleraUserImpl

@Singleton
class LoginAction @Inject internal constructor() :
    V086Action<UserInformation>, V086ServerEventHandler<UserJoinedEvent> {
  override var actionPerformedCount = 0
    private set
  override var handledEventCount = 0
    private set

  override fun toString(): String {
    return DESC
  }

  @Throws(FatalActionException::class)
  override fun performAction(userInfo: UserInformation, clientHandler: V086ClientHandler?) {
    actionPerformedCount++
    val user: KailleraUser = clientHandler!!.user!!
    user.name = userInfo.username
    user.clientType = userInfo.clientType
    user.socketAddress = clientHandler.remoteSocketAddress
    user.connectionType = userInfo.connectionType
    clientHandler.startSpeedTest()
    try {
      clientHandler.send(ServerACK(clientHandler.nextMessageNumber))
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct ServerACK message")
    }
  }

  override fun handleEvent(userJoinedEvent: UserJoinedEvent, clientHandler: V086ClientHandler?) {
    handledEventCount++
    try {
      val user = userJoinedEvent.user as KailleraUserImpl
      clientHandler!!.send(
          UserJoined(
              clientHandler.nextMessageNumber,
              user.name!!,
              user.id,
              user.ping.toLong(),
              user.connectionType))
      val thisUser = clientHandler.user as KailleraUserImpl
      if (thisUser.isEmuLinkerClient && thisUser.access >= AccessManager.ACCESS_SUPERADMIN) {
        if (user != thisUser) {
          val sb = StringBuilder()
          sb.append(":USERINFO=")
          sb.append(user.id)
          sb.append(0x02.toChar())
          sb.append(user.connectSocketAddress.address.hostAddress)
          sb.append(0x02.toChar())
          sb.append(user.accessStr)
          sb.append(0x02.toChar())
          // str = u3.getName().replace(',','.');
          // str = str.replace(';','.');
          sb.append(user.name)
          sb.append(0x02.toChar())
          sb.append(user.ping)
          sb.append(0x02.toChar())
          sb.append(user.status)
          sb.append(0x02.toChar())
          sb.append(user.connectionType.toInt())
          clientHandler.send(
              InformationMessage(clientHandler.nextMessageNumber, "server", sb.toString()))
        }
      }
    } catch (e: MessageFormatException) {
      logger.atSevere().withCause(e).log("Failed to construct UserJoined_Notification message")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
    private const val DESC = "LoginAction"
  }
}

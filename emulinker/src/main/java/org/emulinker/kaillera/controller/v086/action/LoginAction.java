package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.impl.*;

@Singleton
public class LoginAction implements V086Action, V086ServerEventHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String DESC = "LoginAction";

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  LoginAction() {}

  @Override
  public int getActionPerformedCount() {
    return actionCount;
  }

  @Override
  public int getHandledEventCount() {
    return handledCount;
  }

  @Override
  public String toString() {
    return DESC;
  }

  @Override
  public void performAction(V086Message message, V086Controller.V086ClientHandler clientHandler)
      throws FatalActionException {
    actionCount++;

    UserInformation userInfo = (UserInformation) message;
    KailleraUser user = clientHandler.getUser();
    user.setName(userInfo.username());
    user.setClientType(userInfo.clientType());
    user.setSocketAddress(clientHandler.getRemoteSocketAddress());
    user.setConnectionType(userInfo.connectionType());

    clientHandler.startSpeedTest();

    try {
      clientHandler.send(ServerACK.create(clientHandler.getNextMessageNumber()));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct ServerACK message");
    }
  }

  @Override
  public void handleEvent(ServerEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    UserJoinedEvent userJoinedEvent = (UserJoinedEvent) event;

    try {
      KailleraUserImpl user = (KailleraUserImpl) userJoinedEvent.getUser();
      clientHandler.send(
          UserJoined.create(
              clientHandler.getNextMessageNumber(),
              user.getName(),
              user.getID(),
              user.getPing(),
              (byte) user.getConnectionType()));

      KailleraUserImpl thisUser = (KailleraUserImpl) clientHandler.getUser();
      if (thisUser.isEmuLinkerClient() && thisUser.getAccess() >= AccessManager.ACCESS_SUPERADMIN) {
        if (!user.equals(thisUser)) {
          StringBuilder sb = new StringBuilder();

          sb.append(":USERINFO=");
          sb.append(user.getID());
          sb.append((char) 0x02);
          sb.append(user.getConnectSocketAddress().getAddress().getHostAddress());
          sb.append((char) 0x02);
          sb.append(user.getAccessStr());
          sb.append((char) 0x02);
          // str = u3.getName().replace(',','.');
          // str = str.replace(';','.');
          sb.append(user.getName());
          sb.append((char) 0x02);
          sb.append(user.getPing());
          sb.append((char) 0x02);
          sb.append(user.getStatus());
          sb.append((char) 0x02);
          sb.append(user.getConnectionType());

          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(), "server", sb.toString()));
        }
      }
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct UserJoined_Notification message");
    }
  }
}

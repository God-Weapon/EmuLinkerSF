package org.emulinker.kaillera.controller.v086.action;

import com.google.common.flogger.FluentLogger;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.LoginException;

@Singleton
public class ACKAction implements V086Action, V086UserEventHandler {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String DESC = "ACKAction";
  private static int numAcksForSpeedTest = 3;

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  ACKAction() {}

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

    KailleraUser user = clientHandler.getUser();
    if (user.isLoggedIn()) return;

    clientHandler.addSpeedMeasurement();

    if (clientHandler.getSpeedMeasurementCount() > numAcksForSpeedTest) {

      user.setPing(clientHandler.getAverageNetworkSpeed());

      logger.atFine().log(
          "Calculated "
              + user
              + " ping time: average="
              + clientHandler.getAverageNetworkSpeed()
              + ", best="
              + clientHandler.getBestNetworkSpeed());

      try {
        user.login();
      } catch (LoginException e) {
        try {
          clientHandler.send(
              ConnectionRejected.create(
                  clientHandler.getNextMessageNumber(), "server", user.getID(), e.getMessage()));
        } catch (MessageFormatException e2) {
          logger.atSevere().withCause(e).log("Failed to contruct new ConnectionRejected");
        }

        throw new FatalActionException("Login failed: " + e.getMessage());
      }
    } else {
      try {
        clientHandler.send(ServerACK.create(clientHandler.getNextMessageNumber()));
      } catch (MessageFormatException e) {
        logger.atSevere().withCause(e).log("Failed to contruct new ServerACK");
        return;
      }
    }
  }

  @Override
  public void handleEvent(UserEvent event, V086Controller.V086ClientHandler clientHandler) {
    handledCount++;

    ConnectedEvent connectedEvent = (ConnectedEvent) event;

    KailleraServer server = connectedEvent.getServer();
    KailleraUser thisUser = connectedEvent.getUser();

    List<ServerStatus.User> users = new ArrayList<ServerStatus.User>();
    List<ServerStatus.Game> games = new ArrayList<ServerStatus.Game>();

    try {
      for (KailleraUser user : server.getUsers()) {
        if (user.getStatus() != KailleraUser.STATUS_CONNECTING && !user.equals(thisUser))
          users.add(
              ServerStatus.User.create(
                  user.getName(),
                  user.getPing(),
                  (byte) user.getStatus(),
                  user.getID(),
                  user.getConnectionType()));
      }
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct new ServerStatus.User");
      return;
    }

    try {
      for (KailleraGame game : server.getGames()) {
        int num = 0;
        for (KailleraUser user : game.getPlayers()) {
          if (!user.getStealth()) num++;
        }
        games.add(
            ServerStatus.Game.create(
                game.getRomName(),
                game.getID(),
                game.getClientType(),
                game.getOwner().getName(),
                (num + "/" + game.getMaxUsers()),
                (byte) game.getStatus()));
      }
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct new ServerStatus.User");
      return;
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

    int counter = 0;
    boolean sent = false;

    List<ServerStatus.User> usersSubList = new ArrayList<ServerStatus.User>();
    List<ServerStatus.Game> gamesSubList = new ArrayList<ServerStatus.Game>();

    while (!users.isEmpty()) {
      ServerStatus.User user = users.get(0);
      users.remove(0);

      if ((counter + user.getNumBytes()) >= 300) {
        sendServerStatus(clientHandler, usersSubList, gamesSubList, counter);
        usersSubList = new ArrayList<ServerStatus.User>();
        gamesSubList = new ArrayList<ServerStatus.Game>();
        counter = 0;
        sent = true;
        try {
          Thread.sleep(100);
        } catch (Exception e) {
        } // SF MOD
      }

      counter += user.getNumBytes();
      usersSubList.add(user);
    }

    while (!games.isEmpty()) {
      ServerStatus.Game game = games.get(0);
      games.remove(0);

      if ((counter + game.getNumBytes()) >= 300) {
        sendServerStatus(clientHandler, usersSubList, gamesSubList, counter);
        usersSubList = new ArrayList<ServerStatus.User>();
        gamesSubList = new ArrayList<ServerStatus.Game>();
        counter = 0;
        sent = true;
        try {
          Thread.sleep(100);
        } catch (Exception e) {
        } // SF MOD
      }

      counter += game.getNumBytes();
      gamesSubList.add(game);
    }

    if ((usersSubList.size() > 0 || gamesSubList.size() > 0) || !sent)
      sendServerStatus(clientHandler, usersSubList, gamesSubList, counter);
  }

  private void sendServerStatus(
      V086Controller.V086ClientHandler clientHandler,
      List<ServerStatus.User> users,
      List<ServerStatus.Game> games,
      int counter) {
    StringBuilder sb = new StringBuilder();
    for (ServerStatus.Game game : games) {
      sb.append(game.gameId());
      sb.append(",");
    }
    logger.atFine().log(
        "Sending ServerStatus to "
            + clientHandler.getUser()
            + ": "
            + users.size()
            + " users, "
            + games.size()
            + " games in "
            + counter
            + " bytes, games: "
            + sb.toString());
    try {
      clientHandler.send(ServerStatus.create(clientHandler.getNextMessageNumber(), users, games));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct new ServerStatus for users");
    }
  }
}

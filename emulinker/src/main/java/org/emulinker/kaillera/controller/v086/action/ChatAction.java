package org.emulinker.kaillera.controller.v086.action;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.controller.messaging.MessageFormatException;
import org.emulinker.kaillera.controller.v086.V086Controller.V086ClientHandler;
import org.emulinker.kaillera.controller.v086.protocol.*;
import org.emulinker.kaillera.model.event.*;
import org.emulinker.kaillera.model.exception.ActionException;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuLang;

@Singleton
public class ChatAction implements V086Action<Chat_Request>, V086ServerEventHandler<ChatEvent> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final String ADMIN_COMMAND_ESCAPE_STRING = "/";
  private static final String DESC = "ChatAction";

  private final AdminCommandAction adminCommandAction;

  private int actionCount = 0;
  private int handledCount = 0;

  @Inject
  ChatAction(AdminCommandAction adminCommandAction) {
    this.adminCommandAction = adminCommandAction;
  }

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
  public void performAction(Chat_Request chatMessage, V086ClientHandler clientHandler)
      throws FatalActionException {
    if (chatMessage.message().startsWith(ADMIN_COMMAND_ESCAPE_STRING)) {
      if (clientHandler.getUser().getAccess() > AccessManager.ACCESS_ELEVATED) {
        try {
          if (adminCommandAction.isValidCommand(chatMessage.message())) {
            adminCommandAction.performAction(chatMessage, clientHandler);
            if (chatMessage.message().equals("/help")) checkCommands(chatMessage, clientHandler);
          } else checkCommands(chatMessage, clientHandler);
        } catch (FatalActionException e) {
          logger.atWarning().withCause(e).log("Admin command failed");
        }
        return;
      }
      checkCommands(chatMessage, clientHandler);
      return;
    }

    actionCount++;

    try {
      clientHandler.getUser().chat(chatMessage.message());
    } catch (ActionException e) {
      logger.atInfo().withCause(e).log(
          "Chat Denied: " + clientHandler.getUser() + ": " + chatMessage.message());

      try {
        clientHandler.send(
            InformationMessage.create(
                clientHandler.getNextMessageNumber(),
                "server",
                EmuLang.getString("ChatAction.ChatDenied", e.getMessage())));
      } catch (MessageFormatException e2) {
        logger.atSevere().withCause(e2).log("Failed to contruct InformationMessage message");
      }
    }
  }

  private void checkCommands(Chat_Request chatMessage, V086ClientHandler clientHandler)
      throws FatalActionException {
    boolean doCommand = true;
    KailleraUserImpl userN = (KailleraUserImpl) clientHandler.getUser();

    if (userN.getAccess() < AccessManager.ACCESS_ELEVATED) {
      try {
        clientHandler.getUser().chat(":USER_COMMAND");
      } catch (ActionException e) {
        doCommand = false;
      }
    }

    if (doCommand) {
      // SF MOD - User Commands
      if (chatMessage.message().equals("/alivecheck")) {
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  ":ALIVECHECK=EmulinkerSF Alive Check: You are still logged in."));
        } catch (Exception e) {
        }
      } else if (chatMessage.message().equals("/version")
          && clientHandler.getUser().getAccess() < AccessManager.ACCESS_ADMIN) {
        ReleaseInfo releaseInfo = clientHandler.getUser().getServer().getReleaseInfo();
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "VERSION: "
                      + releaseInfo.getProductName()
                      + ": "
                      + releaseInfo.getVersionString()
                      + ": "
                      + releaseInfo.getReleaseDate()));
        } catch (Exception e) {
        }
      } else if (chatMessage.message().equals("/myip")) {
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "Your IP Address is: "
                      + clientHandler
                          .getUser()
                          .getConnectSocketAddress()
                          .getAddress()
                          .getHostAddress()));
        } catch (Exception e) {
        }
      } else if (chatMessage.message().equals("/msgon")) {
        clientHandler.getUser().setMsg(true);
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(), "server", "Private messages are now on."));
        } catch (Exception e) {
        }
      } else if (chatMessage.message().equals("/msgoff")) {
        clientHandler.getUser().setMsg(false);
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(), "server", "Private messages are now off."));
        } catch (Exception e) {
        }
      } else if (chatMessage.message().startsWith("/me")) {
        int space = chatMessage.message().indexOf(' ');
        if (space < 0) {
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(), "server", "Invalid # of Fields!"));
          } catch (Exception e) {
          }
          return;
        }

        String announcement = chatMessage.message().substring(space + 1);
        if (announcement.startsWith(":"))
          announcement =
              announcement.substring(
                  1); // this protects against people screwing up the emulinker supraclient

        int access =
            clientHandler
                .getUser()
                .getServer()
                .getAccessManager()
                .getAccess(clientHandler.getUser().getSocketAddress().getAddress());
        if (access < AccessManager.ACCESS_SUPERADMIN
            && clientHandler
                .getUser()
                .getServer()
                .getAccessManager()
                .isSilenced(clientHandler.getUser().getSocketAddress().getAddress())) {
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(), "server", "You are silenced!"));
          } catch (Exception e) {
          }
          return;
        }

        if (clientHandler.getUser().getServer().checkMe(clientHandler.getUser(), announcement)) {
          String m = announcement;

          announcement = "*" + clientHandler.getUser().getName() + " " + m;
          KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
          clientHandler.getUser().getServer().announce(announcement, true, user1);
        }
      } else if (chatMessage.message().startsWith("/msg")) {
        KailleraUserImpl user1 = (KailleraUserImpl) clientHandler.getUser();
        Scanner scanner = new Scanner(chatMessage.message()).useDelimiter(" ");

        int access =
            clientHandler
                .getUser()
                .getServer()
                .getAccessManager()
                .getAccess(clientHandler.getUser().getSocketAddress().getAddress());
        if (access < AccessManager.ACCESS_SUPERADMIN
            && clientHandler
                .getUser()
                .getServer()
                .getAccessManager()
                .isSilenced(clientHandler.getUser().getSocketAddress().getAddress())) {
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(), "server", "You are silenced!"));
          } catch (Exception e) {
          }
          return;
        }

        try {
          scanner.next();
          int userID = scanner.nextInt();
          KailleraUserImpl user =
              (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);

          StringBuilder sb = new StringBuilder();
          while (scanner.hasNext()) {
            sb.append(scanner.next());
            sb.append(" ");
          }

          if (user == null) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(), "server", "User Not Found!"));
            } catch (Exception e) {
            }
            return;
          }

          if (user == clientHandler.getUser()) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "You can't private message yourself!"));
            } catch (Exception e) {
            }
            return;
          }

          if (user.getMsg() == false
              || user.searchIgnoredUsers(
                      clientHandler
                          .getUser()
                          .getConnectSocketAddress()
                          .getAddress()
                          .getHostAddress())
                  == true) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "<" + user.getName() + "> Is not accepting private messages!"));
            } catch (Exception e) {
            }
            return;
          }

          String m = sb.toString();

          m = m.trim();
          if (Strings.isNullOrEmpty(m) || m.startsWith("�") || m.startsWith("�")) return;

          if (access == AccessManager.ACCESS_NORMAL) {
            char[] chars = m.toCharArray();
            for (int i = 0; i < chars.length; i++) {
              if (chars[i] < 32) {
                logger.atWarning().log(user + " /msg denied: Illegal characters in message");
                try {
                  clientHandler.send(
                      InformationMessage.create(
                          clientHandler.getNextMessageNumber(),
                          "server",
                          "Private Message Denied: Illegal characters in message"));
                } catch (Exception e) {
                }
                return;
              }
            }

            if (m.length() > 320) {
              logger.atWarning().log(user + " /msg denied: Message Length > " + 320);
              try {
                clientHandler.send(
                    InformationMessage.create(
                        clientHandler.getNextMessageNumber(),
                        "server",
                        "Private Message Denied: Message Too Long"));
              } catch (Exception e) {
              }
              return;
            }
          }

          user1.setLastMsgID(user.getID());
          user.setLastMsgID(user1.getID());

          user1
              .getServer()
              .announce(
                  "TO: <"
                      + user.getName()
                      + ">("
                      + user.getID()
                      + ") <"
                      + clientHandler.getUser().getName()
                      + "> ("
                      + clientHandler.getUser().getID()
                      + "): "
                      + m,
                  false,
                  user1);
          user.getServer()
              .announce(
                  "<"
                      + clientHandler.getUser().getName()
                      + "> ("
                      + clientHandler.getUser().getID()
                      + "): "
                      + m,
                  false,
                  user);

          /*if(user1.getGame() != null){
          	user1.getGame().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user1);
          }

          if(user.getGame() != null){
          	user.getGame().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user);
          }*/
        } catch (NoSuchElementException e) {
          if (user1.getLastMsgID() != -1) {
            try {
              KailleraUserImpl user =
                  (KailleraUserImpl)
                      clientHandler.getUser().getServer().getUser(user1.getLastMsgID());

              StringBuilder sb = new StringBuilder();
              while (scanner.hasNext()) {
                sb.append(scanner.next());
                sb.append(" ");
              }

              if (user == null) {
                try {
                  clientHandler.send(
                      InformationMessage.create(
                          clientHandler.getNextMessageNumber(), "server", "User Not Found!"));
                } catch (Exception e1) {
                }
                return;
              }

              if (user == clientHandler.getUser()) {
                try {
                  clientHandler.send(
                      InformationMessage.create(
                          clientHandler.getNextMessageNumber(),
                          "server",
                          "You can't private message yourself!"));
                } catch (Exception e1) {
                }
                return;
              }

              if (user.getMsg() == false) {
                try {
                  clientHandler.send(
                      InformationMessage.create(
                          clientHandler.getNextMessageNumber(),
                          "server",
                          "<" + user.getName() + "> Is not accepting private messages!"));
                } catch (Exception e1) {
                }
                return;
              }

              String m = sb.toString();

              m = m.trim();
              if (Strings.isNullOrEmpty(m) || m.startsWith("�") || m.startsWith("�")) return;

              if (access == AccessManager.ACCESS_NORMAL) {
                char[] chars = m.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                  if (chars[i] < 32) {
                    logger.atWarning().log(user + " /msg denied: Illegal characters in message");
                    try {
                      clientHandler.send(
                          InformationMessage.create(
                              clientHandler.getNextMessageNumber(),
                              "server",
                              "Private Message Denied: Illegal characters in message"));
                    } catch (Exception e1) {
                    }
                    return;
                  }
                }

                if (m.length() > 320) {
                  logger.atWarning().log(user + " /msg denied: Message Length > " + 320);
                  try {
                    clientHandler.send(
                        InformationMessage.create(
                            clientHandler.getNextMessageNumber(),
                            "server",
                            "Private Message Denied: Message Too Long"));
                  } catch (Exception e1) {
                  }
                  return;
                }
              }

              user1
                  .getServer()
                  .announce(
                      "TO: <"
                          + user.getName()
                          + ">("
                          + user.getID()
                          + ") <"
                          + clientHandler.getUser().getName()
                          + "> ("
                          + clientHandler.getUser().getID()
                          + "): "
                          + m,
                      false,
                      user1);
              user.getServer()
                  .announce(
                      "<"
                          + clientHandler.getUser().getName()
                          + "> ("
                          + clientHandler.getUser().getID()
                          + "): "
                          + m,
                      false,
                      user);
              /*if(user1.getGame() != null){
              	user1.getGame().announce("TO: <" + user.getName() + ">(" + user.getID() + ") <" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user1);
              }

              if(user.getGame() != null){
              	user.getGame().announce("<" + clientHandler.getUser().getName() + "> (" + clientHandler.getUser().getID() + "): " + m, user);
              }*/
            } catch (NoSuchElementException e1) {
              try {
                clientHandler.send(
                    InformationMessage.create(
                        clientHandler.getNextMessageNumber(),
                        "server",
                        "Private Message Error: /msg <UserID> <message>"));
              } catch (Exception e2) {
              }
              return;
            }
          } else {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "Private Message Error: /msg <UserID> <message>"));
            } catch (Exception e1) {
            }
            return;
          }
        }
      } else if (chatMessage.message().equals("/ignoreall")) {
        KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
        try {
          clientHandler.getUser().setIgnoreAll(true);
          user.getServer()
              .announce(
                  clientHandler.getUser().getName() + " is now ignoring everyone!", false, null);
        } catch (Exception e) {
        }
      } else if (chatMessage.message().equals("/unignoreall")) {
        KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
        try {
          clientHandler.getUser().setIgnoreAll(false);
          user.getServer()
              .announce(
                  clientHandler.getUser().getName() + " is now unignoring everyone!", false, null);
        } catch (Exception e) {
        }
      } else if (chatMessage.message().startsWith("/ignore")) {
        Scanner scanner = new Scanner(chatMessage.message()).useDelimiter(" ");

        try {
          scanner.next();
          int userID = scanner.nextInt();
          KailleraUserImpl user =
              (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);

          if (user == null) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(), "server", "User Not Found!"));
            } catch (Exception e) {
            }
            return;
          }
          if (user == clientHandler.getUser()) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "You can't ignore yourself!"));
            } catch (Exception e) {
            }
            return;
          }
          if (clientHandler
              .getUser()
              .findIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress())) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "You can't ignore a user that is already ignored!"));
            } catch (Exception e) {
            }
            return;
          }
          if (user.getAccess() >= AccessManager.ACCESS_MODERATOR) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "You cannot ignore a moderator or admin!"));
            } catch (Exception e) {
            }
            return;
          }

          clientHandler
              .getUser()
              .addIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress());
          user.getServer()
              .announce(
                  clientHandler.getUser().getName()
                      + " is now ignoring <"
                      + user.getName()
                      + "> ID: "
                      + user.getID(),
                  false,
                  null);
        } catch (NoSuchElementException e) {
          KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
          user.getServer().announce("Ignore User Error: /ignore <UserID>", false, user);
          logger.atInfo().log(
              "IGNORE USER ERROR: "
                  + user.getName()
                  + ": "
                  + clientHandler.getRemoteSocketAddress().getHostName());
          return;
        }
      } else if (chatMessage.message().startsWith("/unignore")) {
        Scanner scanner = new Scanner(chatMessage.message()).useDelimiter(" ");

        try {
          scanner.next();
          int userID = scanner.nextInt();
          KailleraUserImpl user =
              (KailleraUserImpl) clientHandler.getUser().getServer().getUser(userID);

          if (user == null) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(), "server", "User Not Found!"));
            } catch (Exception e) {
            }
            return;
          }
          if (!clientHandler
              .getUser()
              .findIgnoredUser(user.getConnectSocketAddress().getAddress().getHostAddress())) {
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(),
                      "server",
                      "You can't unignore a user that isn't ignored!"));
            } catch (Exception e) {
            }
            return;
          }

          if (clientHandler
                  .getUser()
                  .removeIgnoredUser(
                      user.getConnectSocketAddress().getAddress().getHostAddress(), false)
              == true)
            user.getServer()
                .announce(
                    clientHandler.getUser().getName()
                        + " is now unignoring <"
                        + user.getName()
                        + "> ID: "
                        + user.getID(),
                    false,
                    null);
          else
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(), "server", "User Not Found!"));
            } catch (Exception e) {
            }
        } catch (NoSuchElementException e) {
          KailleraUserImpl user = (KailleraUserImpl) clientHandler.getUser();
          user.getServer().announce("Unignore User Error: /ignore <UserID>", false, user);
          logger.atInfo().withCause(e).log(
              "UNIGNORE USER ERROR: "
                  + user.getName()
                  + ": "
                  + clientHandler.getRemoteSocketAddress().getHostName());
          return;
        }

      } else if (chatMessage.message().equals("/help")) {
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "/me <message> to make personal message eg. /me is bored ...SupraFast is bored."));
        } catch (Exception e) {
        }
        try {
          Thread.sleep(20);
        } catch (Exception e) {
        }
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "/ignore <UserID> or /unignore <UserID> or /ignoreall or /unignoreall to ignore users."));
        } catch (Exception e) {
        }
        try {
          Thread.sleep(20);
        } catch (Exception e) {
        }
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(),
                  "server",
                  "/msg <UserID> <msg> to PM somebody. /msgoff or /msgon to turn pm off | on."));
        } catch (Exception e) {
        }
        try {
          Thread.sleep(20);
        } catch (Exception e) {
        }
        try {
          clientHandler.send(
              InformationMessage.create(
                  clientHandler.getNextMessageNumber(), "server", "/myip to get your IP Address."));
        } catch (Exception e) {
        }
        try {
          Thread.sleep(20);
        } catch (Exception e) {
        }
        if (clientHandler.getUser().getAccess() == AccessManager.ACCESS_MODERATOR) {
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(),
                    "server",
                    "/silence <UserID> <min> to silence a user. 15min max."));
          } catch (Exception e) {
          }
          try {
            Thread.sleep(20);
          } catch (Exception e) {
          }
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(),
                    "server",
                    "/kick <UserID> to kick a user."));
          } catch (Exception e) {
          }
          try {
            Thread.sleep(20);
          } catch (Exception e) {
          }
        }
        if (clientHandler.getUser().getAccess() < AccessManager.ACCESS_ADMIN) {
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(),
                    "server",
                    "/version to get server version."));
          } catch (Exception e) {
          }
          try {
            Thread.sleep(20);
          } catch (Exception e) {
          }
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(),
                    "server",
                    "/finduser <Nick> to get a user's info. eg. /finduser sup ...will return SupraFast info."));
          } catch (Exception e) {
          }
          try {
            Thread.sleep(20);
          } catch (Exception e) {
          }
          return;
        }
      } else if (chatMessage.message().startsWith("/finduser")
          && clientHandler.getUser().getAccess() < AccessManager.ACCESS_ADMIN) {
        int space = chatMessage.message().indexOf(' ');
        if (space < 0) {
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(),
                    "server",
                    "Finduser Error: /finduser <nick> eg. /finduser sup ...will return SupraFast info."));
          } catch (Exception e) {
          }
          return;
        }
        int foundCount = 0;
        String str = (chatMessage.message().substring(space + 1));
        // WildcardStringPattern pattern = new WildcardStringPattern
        for (KailleraUserImpl user : clientHandler.getUser().getUsers()) {
          if (!user.isLoggedIn()) continue;

          if (user.getName().toLowerCase().contains(str.toLowerCase())) {
            StringBuilder sb = new StringBuilder();
            sb.append("UserID: ");
            sb.append(user.getID());
            sb.append(", Nick: <");
            sb.append(user.getName());
            sb.append(">");
            sb.append(", Access: ");
            if (user.getAccessStr().equals("SuperAdmin") || user.getAccessStr().equals("Admin")) {
              sb.append("Normal");
            } else {
              sb.append(user.getAccessStr());
            }

            if (user.getGame() != null) {
              sb.append(", GameID: ");
              sb.append(user.getGame().getID());
              sb.append(", Game: ");
              sb.append(user.getGame().getRomName());
            }
            try {
              clientHandler.send(
                  InformationMessage.create(
                      clientHandler.getNextMessageNumber(), "server", sb.toString()));
            } catch (Exception e) {
            }
            foundCount++;
          }
        }

        if (foundCount == 0)
          try {
            clientHandler.send(
                InformationMessage.create(
                    clientHandler.getNextMessageNumber(), "server", "No Users Found!"));
          } catch (Exception e) {
          }
      } else userN.getServer().announce("Unknown Command: " + chatMessage.message(), false, userN);
    } else {
      userN.getServer().announce("Denied: Flood Control", false, userN);
    }
  }

  @Override
  public void handleEvent(ChatEvent chatEvent, V086ClientHandler clientHandler) {
    handledCount++;

    try {
      if (clientHandler
              .getUser()
              .searchIgnoredUsers(
                  chatEvent.getUser().getConnectSocketAddress().getAddress().getHostAddress())
          == true) return;
      else if (clientHandler.getUser().getIgnoreAll() == true) {
        if (chatEvent.getUser().getAccess() < AccessManager.ACCESS_ADMIN
            && chatEvent.getUser() != clientHandler.getUser()) return;
      }

      String m = chatEvent.getMessage();

      clientHandler.send(
          Chat_Notification.create(
              clientHandler.getNextMessageNumber(), chatEvent.getUser().getName(), m));
    } catch (MessageFormatException e) {
      logger.atSevere().withCause(e).log("Failed to contruct Chat_Notification message");
    }
  }
}

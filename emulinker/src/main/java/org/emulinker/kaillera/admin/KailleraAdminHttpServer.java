package org.emulinker.kaillera.admin;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.configuration.Configuration;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.model.KailleraServer;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.*;
import org.mortbay.util.InetAddrPort;

public class KailleraAdminHttpServer {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected Server appServer = new Server();

  public KailleraAdminHttpServer(
      Configuration config,
      ThreadPoolExecutor threadPool,
      ConnectController connectController,
      KailleraServer kailleraServer)
      throws Exception {
    int port = config.getInt("adminserver.httpport");
    String jspDir = config.getString("adminserver.jspdir");

    appServer = new Server();
    appServer.addListener(new InetAddrPort(port));

    ServletHttpContext context = (ServletHttpContext) appServer.getContext(jspDir);
    context.setAttribute("threadPool", threadPool);
    context.setAttribute("connectController", connectController);
    context.setAttribute("kailleraServer", kailleraServer);

    setupSecurity(context, config);

    context.addHandler(new ServletHandler());
    context.addHandler(new ResourceHandler());
    context.addHandler(new NotFoundHandler());

    context.setResourceBase(jspDir);
    context.addServlet("JSP", "*.jsp", "org.apache.jasper.servlet.JspServlet");
  }

  /**
   * Configures Basic authentication for the admin server.
   *
   * @param context the context object that is to be configured
   * @param config the configuration file data from emulinker.xml
   * @throws IOException
   */
  private void setupSecurity(ServletHttpContext context, Configuration config) throws IOException {
    boolean isSecurity = config.getBoolean("adminserver.authenticate", false);

    if (isSecurity) {
      logger.atInfo().log("Configuring admin server security.");

      String realmName = config.getString("adminserver.realmname");
      if (Strings.isNullOrEmpty(realmName) || realmName.isBlank()) {
        realmName = "Emulinker";
      }

      String userFile = config.getString("adminserver.userfile");
      if (Strings.isNullOrEmpty(userFile) || userFile.isBlank()) {
        userFile = "conf/user.properties";
      }

      logger.atInfo().log("Establishing realm " + realmName);
      logger.atInfo().log("Loading usernames and passwords from " + userFile);

      SecurityHandler sh = new SecurityHandler();
      sh.setAuthMethod("BASIC");
      sh.setName(realmName);

      context.addHandler(sh);
      SecurityConstraint sc = new SecurityConstraint();
      sc.setAuthenticate(true);
      sc.setName(SecurityConstraint.__BASIC_AUTH);
      sc.addRole("*");
      context.addSecurityConstraint("/*", sc);

      // load the usernames and passwords from a file of the
      // following form, where password is in plain text
      // user: password
      HashUserRealm hur = new HashUserRealm();
      hur.setName(realmName);
      hur.load(userFile);
      context.setRealm(hur);
    } else {
      logger.atInfo().log("Admin server security is disabled.");
    }
  }

  public void start() {
    logger.atInfo().log("Starting Web-based Admin Interface.");

    if (!appServer.isStarted()) {
      try {
        appServer.start();
      } catch (Exception e) {
        logger.atSevere().withCause(e).log("Failed to start admin server");
      }
    }
  }

  public void stop() {
    logger.atInfo().log("Stoping!");

    if (appServer.isStarted()) {
      try {
        appServer.stop();
      } catch (Exception e) {
        logger.atSevere().withCause(e).log("Failed to stop admin server");
      }
    }
  }
}

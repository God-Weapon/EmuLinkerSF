package org.emulinker.kaillera.admin

import com.google.common.flogger.FluentLogger
import java.io.IOException
import org.apache.commons.configuration.Configuration
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.model.KailleraServer
import org.mortbay.http.HashUserRealm
import org.mortbay.http.SecurityConstraint
import org.mortbay.http.handler.NotFoundHandler
import org.mortbay.http.handler.ResourceHandler
import org.mortbay.http.handler.SecurityHandler
import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.ServletHandler
import org.mortbay.jetty.servlet.ServletHttpContext
import org.mortbay.util.InetAddrPort

private val logger = FluentLogger.forEnclosingClass()

class KailleraAdminHttpServer(
    config: Configuration, connectController: ConnectController?, kailleraServer: KailleraServer?
) : AdminServer {
  private var appServer = Server()

  /**
   * Configures Basic authentication for the admin server.
   *
   * @param context the context object that is to be configured
   * @param config the configuration file data from emulinker.xml
   * @throws IOException
   */
  @Throws(IOException::class)
  private fun setupSecurity(context: ServletHttpContext, config: Configuration) {
    val isSecurity = config.getBoolean("adminserver.authenticate", false)
    if (isSecurity) {
      logger.atInfo().log("Configuring admin server security.")
      var realmName = config.getString("adminserver.realmname")
      if (realmName.isNullOrBlank() || realmName.isBlank()) {
        realmName = "Emulinker"
      }
      var userFile = config.getString("adminserver.userfile")
      if (userFile.isNullOrBlank() || userFile.isBlank()) {
        userFile = "conf/user.properties"
      }
      logger.atInfo().log("Establishing realm $realmName")
      logger.atInfo().log("Loading usernames and passwords from $userFile")
      val sh = SecurityHandler()
      sh.authMethod = "BASIC"
      sh.name = realmName
      context.addHandler(sh)
      val sc = SecurityConstraint()
      sc.isAuthenticate = true
      sc.setName(SecurityConstraint.__BASIC_AUTH)
      sc.addRole("*")
      context.addSecurityConstraint("/*", sc)

      // load the usernames and passwords from a file of the
      // following form, where password is in plain text
      // user: password
      val hur = HashUserRealm()
      hur.name = realmName
      hur.load(userFile)
      context.realm = hur
    } else {
      logger.atInfo().log("Admin server security is disabled.")
    }
  }

  override fun start() {
    logger.atInfo().log("Starting Web-based Admin Interface.")
    if (!appServer.isStarted) {
      try {
        appServer.start()
      } catch (e: Exception) {
        logger.atSevere().withCause(e).log("Failed to start admin server")
      }
    }
  }

  override fun stop() {
    logger.atInfo().log("Stoping!")
    if (appServer.isStarted) {
      try {
        appServer.stop()
      } catch (e: Exception) {
        logger.atSevere().withCause(e).log("Failed to stop admin server")
      }
    }
  }

  init {
    val port = config.getInt("adminserver.httpport")
    val jspDir = config.getString("adminserver.jspdir")
    appServer = Server()
    appServer.addListener(InetAddrPort(port))
    val context = appServer.getContext(jspDir) as ServletHttpContext
    //    context.setAttribute("threadPool", threadPool) // NUEFIXME
    context.setAttribute("connectController", connectController)
    context.setAttribute("kailleraServer", kailleraServer)
    setupSecurity(context, config)
    context.addHandler(ServletHandler())
    context.addHandler(ResourceHandler())
    context.addHandler(NotFoundHandler())
    context.resourceBase = jspDir
    context.addServlet("JSP", "*.jsp", "org.apache.jasper.servlet.JspServlet")
  }
}

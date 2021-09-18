package org.emulinker.kaillera.pico;

import com.google.common.flogger.FluentLogger;
import java.time.Instant;

public class PicoStarter {
  public static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /**
   * Main entry point for the EmuLinker Kaillera server. This method accepts no arguments. It starts
   * the pico container which reads its configuration from components.xml. The server components,
   * once started, read their configuration information from emulinker.xml. Each of those files will
   * be located by using the classpath.
   */
  public static void main(String args[]) {
    System.setProperty(
        "flogger.backend_factory",
        "com.google.common.flogger.backend.log4j2.Log4j2BackendFactory#getInstance");

    AppComponent component = DaggerAppComponent.create();

    logger.atInfo().log("EmuLinker server Starting...");
    logger.atInfo().log(component.getReleaseInfo().getWelcome());
    logger.atInfo().log("EmuLinker server is running @ " + Instant.now());

    component.getAccessManager().start();
    component.getKailleraServerController().start();
    component.getServer().start();
    component.getKailleraServer().start();
    component.getMasterListUpdaterImpl().start();
  }
}

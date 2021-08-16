package org.emulinker.kaillera.pico;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.*;
import org.emulinker.net.BindException;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.PicoUtil;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.PicoInvocationTargetInitializationException;

public class PicoStarter {
  private static Log log = LogFactory.getLog(PicoStarter.class);
  public static final String CONFIG_FILE = "components.xml";

  /**
   * Main entry point for the EmuLinker Kaillera server. This method accepts no arguments. It starts
   * the pico container which reads its configuration from components.xml. The server components,
   * once started, read their configuration information from emulinker.xml. Each of those files will
   * be located by using the classpath.
   *
   * @param args
   */
  public static void main(String args[]) {
    try {
      try {
        new PicoStarter();
      } catch (InvocationTargetException ite) {
        throw ite.getCause();
      } catch (PicoInvocationTargetInitializationException pitie) {
        throw pitie.getCause();
      }
    } catch (NoSuchElementException e) {
      log.fatal("EmuLinker server failed to start!");
      log.fatal(e);
      System.out.println("Failed to start! A required propery is missing: " + e.getMessage());
      System.exit(1);
    } catch (ConfigurationException e) {
      log.fatal("EmuLinker server failed to start!");
      log.fatal(e);
      System.out.println(
          "Failed to start! A configuration parameter is incorrect: " + e.getMessage());
      System.exit(1);
    } catch (BindException e) {
      log.fatal("EmuLinker server failed to start!");
      log.fatal(e);
      System.out.println("Failed to start! A server is already running: " + e.getMessage());
      System.exit(1);
    } catch (Throwable e) {
      log.fatal("EmuLinker server failed to start!");
      log.fatal(e);
      System.err.println(
          "Failed to start! Caught unexpected error, stacktrace follows: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private PicoContainer container;

  private PicoStarter() throws Exception {
    System.out.println("EmuLinker server Starting...");
    log.info("Loading and starting components from " + CONFIG_FILE);
    container = PicoUtil.buildContainer(null, "EmuLinker", "/" + CONFIG_FILE);

    ReleaseInfo releaseInfo = (ReleaseInfo) container.getComponentInstanceOfType(ReleaseInfo.class);
    System.out.println(releaseInfo.getWelcome());
    System.out.println("EmuLinker server is running @ " + new Date());
  }

  public PicoContainer getContainer() {
    return container;
  }
}

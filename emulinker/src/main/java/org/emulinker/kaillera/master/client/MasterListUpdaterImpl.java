package org.emulinker.kaillera.master.client;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.*;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.*;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.release.*;
import org.emulinker.util.Executable;
import org.picocontainer.Startable;

public class MasterListUpdaterImpl implements MasterListUpdater, Executable, Startable {
  private static Log log = LogFactory.getLog(MasterListUpdaterImpl.class);

  private ThreadPoolExecutor threadPool;
  private ConnectController connectController;
  private KailleraServer kailleraServer;
  private StatsCollector statsCollector;
  private ReleaseInfo releaseInfo;

  private PublicServerInformation publicInfo;

  private boolean touchKaillera = false;
  private boolean touchEmulinker = false;

  private EmuLinkerMasterUpdateTask emulinkerMasterTask;
  private KailleraMasterUpdateTask kailleraMasterTask;

  private boolean stopFlag = false;
  private boolean isRunning = false;

  public MasterListUpdaterImpl(
      Configuration config,
      ThreadPoolExecutor threadPool,
      ConnectController connectController,
      KailleraServer kailleraServer,
      StatsCollector statsCollector,
      ReleaseInfo releaseInfo)
      throws Exception {
    this.threadPool = threadPool;
    this.connectController = connectController;
    this.kailleraServer = kailleraServer;
    this.statsCollector = statsCollector;
    this.releaseInfo = releaseInfo;

    touchKaillera = config.getBoolean("masterList.touchKaillera", false);
    touchEmulinker = config.getBoolean("masterList.touchEmulinker", false);

    if (touchKaillera || touchEmulinker) publicInfo = new PublicServerInformation(config);

    if (touchKaillera)
      kailleraMasterTask =
          new KailleraMasterUpdateTask(
              publicInfo, connectController, kailleraServer, statsCollector, releaseInfo);

    if (touchEmulinker)
      emulinkerMasterTask =
          new EmuLinkerMasterUpdateTask(publicInfo, connectController, kailleraServer, releaseInfo);
  }

  public synchronized boolean isRunning() {
    return isRunning;
  }

  public synchronized String toString() {
    return "MasterListUpdaterImpl[touchKaillera="
        + touchKaillera
        + " touchEmulinker="
        + touchEmulinker
        + "]";
  }

  public synchronized void start() {
    if (publicInfo != null) {
      log.debug("MasterListUpdater thread received start request!");
      log.debug(
          "MasterListUpdater thread starting (ThreadPool:"
              + threadPool.getActiveCount()
              + "/"
              + threadPool.getPoolSize()
              + ")");
      threadPool.execute(this);
      Thread.yield();
      log.debug(
          "MasterListUpdater thread started (ThreadPool:"
              + threadPool.getActiveCount()
              + "/"
              + threadPool.getPoolSize()
              + ")");
    }
  }

  public synchronized void stop() {
    if (publicInfo != null) {
      log.debug("MasterListUpdater thread received stop request!");

      if (!isRunning()) {
        log.debug("MasterListUpdater thread stop request ignored: not running!");
        return;
      }

      stopFlag = true;
    }
  }

  public void run() {
    isRunning = true;
    log.debug("MasterListUpdater thread running...");

    try {
      while (!stopFlag) {
        try {
          Thread.sleep(60000);
        } catch (Exception e) {
        }

        if (stopFlag) break;

        log.info("MasterListUpdater touching masters...");
        List createdGamesList = statsCollector.getStartedGamesList();

        if (emulinkerMasterTask != null) emulinkerMasterTask.touchMaster();

        if (kailleraMasterTask != null) kailleraMasterTask.touchMaster();

        statsCollector.clearStartedGamesList();
      }
    } finally {
      isRunning = false;
      log.debug("MasterListUpdater thread exiting...");
    }
  }
}

package org.emulinker.kaillera.master.client;

import com.google.common.flogger.FluentLogger;
import java.util.concurrent.ThreadPoolExecutor;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.*;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.release.*;
import org.emulinker.util.Executable;

@Singleton
public class MasterListUpdaterImpl implements MasterListUpdater, Executable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private ThreadPoolExecutor threadPool;
  private StatsCollector statsCollector;

  private PublicServerInformation publicInfo;

  private boolean touchKaillera = false;
  private boolean touchEmulinker = false;

  private EmuLinkerMasterUpdateTask emulinkerMasterTask;
  private KailleraMasterUpdateTask kailleraMasterTask;

  private boolean stopFlag = false;
  private boolean isRunning = false;

  @Inject
  MasterListUpdaterImpl(
      Configuration config,
      ThreadPoolExecutor threadPool,
      ConnectController connectController,
      KailleraServer kailleraServer,
      StatsCollector statsCollector,
      ReleaseInfo releaseInfo) {
    this.threadPool = threadPool;
    this.statsCollector = statsCollector;

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

  @Override
  public synchronized boolean isRunning() {
    return isRunning;
  }

  @Override
  public synchronized String toString() {
    return "MasterListUpdaterImpl[touchKaillera="
        + touchKaillera
        + " touchEmulinker="
        + touchEmulinker
        + "]";
  }

  public synchronized void start() {
    if (publicInfo != null) {
      logger.atFine().log("MasterListUpdater thread received start request!");
      logger.atFine().log(
          "MasterListUpdater thread starting (ThreadPool:"
              + threadPool.getActiveCount()
              + "/"
              + threadPool.getPoolSize()
              + ")");
      threadPool.execute(this);
      Thread.yield();
      logger.atFine().log(
          "MasterListUpdater thread started (ThreadPool:"
              + threadPool.getActiveCount()
              + "/"
              + threadPool.getPoolSize()
              + ")");
    }
  }

  @Override
  public synchronized void stop() {
    if (publicInfo != null) {
      logger.atFine().log("MasterListUpdater thread received stop request!");

      if (!isRunning()) {
        logger.atFine().log("MasterListUpdater thread stop request ignored: not running!");
        return;
      }

      stopFlag = true;
    }
  }

  @Override
  public void run() {
    isRunning = true;
    logger.atFine().log("MasterListUpdater thread running...");

    try {
      while (!stopFlag) {
        try {
          Thread.sleep(60000);
        } catch (Exception e) {
        }

        if (stopFlag) break;

        logger.atInfo().log("MasterListUpdater touching masters...");

        if (emulinkerMasterTask != null) emulinkerMasterTask.touchMaster();

        if (kailleraMasterTask != null) kailleraMasterTask.touchMaster();

        statsCollector.clearStartedGamesList();
      }
    } finally {
      isRunning = false;
      logger.atFine().log("MasterListUpdater thread exiting...");
    }
  }
}

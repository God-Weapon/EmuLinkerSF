package org.emulinker.kaillera.master.client;

import com.google.common.flogger.FluentLogger;
import java.util.concurrent.ThreadPoolExecutor;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.*;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.model.*;
import org.emulinker.kaillera.release.ReleaseInfo;
import org.emulinker.util.Executable;

@Singleton
public class MasterListUpdaterImpl implements MasterListUpdater, Executable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private ThreadPoolExecutor threadPool;
  private StatsCollector statsCollector;

  private PublicServerInformation publicInfo;

  private EmuLinkerMasterUpdateTask emulinkerMasterTask;
  private KailleraMasterUpdateTask kailleraMasterTask;

  private boolean stopFlag = false;
  private boolean isRunning = false;

  private final RuntimeFlags flags;

  @Inject
  MasterListUpdaterImpl(
      RuntimeFlags flags,
      ThreadPoolExecutor threadPool,
      ConnectController connectController,
      KailleraServer kailleraServer,
      StatsCollector statsCollector,
      ReleaseInfo releaseInfo) {
    this.flags = flags;
    this.threadPool = threadPool;
    this.statsCollector = statsCollector;

    if (flags.getTouchKaillera() || flags.getTouchEmulinker()) {
      publicInfo = new PublicServerInformation(flags);
    }

    if (flags.getTouchKaillera()) {
      kailleraMasterTask =
          new KailleraMasterUpdateTask(
              publicInfo, connectController, kailleraServer, statsCollector, releaseInfo);
    }

    if (flags.getTouchEmulinker()) {
      emulinkerMasterTask =
          new EmuLinkerMasterUpdateTask(publicInfo, connectController, kailleraServer, releaseInfo);
    }
  }

  @Override
  public synchronized boolean getRunning() {
    return isRunning;
  }

  @Override
  public synchronized String toString() {
    return "MasterListUpdaterImpl[touchKaillera="
        + flags.getTouchKaillera()
        + " touchEmulinker="
        + flags.getTouchEmulinker()
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

      if (!getRunning()) {
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

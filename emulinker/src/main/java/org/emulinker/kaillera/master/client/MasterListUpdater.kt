package org.emulinker.kaillera.master.client

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.master.PublicServerInformation
import org.emulinker.kaillera.master.StatsCollector
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.release.ReleaseInfo
import org.emulinker.util.Executable

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class MasterListUpdater
    @Inject
    internal constructor(
        private val flags: RuntimeFlags,
        connectController: ConnectController?,
        kailleraServer: KailleraServer?,
        private val statsCollector: StatsCollector,
        releaseInfo: ReleaseInfo?
    ) : Executable {
  private var publicInfo: PublicServerInformation? = null
  private var emulinkerMasterTask: EmuLinkerMasterUpdateTask? = null
  private var kailleraMasterTask: KailleraMasterUpdateTask? = null
  private var stopFlag = false

  @get:Synchronized
  override var threadIsActive = false
    private set

  @Synchronized
  override fun toString() =
      "MasterListUpdater[touchKaillera=${flags.touchKaillera} touchEmulinker=${flags.touchEmulinker}]"

  @Synchronized
  fun start() {
    if (publicInfo != null) {
      logger.atFine().log("MasterListUpdater thread received start request!")
      //      threadPool.execute(this) // NUEFIXME
      //      Thread.yield() // nue removed
    }
  }

  @Synchronized
  override suspend fun stop() {
    if (publicInfo != null) {
      logger.atFine().log("MasterListUpdater thread received stop request!")
      if (!threadIsActive) {
        logger.atFine().log("MasterListUpdater thread stop request ignored: not running!")
        return
      }
      stopFlag = true
    }
  }

  override suspend fun run(globalContext: CoroutineContext) {
    threadIsActive = true
    logger.atFine().log("MasterListUpdater thread running...")
    try {
      while (!stopFlag) {
        delay(60.seconds)
        if (stopFlag) break
        logger.atInfo().log("MasterListUpdater touching masters...")
        emulinkerMasterTask?.touchMaster()
        kailleraMasterTask?.touchMaster()
        statsCollector.clearStartedGamesList()
      }
    } finally {
      threadIsActive = false
      logger.atFine().log("MasterListUpdater thread exiting...")
    }
  }

  init {
    if (flags.touchKaillera || flags.touchEmulinker) {
      publicInfo = PublicServerInformation(flags)
    }
    if (flags.touchKaillera) {
      kailleraMasterTask =
          KailleraMasterUpdateTask(
              publicInfo!!, connectController!!, kailleraServer!!, statsCollector, releaseInfo!!)
    }
    if (flags.touchEmulinker) {
      emulinkerMasterTask =
          EmuLinkerMasterUpdateTask(
              publicInfo!!, connectController!!, kailleraServer!!, releaseInfo!!)
    }
  }
}

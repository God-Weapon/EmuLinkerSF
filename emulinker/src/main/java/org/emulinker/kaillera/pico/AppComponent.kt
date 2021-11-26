package org.emulinker.kaillera.pico

import com.codahale.metrics.MetricRegistry
import dagger.Component
import javax.inject.Singleton
import org.apache.commons.configuration.Configuration
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.access.AccessManager2
import org.emulinker.kaillera.controller.KailleraServerController
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.master.client.MasterListUpdaterImpl
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.release.ReleaseInfo

@Singleton
@Component(modules = [AppModule::class])
abstract class AppComponent {
  abstract val configuration: Configuration?
  abstract val releaseInfo: ReleaseInfo?
  abstract val server: ConnectController?
  abstract val kailleraServerController: KailleraServerController?
  abstract val accessManager: AccessManager2?
  abstract val kailleraServer: KailleraServer?
  abstract val masterListUpdaterImpl: MasterListUpdaterImpl?
  abstract val metricRegistry: MetricRegistry?
  abstract val runtimeFlags: RuntimeFlags?
}

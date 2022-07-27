package org.emulinker.kaillera.pico

import com.codahale.metrics.MetricRegistry
import dagger.Component
import javax.inject.Singleton
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.controller.KailleraServerController
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.master.client.MasterListUpdater
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.release.ReleaseInfo
import org.emulinker.net.UdpSocketProvider

@Singleton
@Component(modules = [AppModule::class])
abstract class AppComponent {
  abstract val releaseInfo: ReleaseInfo
  abstract val server: ConnectController
  abstract val kailleraServerController: KailleraServerController
  abstract val kailleraServer: KailleraServer
  abstract val masterListUpdater: MasterListUpdater
  abstract val metricRegistry: MetricRegistry
  abstract val runtimeFlags: RuntimeFlags
  abstract val udpSocketProvider: UdpSocketProvider
}

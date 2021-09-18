package org.emulinker.kaillera.pico;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.access.AccessManager2;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.v086.V086Controller;
import org.emulinker.kaillera.master.MasterListStatsCollector;
import org.emulinker.kaillera.master.StatsCollector;
import org.emulinker.kaillera.master.client.MasterListUpdater;
import org.emulinker.kaillera.master.client.MasterListUpdaterImpl;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactory;
import org.emulinker.kaillera.model.impl.AutoFireDetectorFactoryImpl;
import org.emulinker.kaillera.model.impl.KailleraServerImpl;
import org.emulinker.kaillera.release.KailleraServerReleaseInfo;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuLinkerExecutor;
import org.emulinker.util.EmuLinkerPropertiesConfig;

@Module
public abstract class AppModule {
  // TODO(nue): Burn this with fire!!!
  // NOTE: This is NOT marked final and there are race conditions involved. Inject @RuntimeFlags
  // instead!
  public static Charset charsetDoNotUse = null;

  @Provides
  public static Configuration provideConfiguration() {
    try {
      return new EmuLinkerPropertiesConfig();
    } catch (ConfigurationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Provides
  public static RuntimeFlags provideRuntimeFlags(Configuration configuration) {
    RuntimeFlags flags = RuntimeFlags.loadFromApacheConfiguration(configuration);
    AppModule.charsetDoNotUse = flags.charset();
    return flags;
  }

  @Binds
  public abstract ThreadPoolExecutor bindThreadPoolExecutor(EmuLinkerExecutor emuLinkerExecutor);

  @Binds
  public abstract ReleaseInfo bindKailleraServerReleaseInfo(
      KailleraServerReleaseInfo kailleraServerReleaseInfo);

  @Binds
  public abstract AccessManager bindAccessManager(AccessManager2 accessManager2);

  @Binds
  public abstract AutoFireDetectorFactory bindAutoFireDetectorFactory(
      AutoFireDetectorFactoryImpl autoFireDetectorFactoryImpl);

  @Binds
  public abstract KailleraServer bindKailleraServer(KailleraServerImpl kailleraServerImpl);

  @Binds
  public abstract KailleraServerController bindKailleraServerController(
      V086Controller v086Controller);

  @Binds
  @IntoSet
  public abstract KailleraServerController bindKailleraServerControllerToSet(
      V086Controller v086Controller);

  @Binds
  public abstract StatsCollector bindStatsCollector(
      MasterListStatsCollector masterListStatsCollector);

  @Binds
  public abstract MasterListUpdater bindMasterListUpdaterImpl(
      MasterListUpdaterImpl masterListUpdaterImpl);
}

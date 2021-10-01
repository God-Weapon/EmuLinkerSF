package org.emulinker.kaillera.pico;

import com.codahale.metrics.MetricRegistry;
import dagger.Component;
import javax.inject.Singleton;
import org.apache.commons.configuration.Configuration;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.access.AccessManager2;
import org.emulinker.kaillera.controller.KailleraServerController;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.client.MasterListUpdaterImpl;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.release.ReleaseInfo;

@Singleton
@Component(modules = AppModule.class)
public abstract class AppComponent {
  public abstract Configuration getConfiguration();

  public abstract ReleaseInfo getReleaseInfo();

  public abstract ConnectController getServer();

  public abstract KailleraServerController getKailleraServerController();

  public abstract AccessManager2 getAccessManager();

  public abstract KailleraServer getKailleraServer();

  public abstract MasterListUpdaterImpl getMasterListUpdaterImpl();

  public abstract MetricRegistry getMetricRegistry();

  public abstract RuntimeFlags getRuntimeFlags();
}

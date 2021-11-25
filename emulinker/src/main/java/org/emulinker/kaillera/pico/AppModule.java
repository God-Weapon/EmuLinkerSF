package org.emulinker.kaillera.pico;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.codahale.metrics.MetricRegistry;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.nio.charset.Charset;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import javax.inject.Singleton;
import org.apache.commons.configuration.Configuration;
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
import org.emulinker.util.EmuLinkerPropertiesConfig;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Module
public abstract class AppModule {
  // TODO(nue): Burn this with fire!!!
  // NOTE: This is NOT marked final and there are race conditions involved. Inject @RuntimeFlags
  // instead!
  public static Charset charsetDoNotUse = null;

  @Provides
  public static Twitter provideTwitter(TwitterFactory twitterFactory) {
    return twitterFactory.getInstance();
  }

  @Provides
  @Singleton
  public static TwitterFactory provideTwitterFactory(RuntimeFlags flags) {
    return new TwitterFactory(
        new ConfigurationBuilder()
            .setDebugEnabled(true)
            .setOAuthAccessToken(flags.getTwitterOAuthAccessToken())
            .setOAuthAccessTokenSecret(flags.getTwitterOAuthAccessTokenSecret())
            .setOAuthConsumerKey(flags.getTwitterOAuthConsumerKey())
            .setOAuthConsumerSecret(flags.getTwitterOAuthConsumerSecret())
            .build());
  }

  @Provides
  @Singleton
  public static Configuration provideConfiguration() {
    return new EmuLinkerPropertiesConfig();
  }

  @Provides
  @Singleton
  public static RuntimeFlags provideRuntimeFlags(Configuration configuration) {
    RuntimeFlags flags = RuntimeFlags.Companion.loadFromApacheConfiguration(configuration);
    AppModule.charsetDoNotUse = flags.getCharset();
    return flags;
  }

  @Provides
  public static ThreadPoolExecutor provideThreadPoolExecutor(RuntimeFlags flags) {
    return new ThreadPoolExecutor(
        flags.getCoreThreadPoolSize(),
        Integer.MAX_VALUE,
        60L,
        SECONDS,
        new SynchronousQueue<Runnable>());
  }

  @Provides
  @Singleton
  public static MetricRegistry provideMetricRegistry() {
    return new MetricRegistry();
  }

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

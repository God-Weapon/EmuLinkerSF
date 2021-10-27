package org.emulinker.kaillera.pico;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.flogger.FluentLogger;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.emulinker.config.RuntimeFlags;

public class PicoStarter {
  public static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /**
   * Main entry point for the EmuLinker Kaillera server. This method accepts no arguments. It starts
   * the pico container which reads its configuration from components.xml. The server components,
   * once started, read their configuration information from emulinker.xml. Each of those files will
   * be located by using the classpath.
   */
  public static void main(String args[]) {
    System.setProperty(
        "flogger.backend_factory",
        "com.google.common.flogger.backend.log4j2.Log4j2BackendFactory#getInstance");

    AppComponent component = DaggerAppComponent.create();

    logger.atInfo().log("EmuLinker server Starting...");
    logger.atInfo().log(component.getReleaseInfo().getWelcome());
    logger.atInfo().log(
        "EmuLinker server is running @ "
            + DateTimeFormatter.ISO_ZONED_DATE_TIME
                .withZone(ZoneId.systemDefault())
                .format(Instant.now()));

    component.getAccessManager().start();
    component.getKailleraServerController().start();
    component.getServer().start();
    component.getKailleraServer().start();
    component.getMasterListUpdaterImpl().start();

    MetricRegistry metrics = component.getMetricRegistry();
    metrics.registerAll(new ThreadStatesGaugeSet());
    metrics.registerAll(new MemoryUsageGaugeSet());

    RuntimeFlags flags = component.getRuntimeFlags();
    if (flags.metricsEnabled()) {
      // TODO(nue): Pass this data to a central server so we can see how performance changes over
      // time in prod.
      // "graphite" is the name of a service in docker-compose.yaml.
      final Graphite graphite = new Graphite(new InetSocketAddress("graphite", 2003));
      final GraphiteReporter reporter =
          GraphiteReporter.forRegistry(metrics)
              .convertRatesTo(SECONDS)
              .convertDurationsTo(MILLISECONDS)
              .filter(MetricFilter.ALL)
              .build(graphite);
      reporter.start(30, SECONDS);
    }
  }
}

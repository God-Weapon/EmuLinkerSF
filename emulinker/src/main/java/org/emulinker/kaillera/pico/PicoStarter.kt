package org.emulinker.kaillera.pico

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import com.codahale.metrics.jvm.MemoryUsageGaugeSet
import com.codahale.metrics.jvm.ThreadStatesGaugeSet
import com.google.common.flogger.FluentLogger
import java.net.InetSocketAddress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

private val logger = FluentLogger.forEnclosingClass()

object PicoStarter {

  /**
   * Main entry point for the EmuLinker Kaillera server. This method accepts no arguments. It starts
   * the pico container which reads its configuration from components.xml. The server components,
   * once started, read their configuration information from emulinker.xml. Each of those files will
   * be located by using the classpath.
   */
  @JvmStatic
  fun main(args: Array<String>) {
    System.setProperty(
        "flogger.backend_factory",
        "com.google.common.flogger.backend.log4j2.Log4j2BackendFactory#getInstance")
    val component = DaggerAppComponent.create()
    logger.atInfo().log("EmuLinker server Starting...")
    logger.atInfo().log(component.releaseInfo.welcome)
    logger
        .atInfo()
        .log(
            "EmuLinker server is running @ ${DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(ZoneId.systemDefault()).format(Instant.now())}")
    component.accessManager.start()
    component.kailleraServerController.start()
    component.server.start()
    component.kailleraServer.start()
    component.masterListUpdaterImpl.start()
    val metrics = component.metricRegistry
    metrics.registerAll(ThreadStatesGaugeSet())
    metrics.registerAll(MemoryUsageGaugeSet())
    val flags = component.runtimeFlags
    if (flags.metricsEnabled) {
      // TODO(nue): Pass this data to a central server so we can see how performance changes over
      // time in prod.
      // "graphite" is the name of a service in docker-compose.yaml.
      val graphite = Graphite(InetSocketAddress("graphite", 2003))
      val reporter =
          GraphiteReporter.forRegistry(metrics)
              .convertRatesTo(SECONDS)
              .convertDurationsTo(MILLISECONDS)
              .filter(MetricFilter.ALL)
              .build(graphite)
      reporter.start(30, SECONDS)
    }
  }
}

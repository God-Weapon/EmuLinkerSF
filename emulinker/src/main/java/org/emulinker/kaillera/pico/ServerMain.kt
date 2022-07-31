package org.emulinker.kaillera.pico

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.Graphite
import com.codahale.metrics.graphite.GraphiteReporter
import com.codahale.metrics.jvm.MemoryUsageGaugeSet
import com.codahale.metrics.jvm.ThreadStatesGaugeSet
import com.google.common.flogger.FluentLogger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val logger = FluentLogger.forEnclosingClass()

/** Main entry point for the Kaillera server. */
fun main(): Unit =
    runBlocking {
      // Change number of Dispatchers.IO coroutines.
      System.setProperty(IO_PARALLELISM_PROPERTY_NAME, 1000.toString())
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

      component.kailleraServerController.start() // Apparently cannot be removed.
      launch(Dispatchers.IO) {
        component.server.start(
            component.udpSocketProvider, coroutineContext + CoroutineName("ConnectServer"))
      }

      component.masterListUpdater.start()
      val metrics = component.metricRegistry
      metrics.registerAll(ThreadStatesGaugeSet())
      metrics.registerAll(MemoryUsageGaugeSet())
      val flags = component.runtimeFlags
      if (flags.metricsEnabled) {
        // TODO(nue): Pass this data to a central server so we can see how performance changes over
        // time in prod.
        // "graphite" is the name of a service in docker-compose.yaml.
        val graphite = Graphite(java.net.InetSocketAddress("graphite", 2003))
        val reporter =
            GraphiteReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite)
        reporter.start(30, TimeUnit.SECONDS)
      }
    }

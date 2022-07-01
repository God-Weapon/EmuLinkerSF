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

/** Main entry point for the Kaillera server. */
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
  component.masterListUpdater.start()
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

  //  // Hacky code but it works! Tests that two users can make and play a game.
  //  // TODO(nue): Move this into a test file in a subsequent PR.
  //  runBlocking {
  //    delay(4.seconds)
  //
  //    arrayOf(
  //        async {
  //          EvalClient("testuser1", io.ktor.network.sockets.InetSocketAddress("127.0.0.1", 27888))
  //              .use {
  //                delay(5.seconds)
  //
  //                it.connectToDedicatedPort()
  //                it.start()
  //
  //                delay(1.seconds)
  //
  //                it.createGame()
  //
  //                delay(5.seconds)
  //
  //                it.startOwnGame()
  //
  //                delay(30.seconds)
  //                it.dropGame()
  //                delay(1.seconds)
  //                it.quitGame()
  //                delay(1.seconds)
  //                it.quitServer()
  //
  //                delay(15.seconds)
  //              }
  //        },
  //        async {
  //          EvalClient("testuser2", io.ktor.network.sockets.InetSocketAddress("127.0.0.1", 27888))
  //              .use {
  //                delay(9.seconds)
  //
  //                it.connectToDedicatedPort()
  //                it.start()
  //
  //                delay(1.seconds)
  //                it.joinAnyAvailableGame()
  //
  //                delay(40.seconds)
  //                it.quitServer()
  //              }
  //        })
  //        .forEach { it.join() }
  //
  //    logger.atInfo().log("Shutting down everything else")
  //
  //    component.accessManager.stop()
  //    component.kailleraServerController.stop()
  //    component.kailleraServer.stop()
  //    component.masterListUpdater.stop()
  //  }
}

package org.emulinker.kaillera.release

import com.google.common.flogger.FluentLogger
import java.io.IOException
import java.time.Instant
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.util.EmuUtil

private val logger: FluentLogger = FluentLogger.forEnclosingClass()

private val properties = Properties()

/**
 * Provides release and build information for the EmuLinker project. This class also formats a
 * welcome message for printing at server startup.
 */
@Singleton
class ReleaseInfo @Inject constructor() {
  val productName: String by lazy { properties.getProperty("project.name") }

  val versionString: String by lazy { properties.getProperty("project.version") }

  val buildDate: Instant by lazy { Instant.parse(properties.getProperty("project.buildDate")) }

  val websiteString: String by lazy { properties.getProperty("project.url") }

  val licenseInfo = "Usage of this sofware is subject to the terms found in the included license"

  /**
   * Formats release information into a welcome message. This message is printed by the server at
   * server startup.
   */
  val welcome by lazy {
    """// $productName version $versionString (${EmuUtil.toSimpleUtcDatetime(buildDate)}) 
// $licenseInfo
// For the most up-to-date information please visit: $websiteString"""
  }

  init {
    try {
      properties.load(this.javaClass.classLoader.getResourceAsStream("kailleraserver.properties"))
    } catch (e: IOException) {
      logger.atSevere().withCause(e).log("Failed to read kailleraserver.properties file")
      throw IllegalStateException(e)
    }
  }
}

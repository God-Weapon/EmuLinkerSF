package org.emulinker.kaillera.release;

import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.time.Instant;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuUtil;

/**
 * Provides release and build information for the EmuLinker project. This class also formats a
 * welcome message for printing at server startup.
 */
@Singleton
public final class KailleraServerReleaseInfo implements ReleaseInfo {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String licenseInfo =
      "Usage of this sofware is subject to the terms found in the included license";

  private final String productName;
  private final String version;
  private final Instant buildTimestamp;
  private final String website;

  @Inject
  KailleraServerReleaseInfo() {
    Properties properties = new Properties();
    try {
      properties.load(
          this.getClass().getClassLoader().getResourceAsStream("kailleraserver.properties"));
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to read kailleraserver.properties file");
      throw new IllegalStateException(e);
    }
    this.productName = properties.getProperty("project.name");
    this.website = properties.getProperty("project.url");
    this.version = properties.getProperty("project.version");
    this.buildTimestamp = Instant.parse(properties.getProperty("project.buildDate"));
  }

  @Override
  public final String getProductName() {
    return productName;
  }

  @Override
  public final Instant getBuildDate() {
    return buildTimestamp;
  }

  @Override
  public final String getLicenseInfo() {
    return licenseInfo;
  }

  @Override
  public final String getWebsiteString() {
    return website;
  }

  /**
   * Returns the version number for the EmuLinker server in the form
   *
   * <p><i>major</i>.<i>minor</i>
   */
  @Override
  public final String getVersionString() {
    return version;
  }

  /**
   * Formats release information into a welcome message. This message is printed by the server at
   * server startup.
   */
  @Override
  public String getWelcome() {
    return String.format(
        "// %s version %s (%s) \n// %s\n// For the most up-to-date information please visit: %s",
        getProductName(),
        getVersionString(),
        EmuUtil.toSimpleUtcDatetime(getBuildDate()),
        getLicenseInfo(),
        getWebsiteString());
  }
}

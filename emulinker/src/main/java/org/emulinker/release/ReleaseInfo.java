package org.emulinker.release;

import java.time.Instant;

/**
 * Provides release information about the project.
 *
 * @author Paul Cowan
 * @see www.emulinker.org
 */
public interface ReleaseInfo {
  /** @return The name of this software. */
  public String getProductName();

  /** @return The release date of this software */
  public Instant getBuildDate();

  /** @return A string containing the full version information */
  public String getVersionString();

  /** @return License information */
  public String getLicenseInfo();

  /** @return A string containing software website iformation */
  public String getWebsiteString();

  /** @return A string containg a welcome message intended to be display on software startup */
  public String getWelcome();
}

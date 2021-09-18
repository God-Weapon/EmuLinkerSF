package org.emulinker.kaillera.master;

import org.emulinker.config.RuntimeFlags;

public class PublicServerInformation {
  private final RuntimeFlags flags;

  public PublicServerInformation(RuntimeFlags flags) {
    this.flags = flags;
  }

  public String getServerName() {
    return flags.serverName();
  }

  public String getLocation() {
    return flags.serverLocation();
  }

  public String getWebsite() {
    return flags.serverWebsite();
  }

  public String getConnectAddress() {
    return flags.serverAddress();
  }
}

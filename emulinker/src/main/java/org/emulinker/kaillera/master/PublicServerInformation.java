package org.emulinker.kaillera.master;

import org.apache.commons.configuration.Configuration;

public class PublicServerInformation {
  private String serverName;
  private String serverLocation;
  private String serverWebsite;
  private String serverAddress;

  public PublicServerInformation(Configuration config) {
    serverName = config.getString("masterList.serverName", "Emulinker Server");
    serverLocation = config.getString("masterList.serverLocation", "Unknown");
    serverWebsite = config.getString("masterList.serverWebsite", "");
    serverAddress = config.getString("masterList.serverConnectAddress", "");
  }

  public String getServerName() {
    return serverName;
  }

  public String getLocation() {
    return serverLocation;
  }

  public String getWebsite() {
    return serverWebsite;
  }

  public String getConnectAddress() {
    return serverAddress;
  }
}

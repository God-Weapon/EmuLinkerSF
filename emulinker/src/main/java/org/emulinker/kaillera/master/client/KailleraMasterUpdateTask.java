package org.emulinker.kaillera.master.client;

import java.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.*;
import org.emulinker.kaillera.model.*;
import org.emulinker.release.ReleaseInfo;

public class KailleraMasterUpdateTask implements MasterListUpdateTask {
  private static Log log = LogFactory.getLog(KailleraMasterUpdateTask.class);

  private PublicServerInformation publicInfo;
  private ConnectController connectController;
  private KailleraServer kailleraServer;
  private StatsCollector statsCollector;
  private HttpClient httpClient;
  private ReleaseInfo releaseInfo;

  public KailleraMasterUpdateTask(
      PublicServerInformation publicInfo,
      ConnectController connectController,
      KailleraServer kailleraServer,
      StatsCollector statsCollector,
      ReleaseInfo releaseInfo) {
    this.publicInfo = publicInfo;
    this.connectController = connectController;
    this.kailleraServer = kailleraServer;
    this.releaseInfo = releaseInfo;
    this.statsCollector = statsCollector;
    this.publicInfo = publicInfo;

    httpClient = new HttpClient();
    httpClient.setConnectionTimeout(5000);
    httpClient.setTimeout(5000);
  }

  @Override
  public void touchMaster() {
    List createdGamesList = statsCollector.getStartedGamesList();

    StringBuilder createdGames = new StringBuilder();
    synchronized (createdGamesList) {
      Iterator iter = createdGamesList.iterator();
      while (iter.hasNext()) {
        createdGames.append(iter.next());
        createdGames.append("|");
      }

      createdGamesList.clear();
    }

    StringBuilder waitingGames = new StringBuilder();
    for (KailleraGame game : kailleraServer.getGames()) {
      if (game.getStatus() != KailleraGame.STATUS_WAITING) continue;

      waitingGames.append(game.getID());
      waitingGames.append("|");
      waitingGames.append(game.getRomName());
      waitingGames.append("|");
      waitingGames.append(game.getOwner().getName());
      waitingGames.append("|");
      waitingGames.append(game.getOwner().getClientType());
      waitingGames.append("|");
      waitingGames.append(game.getNumPlayers());
      waitingGames.append("|");
    }

    NameValuePair[] params = new NameValuePair[9];
    params[0] = new NameValuePair("servername", publicInfo.getServerName());
    params[1] = new NameValuePair("port", Integer.toString(connectController.getBindPort()));
    params[2] = new NameValuePair("nbusers", Integer.toString(kailleraServer.getNumUsers()));
    params[3] = new NameValuePair("maxconn", Integer.toString(kailleraServer.getMaxUsers()));

    params[4] = new NameValuePair("version", "ESF" + releaseInfo.getVersionString());
    params[5] = new NameValuePair("nbgames", Integer.toString(kailleraServer.getNumGames()));
    params[6] = new NameValuePair("location", publicInfo.getLocation());
    params[7] = new NameValuePair("ip", publicInfo.getConnectAddress());
    params[8] = new NameValuePair("url", publicInfo.getWebsite());

    HttpMethod kailleraTouch = new GetMethod("http://www.kaillera.com/touch_server.php");
    kailleraTouch.setQueryString(params);
    kailleraTouch.setRequestHeader("Kaillera-games", createdGames.toString());
    kailleraTouch.setRequestHeader("Kaillera-wgames", waitingGames.toString());

    try {
      int statusCode = httpClient.executeMethod(kailleraTouch);
      if (statusCode != HttpStatus.SC_OK)
        log.error("Failed to touch Kaillera Master: " + kailleraTouch.getStatusLine());
      else log.info("Touching Kaillera Master done");
    } catch (Exception e) {
      log.error("Failed to touch Kaillera Master: " + e.getMessage());
    } finally {
      if (kailleraTouch != null) {
        try {
          kailleraTouch.releaseConnection();
        } catch (Exception e) {
        }
      }
    }
  }
}

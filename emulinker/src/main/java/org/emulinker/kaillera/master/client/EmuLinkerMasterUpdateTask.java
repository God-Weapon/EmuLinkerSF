package org.emulinker.kaillera.master.client;

import com.google.common.flogger.FluentLogger;
import java.util.Properties;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.PublicServerInformation;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.release.ReleaseInfo;

public class EmuLinkerMasterUpdateTask implements MasterListUpdateTask {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String url = "http://master.emulinker.org/touch_list.php";

  private PublicServerInformation publicInfo;
  private ConnectController connectController;
  private KailleraServer kailleraServer;
  private ReleaseInfo releaseInfo;
  private HttpClient httpClient;

  public EmuLinkerMasterUpdateTask(
      PublicServerInformation publicInfo,
      ConnectController connectController,
      KailleraServer kailleraServer,
      ReleaseInfo releaseInfo) {
    this.publicInfo = publicInfo;
    this.connectController = connectController;
    this.kailleraServer = kailleraServer;
    this.publicInfo = publicInfo;
    this.releaseInfo = releaseInfo;

    httpClient = new HttpClient();
    httpClient.setConnectionTimeout(5000);
    httpClient.setTimeout(5000);
  }

  @Override
  public void touchMaster() {
    StringBuilder waitingGames = new StringBuilder();
    for (KailleraGame game : kailleraServer.getGames()) {
      if (game.getStatus() != KailleraGame.STATUS_WAITING) continue;

      waitingGames.append(game.getRomName());
      waitingGames.append("|");
      waitingGames.append(game.getOwner().getName());
      waitingGames.append("|");
      waitingGames.append(game.getOwner().getClientType());
      waitingGames.append("|");
      waitingGames.append(game.getNumPlayers());
      waitingGames.append("/");
      waitingGames.append(game.getMaxUsers());
      waitingGames.append("|");
    }

    NameValuePair[] params = new NameValuePair[10];
    params[0] = new NameValuePair("serverName", publicInfo.getServerName());
    params[1] = new NameValuePair("ipAddress", publicInfo.getConnectAddress());
    params[2] = new NameValuePair("location", publicInfo.getLocation());
    params[3] = new NameValuePair("website", publicInfo.getWebsite());
    params[4] = new NameValuePair("port", Integer.toString(connectController.getBindPort()));
    params[5] = new NameValuePair("numUsers", Integer.toString(kailleraServer.getNumUsers()));
    params[6] = new NameValuePair("maxUsers", Integer.toString(kailleraServer.getMaxUsers()));
    params[7] = new NameValuePair("numGames", Integer.toString(kailleraServer.getNumGames()));
    params[8] = new NameValuePair("maxGames", Integer.toString(kailleraServer.getMaxGames()));
    params[9] = new NameValuePair("version", "ESF" + releaseInfo.getVersionString());

    HttpMethod meth = new GetMethod(url);
    meth.setQueryString(params);
    meth.setRequestHeader("Waiting-games", waitingGames.toString());
    meth.setFollowRedirects(true);

    Properties props = new Properties();

    try {
      int statusCode = httpClient.executeMethod(meth);
      if (statusCode != HttpStatus.SC_OK)
        logger.atSevere().log("Failed to touch EmuLinker Master: " + meth.getStatusLine());
      else {
        props.load(meth.getResponseBodyAsStream());
        logger.atInfo().log("Touching EmuLinker Master done");
      }
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Failed to touch EmuLinker Master");
    } finally {
      if (meth != null) {
        try {
          meth.releaseConnection();
        } catch (Exception e) {
        }
      }
    }

    // TODO(nue): Consider adding a server that can give update available notices for the Netosuma
    // build.
    // String updateAvailable = props.getProperty("updateAvailable");
    // if (updateAvailable != null && updateAvailable.equalsIgnoreCase("true")) {
    //   String latestVersion = props.getProperty("latest");
    //   String notes = props.getProperty("notes");
    //   StringBuilder sb = new StringBuilder();
    //   sb.append("A updated version of EmuLinkerSF is available: ");
    //   sb.append(latestVersion);
    //   if (notes != null) {
    //     sb.append(" (");
    //     sb.append(notes);
    //     sb.append(")");
    //   }
    //   logger.atWarning().log(sb.toString());
    // }
  }
}

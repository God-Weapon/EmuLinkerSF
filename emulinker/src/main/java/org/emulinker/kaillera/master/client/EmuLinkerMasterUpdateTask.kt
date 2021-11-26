package org.emulinker.kaillera.master.client

import com.google.common.flogger.FluentLogger
import java.util.*
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.master.PublicServerInformation
import org.emulinker.kaillera.model.KailleraGame
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.release.ReleaseInfo

private val logger = FluentLogger.forEnclosingClass()

private const val url = "http://master.emulinker.org/touch_list.php"

class EmuLinkerMasterUpdateTask(
    private val publicInfo: PublicServerInformation,
    private val connectController: ConnectController,
    private val kailleraServer: KailleraServer,
    private val releaseInfo: ReleaseInfo
) : MasterListUpdateTask {

  private val httpClient: HttpClient = HttpClient()

  init {
    httpClient.setConnectionTimeout(5000)
    httpClient.setTimeout(5000)
  }

  override fun touchMaster() {
    val waitingGames = StringBuilder()
    for (game in kailleraServer.games) {
      if (game.status != KailleraGame.STATUS_WAITING.toInt()) {
        continue
      }
      waitingGames.append(
          "${game.romName}|${game.owner.name}|${game.owner.clientType}|${game.numPlayers}/${game.maxUsers}|")
    }
    val params =
        arrayOf(
            NameValuePair("serverName", publicInfo.serverName),
            NameValuePair("ipAddress", publicInfo.connectAddress),
            NameValuePair("location", publicInfo.location),
            NameValuePair("website", publicInfo.website),
            NameValuePair("port", connectController.bindPort.toString()),
            NameValuePair("numUsers", kailleraServer.numUsers.toString()),
            NameValuePair("maxUsers", kailleraServer.maxUsers.toString()),
            NameValuePair("numGames", kailleraServer.numGames.toString()),
            NameValuePair("maxGames", kailleraServer.maxGames.toString()),
            NameValuePair("version", "ESF" + releaseInfo.versionString),
        )

    val meth: HttpMethod = GetMethod(url)
    meth.setQueryString(params)
    meth.setRequestHeader("Waiting-games", waitingGames.toString())
    meth.followRedirects = true
    val props = Properties()
    try {
      val statusCode = httpClient.executeMethod(meth)
      if (statusCode != HttpStatus.SC_OK)
          logger.atSevere().log("Failed to touch EmuLinker Master: " + meth.statusLine)
      else {
        props.load(meth.responseBodyAsStream)
        logger.atInfo().log("Touching EmuLinker Master done")
      }
    } catch (e: Exception) {
      logger.atSevere().withCause(e).log("Failed to touch EmuLinker Master")
    } finally {
      if (meth != null) {
        try {
          meth.releaseConnection()
        } catch (e: Exception) {}
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

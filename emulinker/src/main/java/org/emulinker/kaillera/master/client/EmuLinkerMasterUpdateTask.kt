package org.emulinker.kaillera.master.client

import com.google.common.flogger.FluentLogger
import java.util.*
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.master.PublicServerInformation
import org.emulinker.kaillera.model.GameStatus
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
    kailleraServer.games.asSequence().filter { it.status == GameStatus.WAITING }.forEach {
      waitingGames.append(
          "${it.romName}|${it.owner.name}|${it.owner.clientType}|${it.players.size}/${it.maxUsers}|")
    }
    val params =
        arrayOf(
            "serverName" to publicInfo.serverName,
            "ipAddress" to publicInfo.connectAddress,
            "location" to publicInfo.location,
            "website" to publicInfo.website,
            "port" to connectController.bindPort.toString(),
            "numUsers" to kailleraServer.users.size.toString(),
            "maxUsers" to kailleraServer.maxUsers.toString(),
            "numGames" to kailleraServer.games.size.toString(),
            "maxGames" to kailleraServer.maxGames.toString(),
            "version" to releaseInfo.shortVersionString,
        )

    val meth = GetMethod(url)
    meth.setQueryString(params.map { NameValuePair(it.first, it.second) }.toTypedArray())
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
      try {
        meth.releaseConnection()
      } catch (e: Exception) {}
    }

    // TODO(nue): Consider adding a server that can give update available notices for the netsma
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

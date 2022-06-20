package org.emulinker.kaillera.master.client

import com.google.common.flogger.FluentLogger
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.GetMethod
import org.emulinker.kaillera.controller.connectcontroller.ConnectController
import org.emulinker.kaillera.master.PublicServerInformation
import org.emulinker.kaillera.master.StatsCollector
import org.emulinker.kaillera.model.GameStatus
import org.emulinker.kaillera.model.KailleraServer
import org.emulinker.kaillera.release.ReleaseInfo

private val logger = FluentLogger.forEnclosingClass()

class KailleraMasterUpdateTask(
    private val publicInfo: PublicServerInformation,
    private val connectController: ConnectController,
    private val kailleraServer: KailleraServer,
    private val statsCollector: StatsCollector,
    private val releaseInfo: ReleaseInfo
) : MasterListUpdateTask {

  private val httpClient: HttpClient = HttpClient()

  override fun touchMaster() {
    val createdGamesList = statsCollector.getStartedGamesList()

    val createdGames = StringBuilder()
    synchronized(createdGamesList) {
      val iter = createdGamesList.iterator()
      while (iter.hasNext()) {
        createdGames.append(iter.next())
        createdGames.append("|")
      }
      createdGamesList.clear()
    }
    val waitingGames = StringBuilder()
    for (game in kailleraServer.games) {
      if (game.status != GameStatus.WAITING) continue
      waitingGames.append(
          "${game.id}|${game.romName}|${game.owner.name}|${game.owner.clientType}|${game.players.size}|")
    }
    val params =
        arrayOf(
            NameValuePair("servername", publicInfo.serverName),
            NameValuePair("port", connectController.bindPort.toString()),
            NameValuePair("nbusers", kailleraServer.users.size.toString()),
            NameValuePair("maxconn", kailleraServer.maxUsers.toString()),
            NameValuePair("version", "ESF" + releaseInfo.versionString),
            NameValuePair("nbgames", kailleraServer.games.size.toString()),
            NameValuePair("location", publicInfo.location),
            NameValuePair("ip", publicInfo.connectAddress),
            NameValuePair("url", publicInfo.website),
        )
    val kailleraTouch: HttpMethod = GetMethod("http://www.kaillera.com/touch_server.php")
    kailleraTouch.setQueryString(params)
    kailleraTouch.setRequestHeader("Kaillera-games", createdGames.toString())
    kailleraTouch.setRequestHeader("Kaillera-wgames", waitingGames.toString())
    try {
      val statusCode = httpClient.executeMethod(kailleraTouch)
      if (statusCode != HttpStatus.SC_OK)
          logger.atSevere().log("Failed to touch Kaillera Master: " + kailleraTouch.statusLine)
      else logger.atInfo().log("Touching Kaillera Master done")
    } catch (e: Exception) {
      logger.atSevere().withCause(e).log("Failed to touch Kaillera Master")
    } finally {
      try {
        kailleraTouch.releaseConnection()
      } catch (e: Exception) {
        logger.atSevere().withCause(e).log("Failed to release connection")
      }
    }
  }

  init {
    httpClient.setConnectionTimeout(5000)
    httpClient.setTimeout(5000)
  }
}

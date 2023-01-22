package org.emulinker.kaillera.master.client;

import java.util.Properties;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.logging.*;
import org.emulinker.kaillera.controller.connectcontroller.ConnectController;
import org.emulinker.kaillera.master.PublicServerInformation;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.release.ReleaseInfo;
import org.emulinker.util.EmuUtil;

public class EmuLinkerMasterUpdateTask implements MasterListUpdateTask
{
	private static Log				log	= LogFactory.getLog(EmuLinkerMasterUpdateTask.class);
	private static final String		url	= "http://kaillerareborn.2manygames.fr/touch_list.php";

	private PublicServerInformation	publicInfo;
	private ConnectController		connectController;
	private KailleraServer			kailleraServer;
	private ReleaseInfo				releaseInfo;
	private HttpClient				httpClient;

	public EmuLinkerMasterUpdateTask(PublicServerInformation publicInfo, ConnectController connectController, KailleraServer kailleraServer, ReleaseInfo releaseInfo)
	{
		this.publicInfo = publicInfo;
		this.connectController = connectController;
		this.kailleraServer = kailleraServer;
		this.publicInfo = publicInfo;
		this.releaseInfo = releaseInfo;

		httpClient = new HttpClient();
		httpClient.setConnectionTimeout(5000);
		httpClient.setTimeout(5000);
	}

	public void touchMaster()
	{
		StringBuilder waitingGames = new StringBuilder();
		for(KailleraGame game : kailleraServer.getGames())
		{
			if (game.getStatus() != KailleraGame.STATUS_WAITING)
				continue;

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

		try
		{
			int statusCode = httpClient.executeMethod(meth);
			if (statusCode != HttpStatus.SC_OK)
				log.error("Failed to touch EmuLinker Master: " + meth.getStatusLine());
			else
			{
				props.load(meth.getResponseBodyAsStream());
				log.info("Touching EmuLinker Master done");
			}
		}
		catch (Exception e)
		{
			log.error("Failed to touch EmuLinker Master: " + e.getMessage());
		}
		finally
		{
			if (meth != null)
			{
				try
				{
					meth.releaseConnection();
				}
				catch (Exception e)
				{
				}
			}
		}

		String updateAvailable = props.getProperty("updateAvailable");
		if (updateAvailable != null && updateAvailable.equalsIgnoreCase("true"))
		{
			String latestVersion = props.getProperty("latest");
			String notes = props.getProperty("notes");
			StringBuilder sb = new StringBuilder();
			sb.append("A updated version of EmuLinkerSF is available: ");
			sb.append(latestVersion);
			if (notes != null)
			{
				sb.append(" (");
				sb.append(notes);
				sb.append(")");
			}
			log.warn(sb.toString());
		}
	}
}

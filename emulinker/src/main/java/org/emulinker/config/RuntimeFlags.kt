package org.emulinker.config

import java.nio.charset.Charset
import java.time.Duration
import javax.inject.Singleton
import org.apache.commons.configuration.Configuration

/** Configuration flags that are set at startup and do not change until the job is restarted. */
@Singleton
data class RuntimeFlags
    constructor(
        val allowMultipleConnections: Boolean,
        val allowSinglePlayer: Boolean,
        val metricsEnabled: Boolean,
        val touchEmulinker: Boolean,
        val touchKaillera: Boolean,
        val charset: Charset,
        val metricsLoggingFrequency: Duration,
        val connectionTypes: List<String>,
        val coreThreadPoolSize: Int,
        val chatFloodTime: Int,
        val createGameFloodTime: Int,
        val gameAutoFireSensitivity: Int,
        val gameBufferSize: Int,
        val gameDesynchTimeouts: Int,
        val gameTimeoutMillis: Int,
        val idleTimeout: Int,
        val keepAliveTimeout: Int,
        val maxChatLength: Int,
        val maxClientNameLength: Int,
        val maxGameChatLength: Int,
        val maxGameNameLength: Int,
        val maxGames: Int,
        val maxPing: Int,
        val maxQuitMessageLength: Int,
        val maxUserNameLength: Int,
        val maxUsers: Int,
        val serverAddress: String,
        val serverLocation: String,
        val serverName: String,
        val serverWebsite: String,
        val v086BufferSize: Int,
        val twitterBroadcastDelay: Duration,
        val twitterEnabled: Boolean,
        val twitterOAuthAccessToken: String,
        val twitterOAuthAccessTokenSecret: String,
        val twitterOAuthConsumerKey: String,
        val twitterOAuthConsumerSecret: String,
        val twitterPreventBroadcastNameSuffixes: List<String>,
    ) {

  init {
    // Note: this used to be max 30, but for some reason we had 31 set as the default in the config.
    // Setting this to max 31 so we don't break existing users.
    // TODO(nue): Just remove this restriction as it seems unhelpful?
    require(maxUserNameLength <= 31) { "server.maxUserNameLength must be <= 31" }
    require(maxGameNameLength <= 127) { "server.maxGameNameLength must be <= 127" }
    require(maxClientNameLength <= 127) { "server.maxClientNameLength must be <= 127" }
    require(maxPing > 0) { "server.maxPing can not be <= 0" }
    require(maxPing <= 1000) { "server.maxPing can not be > 1000" }
    require(keepAliveTimeout > 0) { "server.keepAliveTimeout must be > 0 (190 is recommended)" }

    require(gameBufferSize > 0) { "game.bufferSize can not be <= 0" }
    require(gameTimeoutMillis > 0) { "game.timeoutMillis can not be <= 0" }
    require(gameAutoFireSensitivity in 0..5) { "game.defaultAutoFireSensitivity must be 0-5" }
    for (s in connectionTypes) {
      try {
        val ct = s.toInt()
        require(ct in 1..6) { "Invalid connectionType: $s" }
      } catch (e: NumberFormatException) {
        throw IllegalStateException("Invalid connectionType: $s", e)
      }
    }
  }

  companion object {

    fun loadFromApacheConfiguration(config: Configuration): RuntimeFlags {
      @Suppress("UNCHECKED_CAST") // TODO(nue): Replace commons-configurations.
      return RuntimeFlags(
          allowMultipleConnections = config.getBoolean("server.allowMultipleConnections"),
          allowSinglePlayer = config.getBoolean("server.allowSinglePlayer", true),
          charset = Charset.forName(config.getString("emulinker.charset")),
          chatFloodTime = config.getInt("server.chatFloodTime"),
          connectionTypes = config.getList("server.allowedConnectionTypes") as List<String>,
          // TODO(nue): Experiment with Runtime.getRuntime().availableProcessors()
          coreThreadPoolSize = config.getInt("server.coreThreadpoolSize", 5),
          createGameFloodTime = config.getInt("server.createGameFloodTime"),
          metricsEnabled = config.getBoolean("metrics.enabled", false),
          metricsLoggingFrequency =
              Duration.ofSeconds(config.getInt("metrics.loggingFrequencySeconds", 30).toLong()),
          gameAutoFireSensitivity = config.getInt("game.defaultAutoFireSensitivity"),
          gameBufferSize = config.getInt("game.bufferSize"),
          gameDesynchTimeouts = config.getInt("game.desynchTimeouts"),
          gameTimeoutMillis = config.getInt("game.timeoutMillis"),
          idleTimeout = config.getInt("server.idleTimeout"),
          keepAliveTimeout = config.getInt("server.keepAliveTimeout"),
          maxChatLength = config.getInt("server.maxChatLength"),
          maxClientNameLength = config.getInt("server.maxClientNameLength"),
          maxGameChatLength = config.getInt("server.maxGameChatLength"),
          maxGameNameLength = config.getInt("server.maxGameNameLength"),
          maxGames = config.getInt("server.maxGames"),
          maxPing = config.getInt("server.maxPing"),
          maxQuitMessageLength = config.getInt("server.maxQuitMessageLength"),
          maxUserNameLength = config.getInt("server.maxUserNameLength"),
          maxUsers = config.getInt("server.maxUsers"),
          serverAddress = config.getString("masterList.serverConnectAddress", ""),
          serverLocation = config.getString("masterList.serverLocation", "Unknown"),
          serverName = config.getString("masterList.serverName", "Emulinker Server"),
          serverWebsite = config.getString("masterList.serverWebsite", ""),
          touchEmulinker = config.getBoolean("masterList.touchEmulinker", false),
          touchKaillera = config.getBoolean("masterList.touchKaillera", false),
          v086BufferSize = config.getInt("controllers.v086.bufferSize", 4096),
          twitterBroadcastDelay =
              Duration.ofSeconds(config.getInt("twitter.broadcastDelaySeconds", 15).toLong()),
          twitterEnabled = config.getBoolean("twitter.enabled", false),
          twitterOAuthAccessToken = config.getString("twitter.auth.oAuthAccessToken", ""),
          twitterOAuthAccessTokenSecret =
              config.getString("twitter.auth.oAuthAccessTokenSecret", ""),
          twitterOAuthConsumerKey = config.getString("twitter.auth.oAuthConsumerKey", ""),
          twitterOAuthConsumerSecret =
              config.getString(
                  "twitter.auth.oAuthConsumerSecret", ""), // TODO(nue): Read these from a file
          twitterPreventBroadcastNameSuffixes = listOf("待", "街", "町", "再起", "侍ち"))
      // ImmutableList.copyOf(config.getString("twitter.preventBroadcastNameSuffixes",
      // "").split(",")))
    }
  }
}

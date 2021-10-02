package org.emulinker.config;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import javax.inject.Singleton;
import org.apache.commons.configuration.Configuration;

/** Configuration flags that are set at startup and do not change until the job is restarted. */
@AutoValue
@Singleton
public abstract class RuntimeFlags {

  public abstract boolean allowMultipleConnections();

  public abstract boolean allowSinglePlayer();

  public abstract boolean metricsEnabled();

  public abstract boolean touchEmulinker();

  public abstract boolean touchKaillera();

  public abstract Charset charset();

  public abstract Duration metricsLoggingFrequency();

  public abstract ImmutableList<String> connectionTypes();

  public abstract int coreThreadPoolSize();

  public abstract int chatFloodTime();

  public abstract int createGameFloodTime();

  public abstract int gameAutoFireSensitivity();

  public abstract int gameBufferSize();

  public abstract int gameDesynchTimeouts();

  public abstract int gameTimeoutMillis();

  public abstract int idleTimeout();

  public abstract int keepAliveTimeout();

  public abstract int maxChatLength();

  public abstract int maxClientNameLength();

  public abstract int maxGameChatLength();

  public abstract int maxGameNameLength();

  public abstract int maxGames();

  public abstract int maxPing();

  public abstract int maxQuitMessageLength();

  public abstract int maxUserNameLength();

  public abstract int maxUsers();

  public abstract String serverAddress();

  public abstract String serverLocation();

  public abstract String serverName();

  public abstract String serverWebsite();

  public abstract int v086BufferSize();

  public static Builder builder() {
    return new AutoValue_RuntimeFlags.Builder();
  }

  public static RuntimeFlags loadFromApacheConfiguration(Configuration config) {
    return builder()
        .setAllowMultipleConnections(config.getBoolean("server.allowMultipleConnections"))
        .setAllowSinglePlayer(config.getBoolean("server.allowSinglePlayer", true))
        .setCharset(Charset.forName(config.getString("emulinker.charset")))
        .setChatFloodTime(config.getInt("server.chatFloodTime"))
        .setConnectionTypes(config.getList("server.allowedConnectionTypes"))
        // TODO(nue): Experiment with Runtime.getRuntime().availableProcessors().
        .setCoreThreadPoolSize(config.getInt("server.coreThreadpoolSize", 5))
        .setCreateGameFloodTime(config.getInt("server.createGameFloodTime"))
        .setMetricsEnabled(config.getBoolean("metrics.enabled", false))
        .setMetricsLoggingFrequency(
            Duration.ofSeconds(config.getInt("metrics.loggingFrequencySeconds", 30)))
        .setGameAutoFireSensitivity(config.getInt("game.defaultAutoFireSensitivity"))
        .setGameBufferSize(config.getInt("game.bufferSize"))
        .setGameDesynchTimeouts(config.getInt("game.desynchTimeouts"))
        .setGameTimeoutMillis(config.getInt("game.timeoutMillis"))
        .setIdleTimeout(config.getInt("server.idleTimeout"))
        .setKeepAliveTimeout(config.getInt("server.keepAliveTimeout"))
        .setMaxChatLength(config.getInt("server.maxChatLength"))
        .setMaxClientNameLength(config.getInt("server.maxClientNameLength"))
        .setMaxGameChatLength(config.getInt("server.maxGameChatLength"))
        .setMaxGameNameLength(config.getInt("server.maxGameNameLength"))
        .setMaxGames(config.getInt("server.maxGames"))
        .setMaxPing(config.getInt("server.maxPing"))
        .setMaxQuitMessageLength(config.getInt("server.maxQuitMessageLength"))
        .setMaxUserNameLength(config.getInt("server.maxUserNameLength"))
        .setMaxUsers(config.getInt("server.maxUsers"))
        .setServerAddress(config.getString("masterList.serverConnectAddress", ""))
        .setServerLocation(config.getString("masterList.serverLocation", "Unknown"))
        .setServerName(config.getString("masterList.serverName", "Emulinker Server"))
        .setServerWebsite(config.getString("masterList.serverWebsite", ""))
        .setTouchEmulinker(config.getBoolean("masterList.touchEmulinker", false))
        .setTouchKaillera(config.getBoolean("masterList.touchKaillera", false))
        .setV086BufferSize(config.getInt("controllers.v086.bufferSize", 4096))
        .build();
  }

  private void validateFlags() {
    // Note: this used to be max 30, but for some reason we had 31 set as the default in the config.
    // Setting this to max 31 so we don't break existing users.
    // TODO(nue): Just remove this restriction as it seems unhelpful?
    checkArgument(maxUserNameLength() <= 31, "server.maxUserNameLength must be <= 31");
    checkArgument(maxGameNameLength() <= 127, "server.maxGameNameLength must be <= 127");
    checkArgument(maxClientNameLength() <= 127, "server.maxClientNameLength must be <= 127");
    checkArgument(maxPing() > 0, "server.maxPing can not be <= 0");
    checkArgument(maxPing() <= 1000, "server.maxPing can not be > 1000");
    checkArgument(
        keepAliveTimeout() > 0, "server.keepAliveTimeout must be > 0 (190 is recommended)");
    checkArgument(gameBufferSize() > 0, "game.bufferSize can not be <= 0");
    checkArgument(gameTimeoutMillis() > 0, "game.timeoutMillis can not be <= 0");
    checkArgument(
        gameAutoFireSensitivity() >= 0 && gameAutoFireSensitivity() <= 5,
        "game.defaultAutoFireSensitivity must be 0-5");

    for (String s : connectionTypes()) {
      try {
        int ct = Integer.parseInt(s);
        checkArgument(ct >= 1 && ct <= 6, "Invalid connectionType: " + s);
      } catch (NumberFormatException e) {
        throw new IllegalStateException("Invalid connectionType: " + s, e);
      }
    }
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setAllowMultipleConnections(boolean allowMultipleConnections);

    public abstract Builder setAllowSinglePlayer(boolean allowSinglePlayer);

    public abstract Builder setCharset(Charset charset);

    public abstract Builder setChatFloodTime(int chatFloodTime);

    public abstract Builder setConnectionTypes(List<String> connectionTypes);

    public abstract Builder setCoreThreadPoolSize(int coreThreadPoolSize);

    public abstract Builder setCreateGameFloodTime(int createGameFloodTime);

    public abstract Builder setMetricsEnabled(boolean enableMetrics);

    public abstract Builder setMetricsLoggingFrequency(Duration metricsLoggingFrequency);

    public abstract Builder setGameAutoFireSensitivity(int gameAutoFireSensitivity);

    public abstract Builder setGameBufferSize(int gameBufferSize);

    public abstract Builder setGameDesynchTimeouts(int gameDesynchTimeouts);

    public abstract Builder setGameTimeoutMillis(int gameTimeoutMillis);

    public abstract Builder setIdleTimeout(int idleTimeout);

    public abstract Builder setKeepAliveTimeout(int keepAliveTimeout);

    public abstract Builder setMaxChatLength(int maxChatLength);

    public abstract Builder setMaxClientNameLength(int maxClientNameLength);

    public abstract Builder setMaxGameChatLength(int maxGameChatLength);

    public abstract Builder setMaxGameNameLength(int maxGameNameLength);

    public abstract Builder setMaxGames(int maxGames);

    public abstract Builder setMaxPing(int maxPing);

    public abstract Builder setMaxQuitMessageLength(int maxQuitMessageLength);

    public abstract Builder setMaxUserNameLength(int maxUserNameLength);

    public abstract Builder setMaxUsers(int maxUsers);

    public abstract Builder setTouchEmulinker(boolean touchEmulinker);

    public abstract Builder setTouchKaillera(boolean touchKaillera);

    public abstract Builder setServerAddress(String serverAddress);

    public abstract Builder setServerLocation(String serverLocation);

    public abstract Builder setServerName(String serverName);

    public abstract Builder setServerWebsite(String serverWebsite);

    public abstract Builder setV086BufferSize(int v086BufferSize);

    protected abstract RuntimeFlags innerBuild();

    public final RuntimeFlags build() {
      RuntimeFlags flags = innerBuild();
      flags.validateFlags();
      return flags;
    }
  }
}

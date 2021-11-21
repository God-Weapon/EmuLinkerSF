package org.emulinker.kaillera.lookingforgame;

import com.google.common.flogger.FluentLogger;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.model.KailleraUser;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;

/**
 * Observes when a user is looking for a game opponent and publishes a report to one or more
 * external services (e.g. Twitter, Discord).
 */
@Singleton
public final class TwitterBroadcaster {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RuntimeFlags flags;
  private final Twitter twitter;

  private static final ConcurrentMap<LookingForGameEvent, Disposable> pendingReports =
      new ConcurrentHashMap<>();

  private static final ConcurrentMap<LookingForGameEvent, Long> postedTweets =
      new ConcurrentHashMap<>();

  @Inject
  TwitterBroadcaster(RuntimeFlags flags, Twitter twitter) {
    this.flags = flags;
    this.twitter = twitter;
  }

  /**
   * After the number of seconds defined in the config, it will report.
   *
   * @return Whether or not the timer was started.
   */
  public boolean reportAndStartTimer(LookingForGameEvent lookingForGameEvent) {
    if (!flags.twitterEnabled()) {
      return false;
    }
    String username = lookingForGameEvent.user().getName();
    // TODO(nue): Abstract the @ into a status field instead of keeping it in the name.
    // Note: This isn't the normal @ character..
    if (username.contains("＠")) {
      String afterAt = username.substring(username.indexOf("＠"));
      if (flags.twitterPreventBroadcastNameSuffixes().stream()
          .anyMatch((suffix) -> afterAt.contains(suffix))) {
        return false;
      }
    }

    // *Chat or *Away "games".
    if (lookingForGameEvent.gameTitle().startsWith("*")) {
      return false;
    }

    Disposable disposable =
        Completable.timer(
                flags.twitterBroadcastDelay().getSeconds(), TimeUnit.SECONDS, Schedulers.io())
            .subscribe(
                () -> {
                  pendingReports.remove(lookingForGameEvent);

                  KailleraUser user = lookingForGameEvent.user();
                  String message =
                      String.format(
                          "User: %s\nGame: %s\nServer: %s (%s)",
                          user.getName(),
                          lookingForGameEvent.gameTitle(),
                          flags.serverName(),
                          flags.serverAddress());

                  Status tweet = twitter.updateStatus(message);
                  user.getGame().announce(getUrl(tweet), user);

                  logger.atInfo().log("Posted tweet: %s", getUrl(tweet));
                  postedTweets.put(lookingForGameEvent, tweet.getId());
                });
    pendingReports.put(lookingForGameEvent, disposable);
    return true;
  }

  public boolean cancelActionsForUser(int userId) {
    return cancelMatchingEvents((event) -> event.user().getID() == userId);
  }

  public boolean cancelActionsForGame(int gameId) {
    return cancelMatchingEvents((event) -> event.gameId() == gameId);
  }

  private boolean cancelMatchingEvents(Predicate<? super LookingForGameEvent> predicate) {
    boolean anyModified =
        pendingReports.keySet().stream()
            .filter(predicate)
            // Use map instead of foreach because it lets us return whether or not something was
            // modified.
            .map(
                (event) -> {
                  Disposable disposable = pendingReports.get(event);
                  if (disposable != null) {
                    disposable.dispose();
                    pendingReports.remove(event);
                    logger.atInfo().log("Prevented pending tweet");
                  }
                  return event;
                })
            .findAny()
            .isPresent();

    boolean tweetsClosed =
        postedTweets.keySet().stream()
            .filter(predicate)
            // Use map instead of foreach because it lets us return whether or not something was
            // modified.
            .map(
                (event) -> {
                  Long tweetId = postedTweets.get(event);
                  if (tweetId != null) {
                    postedTweets.remove(event);

                    Observable.just(tweetId)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            (id) -> {
                              StatusUpdate reply = new StatusUpdate("〆");
                              reply.setInReplyToStatusId(id);
                              Status tweet = twitter.updateStatus(reply);
                              logger.atInfo().log("Posted tweet canceling LFG: %s", getUrl(tweet));
                            });
                  }
                  return event;
                })
            .findAny()
            .isPresent();

    return anyModified || tweetsClosed;
  }

  private static String getUrl(Status tweet) {
    return String.format(
        "https://twitter.com/%s/status/%d", tweet.getUser().getScreenName(), tweet.getId());
  }
}

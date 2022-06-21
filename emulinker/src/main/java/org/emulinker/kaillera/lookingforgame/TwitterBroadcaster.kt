package org.emulinker.kaillera.lookingforgame

import com.google.common.flogger.FluentLogger
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.config.RuntimeFlags
import org.emulinker.kaillera.model.KailleraUser
import twitter4j.Status
import twitter4j.StatusUpdate
import twitter4j.Twitter

private val logger = FluentLogger.forEnclosingClass()

private fun getUrl(tweet: Status) =
    "https://twitter.com/${tweet.user.screenName}/status/${tweet.id}"

/**
 * Observes when a user is looking for a game opponent and publishes a report to one or more
 * external services (e.g. Twitter, Discord).
 */
@Singleton
class TwitterBroadcaster
    @Inject
    internal constructor(private val flags: RuntimeFlags, private val twitter: Twitter) {

  private val pendingReports: ConcurrentMap<LookingForGameEvent, Disposable> = ConcurrentHashMap()
  private val postedTweets: ConcurrentMap<LookingForGameEvent, Long> = ConcurrentHashMap()

  /**
   * After the number of seconds defined in the config, it will report.
   *
   * @return Whether or not the timer was started.
   */
  fun reportAndStartTimer(lookingForGameEvent: LookingForGameEvent): Boolean {
    if (!flags.twitterEnabled) {
      return false
    }
    val username: String = lookingForGameEvent.user.name!!
    // TODO(nue): Abstract the @ into a status field instead of keeping it in the name.
    // Note: This isn't the normal @ character..
    if (username.contains("＠")) {
      val afterAt = username.substring(username.indexOf("＠"))
      if (flags.twitterPreventBroadcastNameSuffixes.stream().anyMatch { suffix: String? ->
        afterAt.contains(suffix!!)
      }) {
        return false
      }
    }

    // *Chat or *Away "games".
    if (lookingForGameEvent.gameTitle.startsWith("*")) {
      return false
    }
    val disposable =
        Completable.timer(
                flags.twitterBroadcastDelay.inWholeSeconds, TimeUnit.SECONDS, Schedulers.io())
            .subscribe {
              pendingReports.remove(lookingForGameEvent)
              val user: KailleraUser = lookingForGameEvent.user
              val message =
                  java.lang.String.format(
                      "User: %s\nGame: %s\nServer: %s (%s)",
                      user.name,
                      lookingForGameEvent.gameTitle,
                      flags.serverName,
                      flags.serverAddress)
              val tweet = twitter.updateStatus(message)
              user.game!!.announce(getUrl(tweet), user)
              logger.atInfo().log("Posted tweet: %s", getUrl(tweet))
              postedTweets[lookingForGameEvent] = tweet.id
            }
    pendingReports[lookingForGameEvent] = disposable
    return true
  }

  fun cancelActionsForUser(userId: Int): Boolean {
    return cancelMatchingEvents { event: LookingForGameEvent -> event.user.id == userId }
  }

  fun cancelActionsForGame(gameId: Int): Boolean {
    return cancelMatchingEvents { event: LookingForGameEvent -> event.gameId == gameId }
  }

  private fun cancelMatchingEvents(predicate: (LookingForGameEvent) -> Boolean): Boolean {
    val anyModified =
        pendingReports
            .keys
            .stream()
            .filter(
                predicate) // Use map instead of foreach because it lets us return whether or not
            // something was
            // modified.
            .map { event: LookingForGameEvent ->
              val disposable = pendingReports[event]
              if (disposable != null) {
                disposable.dispose()
                pendingReports.remove(event)
                logger.atInfo().log("Prevented pending tweet")
              }
              event
            }
            .findAny()
            .isPresent
    val tweetsClosed =
        postedTweets
            .keys
            .stream()
            .filter(
                predicate) // Use map instead of foreach because it lets us return whether or not
            // something was
            // modified.
            .map { event: LookingForGameEvent ->
              val tweetId = postedTweets[event]
              if (tweetId != null) {
                postedTweets.remove(event)
                Observable.just(tweetId).subscribeOn(Schedulers.io()).subscribe { id: Long? ->
                  val reply = StatusUpdate("〆")
                  reply.inReplyToStatusId = id!!
                  val tweet = twitter.updateStatus(reply)
                  logger.atInfo().log("Posted tweet canceling LFG: %s", getUrl(tweet))
                }
              }
              event
            }
            .findAny()
            .isPresent
    return anyModified || tweetsClosed
  }
}

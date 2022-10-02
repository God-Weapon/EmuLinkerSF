package org.emulinker.kaillera.lookingforgame

import com.google.common.flogger.FluentLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
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

  private val scope = CoroutineScope(Dispatchers.IO)

  private val pendingReports: ConcurrentMap<LookingForGameEvent, Job> = ConcurrentHashMap()
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
      if (flags.twitterPreventBroadcastNameSuffixes.any { afterAt.contains(it) }) {
        return false
      }
    }

    // *Chat or *Away "games".
    if (lookingForGameEvent.gameTitle.startsWith("*")) {
      return false
    }

    pendingReports[lookingForGameEvent] =
        scope.launch {
          delay(flags.twitterBroadcastDelay)

          pendingReports.remove(lookingForGameEvent)
          val user: KailleraUser = lookingForGameEvent.user
          val message =
              """
                  User: ${user.name}
                  Game: ${lookingForGameEvent.gameTitle}
                  Server: ${flags.serverName} (${flags.serverAddress})""".trimIndent()
          val tweet = twitter.updateStatus(message)
          user.game!!.announce(getUrl(tweet), user)
          logger.atInfo().log("Posted tweet: %s", getUrl(tweet))
          postedTweets[lookingForGameEvent] = tweet.id
        }
    return true
  }

  fun cancelActionsForUser(userId: Int): Boolean {
    return cancelMatchingEvents { it.user.id == userId }
  }

  fun cancelActionsForGame(gameId: Int): Boolean {
    return cancelMatchingEvents { it.gameId == gameId }
  }

  private fun cancelMatchingEvents(predicate: (LookingForGameEvent) -> Boolean): Boolean {
    var anyModified = false
    pendingReports.keys.asSequence().filter { predicate(it) }.forEach { event ->
      val job = pendingReports[event]
      if (job != null) {
        job.cancel()
        pendingReports.remove(event)
        logger.atInfo().log("Prevented pending tweet")
      }
      anyModified = true
    }

    var tweetsClosed = false
    postedTweets.keys.asSequence().filter(predicate).forEach { event: LookingForGameEvent ->
      val tweetId = postedTweets[event]
      if (tweetId != null) {
        postedTweets.remove(event)

        scope.launch {
          val reply = StatusUpdate("〆")
          reply.inReplyToStatusId = tweetId
          val tweet = twitter.updateStatus(reply)
          logger.atInfo().log("Posted tweet canceling LFG: %s", getUrl(tweet))
        }
      }
      tweetsClosed = true
    }
    return anyModified || tweetsClosed
  }
}

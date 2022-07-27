package org.emulinker.kaillera.access

import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration
import org.emulinker.util.WildcardStringPattern

sealed class TemporaryAttribute(accessStr: String, val duration: Duration) {
  private val patterns =
      accessStr
          .lowercase(Locale.getDefault())
          .splitToSequence("|")
          .map { WildcardStringPattern(it) }
          .toList()

  @OptIn(ExperimentalTime::class)
  private val endTime = Instant.now().plus(duration.toJavaDuration())

  val isExpired
    get() = Instant.now().isAfter(endTime)

  fun matches(address: String): Boolean {
    return patterns.any { it.match(address) }
  }
}

class TempBan(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

class TempAdmin(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

class TempModerator(accessStr: String, duration: Duration) :
    TemporaryAttribute(accessStr, duration)

class TempElevated(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

class Silence(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

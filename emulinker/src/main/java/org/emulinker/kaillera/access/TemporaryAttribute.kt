package org.emulinker.kaillera.access

import java.time.Duration
import java.time.Instant
import java.util.*
import org.emulinker.util.WildcardStringPattern

sealed class TemporaryAttribute(accessStr: String, duration: Duration) {
  private val patterns =
      accessStr
          .lowercase(Locale.getDefault())
          .splitToSequence("|")
          .map { WildcardStringPattern(it) }
          .toList()
  private val endTime = Instant.now().plus(duration)

  val isExpired
    get() = Instant.now().isAfter(endTime)

  fun matches(address: String?): Boolean {
    return patterns.any { it.match(address) }
  }
}

class TempBan(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

class TempAdmin(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

class TempModerator(accessStr: String, duration: Duration) :
    TemporaryAttribute(accessStr, duration)

class TempElevated(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

class Silence(accessStr: String, duration: Duration) : TemporaryAttribute(accessStr, duration)

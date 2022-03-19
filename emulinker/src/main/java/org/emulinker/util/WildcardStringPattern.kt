package org.emulinker.util

import java.util.LinkedList
import java.util.StringTokenizer

class WildcardStringPattern(pattern: String?) {
  protected var equals = false
  protected var startsWith = false
  protected var endsWith = false
  protected var contains = false

  private var startString = ""
  private var endString = ""
  private val containsStrings = LinkedList<String>()

  fun match(s: String?): Boolean {
    var s = s
    if (s == null || s == "") return false
    if (equals && s != startString) return false
    if (startsWith && !s.startsWith(startString)) return false
    if (endsWith && !s.endsWith(endString)) return false
    if (contains) {
      for (pattern in containsStrings) {
        val idx = s!!.indexOf(pattern)
        if (idx == -1) return false
        if (idx + pattern.length == s.length) {
          // Match occured at the end of the string.
          // In that case, the substring assignment below
          // would fail. Continue the loop. If there are
          // more items to match, the next test will fail.
          // Otherwise, the match has succeeded.
          s = ""
          continue
        }
        s = s.substring(idx + pattern.length)
      }
    }
    return true
  }

  override fun toString(): String {
    if (equals) return startString

    // If there is a startString, append "*" to it.
    // (There must be a follow up, or this would be an equals match.)
    // If startString is null, the remainder of the pattern is
    // an endsWith or contains. In either case, it must start
    // with "*".
    var s = startString
    s += "*"
    for (pattern in containsStrings) s += pattern + "*"
    s += endString
    return s
  }

  companion object {

    fun main(args: Array<String>) {
      val test = WildcardStringPattern(args[0])
      for (i in 1 until args.size) {
        val match = test.match(args[i])
        println(args[i] + " = " + match)
      }
    }
  }

  init {
    if (pattern == null || pattern == "") {
      // match() function will always return true.
    } else {
      val elements = LinkedList<String>()
      val st = StringTokenizer(pattern, "*", true)
      while (st.hasMoreElements()) {
        elements.add(st.nextToken())
      }
      if (elements.size == 1) {
        val s = elements.first
        if (s != "*") {

          // Real text
          equals = true
          startString = elements.first
        }
      } else {
        // Multiple elements in the pattern
        // Pick off start and end strings.
        // Add remaining items to the list of contains strings.
        if (elements.first != "*") {
          startsWith = true
          startString = elements.first
          elements.removeFirst()
        }
        if (elements.last != "*") {
          endsWith = true
          endString = elements.last
          elements.removeLast()
        }
        for (x in elements) {
          if (x == "*") continue
          containsStrings.add(x)
          contains = true
        }
      }
    }
  }
}

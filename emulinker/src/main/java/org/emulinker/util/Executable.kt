package org.emulinker.util

import java.lang.Runnable

interface Executable : Runnable {
  val running: Boolean

  fun stop()
}

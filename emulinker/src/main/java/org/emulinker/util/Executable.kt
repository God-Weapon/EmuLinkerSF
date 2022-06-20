package org.emulinker.util

import java.lang.Runnable

interface Executable : Runnable {
  val threadIsActive: Boolean

  fun stop()
}

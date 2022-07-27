package org.emulinker.util

import kotlin.coroutines.CoroutineContext

interface Executable {
  val threadIsActive: Boolean

  suspend fun stop()

  suspend fun run(globalContext: CoroutineContext)
}

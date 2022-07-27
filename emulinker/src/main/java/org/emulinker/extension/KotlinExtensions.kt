package org.emulinker.extension

import com.google.common.flogger.LoggingApi

/**
 * A more efficient logging method for fine logging because the logged string is not prepared unless
 * the method is called.
 */
@Deprecated(level = DeprecationLevel.ERROR, message = "Prefer using the normal Flogger methods")
fun <T : LoggingApi<T>?> LoggingApi<T>.logLazy(lazyMessage: () -> String) {
  if (this !is LoggingApi.NoOp) {
    log(lazyMessage())
  }
}

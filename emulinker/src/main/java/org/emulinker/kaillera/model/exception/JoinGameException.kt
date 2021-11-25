package org.emulinker.kaillera.model.exception

import java.lang.Exception

class JoinGameException : ActionException {
  constructor(message: String?) : super(message)
  constructor(message: String?, source: Exception?) : super(message, source)
}

package org.emulinker.kaillera.model.exception

import java.lang.Exception

class ServerFullException : NewConnectionException {
  constructor(message: String?) : super(message)
  constructor(message: String?, source: Exception?) : super(message, source)
}

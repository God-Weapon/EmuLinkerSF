package org.emulinker.kaillera.model.exception

open class ActionException : Exception {
  constructor()
  constructor(message: String?) : super(message)
  constructor(message: String?, source: Exception?) : super(message, source)
}

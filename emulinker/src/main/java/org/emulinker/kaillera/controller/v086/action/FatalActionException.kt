package org.emulinker.kaillera.controller.v086.action

class FatalActionException : Exception {
  constructor(message: String?) : super(message) {}
  constructor(message: String?, source: Exception?) : super(message, source) {}
}

package org.emulinker.kaillera.model.exception;

public class ConnectionTypeException extends LoginException {
  public ConnectionTypeException(String message) {
    super(message);
  }

  public ConnectionTypeException(String message, Exception source) {
    super(message, source);
  }
}

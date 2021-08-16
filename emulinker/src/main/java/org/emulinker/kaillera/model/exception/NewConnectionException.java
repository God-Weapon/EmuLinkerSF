package org.emulinker.kaillera.model.exception;

public class NewConnectionException extends ActionException {
  public NewConnectionException(String message) {
    super(message);
  }

  public NewConnectionException(String message, Exception source) {
    super(message, source);
  }
}

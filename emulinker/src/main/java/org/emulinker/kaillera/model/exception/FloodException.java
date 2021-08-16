package org.emulinker.kaillera.model.exception;

public class FloodException extends ActionException {
  public FloodException() {
    super();
  }

  public FloodException(String message) {
    super(message);
  }

  public FloodException(String message, Exception source) {
    super(message, source);
  }
}

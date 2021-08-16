package org.emulinker.kaillera.model.exception;

public class QuitGameException extends ActionException {
  public QuitGameException(String message) {
    super(message);
  }

  public QuitGameException(String message, Exception source) {
    super(message, source);
  }
}

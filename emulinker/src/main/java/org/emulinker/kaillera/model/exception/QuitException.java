package org.emulinker.kaillera.model.exception;

public class QuitException extends ActionException {
  public QuitException(String message) {
    super(message);
  }

  public QuitException(String message, Exception source) {
    super(message, source);
  }
}

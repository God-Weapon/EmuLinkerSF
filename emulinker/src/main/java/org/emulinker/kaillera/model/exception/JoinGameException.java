package org.emulinker.kaillera.model.exception;

public class JoinGameException extends ActionException {
  public JoinGameException(String message) {
    super(message);
  }

  public JoinGameException(String message, Exception source) {
    super(message, source);
  }
}

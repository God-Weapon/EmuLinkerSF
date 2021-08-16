package org.emulinker.kaillera.model.exception;

public class ChatException extends ActionException {
  public ChatException(String message) {
    super(message);
  }

  public ChatException(String message, Exception source) {
    super(message, source);
  }
}

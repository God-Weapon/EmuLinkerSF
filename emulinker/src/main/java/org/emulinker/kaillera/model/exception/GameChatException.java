package org.emulinker.kaillera.model.exception;

public class GameChatException extends ActionException {
  public GameChatException(String message) {
    super(message);
  }

  public GameChatException(String message, Exception source) {
    super(message, source);
  }
}

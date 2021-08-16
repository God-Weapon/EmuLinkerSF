package org.emulinker.kaillera.model.exception;

public class InvalidRequestException extends Exception {
  public InvalidRequestException(String message) {
    super(message);
  }

  public InvalidRequestException(String message, Exception source) {
    super(message, source);
  }
}

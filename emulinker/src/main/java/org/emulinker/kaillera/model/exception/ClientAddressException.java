package org.emulinker.kaillera.model.exception;

public class ClientAddressException extends LoginException {
  public ClientAddressException(String message) {
    super(message);
  }

  public ClientAddressException(String message, Exception source) {
    super(message, source);
  }
}

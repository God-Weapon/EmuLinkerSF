package org.emulinker.kaillera.controller;

import java.net.InetSocketAddress;
import org.emulinker.kaillera.model.KailleraServer;
import org.emulinker.kaillera.model.exception.*;

public interface KailleraServerController {
  public int newConnection(InetSocketAddress clientSocketAddress, String protocol)
      throws ServerFullException, NewConnectionException;

  public KailleraServer getServer();

  public int getBufferSize();

  public String getVersion();

  public int getNumClients();

  public String[] getClientTypes();

  public void start();

  public void stop();
}

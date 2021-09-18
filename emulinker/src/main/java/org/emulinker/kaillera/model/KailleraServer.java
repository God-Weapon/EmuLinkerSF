package org.emulinker.kaillera.model;

import java.net.InetSocketAddress;
import java.util.Collection;
import org.emulinker.kaillera.access.AccessManager;
import org.emulinker.kaillera.model.event.KailleraEventListener;
import org.emulinker.kaillera.model.exception.*;
import org.emulinker.kaillera.model.impl.KailleraUserImpl;
import org.emulinker.kaillera.model.impl.Trivia;
import org.emulinker.release.*;

public interface KailleraServer {
  public ReleaseInfo getReleaseInfo();

  public int getNumUsers();

  public int getNumGames();

  public int getMaxUsers();

  public void setTrivia(Trivia trivia);

  public void setSwitchTrivia(boolean switchTrivia);

  public Trivia getTrivia();

  public boolean getSwitchTrivia();

  public void announce(String announcement, boolean gamesAlso, KailleraUserImpl user);

  public int getMaxGames();

  public int getMaxPing();

  public AccessManager getAccessManager();

  public Collection<? extends KailleraUser> getUsers();

  public Collection<? extends KailleraGame> getGames();

  public KailleraUser getUser(int userID);

  public KailleraGame getGame(int gameID);

  public boolean checkMe(KailleraUser user, String message);

  public KailleraUser newConnection(
      InetSocketAddress clientSocketAddress, String protocol, KailleraEventListener listener)
      throws ServerFullException, NewConnectionException;

  public void login(KailleraUser user)
      throws PingTimeException, ClientAddressException, ConnectionTypeException, UserNameException,
          LoginException;

  public void chat(KailleraUser user, String message) throws ChatException, FloodException;

  public KailleraGame createGame(KailleraUser user, String romName)
      throws CreateGameException, FloodException;

  public void quit(KailleraUser user, String message)
      throws QuitException, DropGameException, QuitGameException, CloseGameException;

  public abstract void start();

  public abstract void stop();
}

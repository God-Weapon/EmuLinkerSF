package org.emulinker.kaillera.access;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.commons.logging.*;
import org.emulinker.util.WildcardStringPattern;
import org.picocontainer.Startable;

public class AccessManager2 implements AccessManager, Startable, Runnable {
  static {
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "60");
  }

  private static Log log = LogFactory.getLog(AccessManager2.class);

  private ThreadPoolExecutor threadPool;
  private boolean isRunning = false;
  private boolean stopFlag = false;

  private File accessFile;
  private long lastLoadModifiedTime = -1;

  private List<UserAccess> userList = new CopyOnWriteArrayList<UserAccess>();
  private List<GameAccess> gameList = new CopyOnWriteArrayList<GameAccess>();
  private List<EmulatorAccess> emulatorList = new CopyOnWriteArrayList<EmulatorAccess>();
  private List<AddressAccess> addressList = new CopyOnWriteArrayList<AddressAccess>();
  private List<TempBan> tempBanList = new CopyOnWriteArrayList<TempBan>();
  private List<TempAdmin> tempAdminList = new CopyOnWriteArrayList<TempAdmin>();
  private List<TempModerator> tempModeratorList = new CopyOnWriteArrayList<TempModerator>();
  private List<TempElevated> tempElevatedList = new CopyOnWriteArrayList<TempElevated>();
  private List<Silence> silenceList = new CopyOnWriteArrayList<Silence>();

  public AccessManager2(ThreadPoolExecutor threadPool)
      throws NoSuchElementException, FileNotFoundException {
    this.threadPool = threadPool;

    URL url = AccessManager2.class.getResource("/access.cfg");
    if (url == null) throw new FileNotFoundException("Resource not found: /access.conf");

    try {
      accessFile = new File(url.toURI());
    } catch (URISyntaxException e) {
      throw new FileNotFoundException(e.getMessage());
    }

    if (!accessFile.exists()) throw new FileNotFoundException("Resource not found: /access.conf");

    if (!accessFile.canRead()) throw new FileNotFoundException("Can not read: /access.conf");

    loadAccess();

    threadPool.execute(this);
  }

  @Override
  public synchronized void start() {
    log.debug("AccessManager2 thread received start request!");
    log.debug(
        "AccessManager2 thread starting (ThreadPool:"
            + threadPool.getActiveCount()
            + "/"
            + threadPool.getPoolSize()
            + ")");
    threadPool.execute(this);
    Thread.yield();
  }

  public boolean isRunning() {
    return isRunning;
  }

  @Override
  public synchronized void stop() {
    log.debug("AccessManager2 thread received stop request!");

    if (!isRunning()) {
      log.debug("KailleraServer thread stop request ignored: not running!");
      return;
    }

    stopFlag = true;

    userList.clear();
    gameList.clear();
    emulatorList.clear();
    addressList.clear();
    tempBanList.clear();
    tempElevatedList.clear();
    tempAdminList.clear();
    silenceList.clear();
  }

  @Override
  public void run() {
    isRunning = true;
    log.debug("AccessManager2 thread running...");

    try {
      while (!stopFlag) {
        try {
          Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
          log.error("Sleep Interrupted!", e);
        }

        if (stopFlag) break;

        synchronized (this) {
          for (TempBan tempBan : tempBanList) {
            if (tempBan.isExpired()) tempBanList.remove(tempBan);
          }

          for (TempAdmin tempAdmin : tempAdminList) {
            if (tempAdmin.isExpired()) tempAdminList.remove(tempAdmin);
          }

          for (TempModerator tempModerator : tempModeratorList) {
            if (tempModerator.isExpired()) tempModeratorList.remove(tempModerator);
          }

          for (TempElevated tempElevated : tempElevatedList) {
            if (tempElevated.isExpired()) tempElevatedList.remove(tempElevated);
          }

          for (Silence silence : silenceList) {
            if (silence.isExpired()) silenceList.remove(silence);
          }

          for (UserAccess userAccess : userList) {
            userAccess.refreshDNS();
          }

          for (AddressAccess addressAccess : addressList) {
            addressAccess.refreshDNS();
          }
        }
      }
    } catch (Throwable e) {
      if (!stopFlag) log.fatal("AccessManager2 thread caught unexpected exception: " + e, e);
    } finally {
      isRunning = false;
      log.debug("AccessManager2 thread exiting...");
    }
  }

  private synchronized void checkReload() {
    if (accessFile != null && accessFile.lastModified() > lastLoadModifiedTime) loadAccess();
  }

  private synchronized void loadAccess() {
    if (accessFile == null) return;

    log.info("Reloading permissions...");

    lastLoadModifiedTime = accessFile.lastModified();

    userList.clear();
    gameList.clear();
    emulatorList.clear();
    addressList.clear();

    try {
      FileInputStream file = new FileInputStream(this.accessFile);
      Reader temp = new InputStreamReader(file, System.getProperty("emulinker.charset"));
      BufferedReader reader = new BufferedReader(temp);
      String line = null;
      while ((line = reader.readLine()) != null) {
        if (line.length() == 0 || line.startsWith("#") || line.startsWith("//")) continue;

        StringTokenizer st = new StringTokenizer(line, ",");
        if (st.countTokens() < 3) {
          log.error("Failed to load access line, too few tokens: " + line);
          continue;
        }

        String type = st.nextToken();

        try {
          if (type.equalsIgnoreCase("user")) userList.add(new UserAccess(st));
          else if (type.equalsIgnoreCase("game")) gameList.add(new GameAccess(st));
          else if (type.equalsIgnoreCase("emulator")) emulatorList.add(new EmulatorAccess(st));
          else if (type.equalsIgnoreCase("ipaddress")) addressList.add(new AddressAccess(st));
          else throw new Exception("Unrecognized access type: " + type);
        } catch (Exception e) {
          log.error("Failed to load access line: " + e.getMessage() + ": " + line);
        }
      }

      reader.close();
    } catch (IOException e) {
      log.error("Failed to load access file: " + e.getMessage(), e);
    }
  }

  @Override
  public void addTempBan(String addressPattern, int minutes) {
    tempBanList.add(new TempBan(addressPattern, minutes));
  }

  @Override
  public void addTempAdmin(String addressPattern, int minutes) {
    tempAdminList.add(new TempAdmin(addressPattern, minutes));
  }

  @Override
  public void addTempModerator(String addressPattern, int minutes) {
    tempModeratorList.add(new TempModerator(addressPattern, minutes));
  }

  @Override
  public void addTempElevated(String addressPattern, int minutes) {
    tempElevatedList.add(new TempElevated(addressPattern, minutes));
  }

  @Override
  public void addSilenced(String addressPattern, int minutes) {
    silenceList.add(new Silence(addressPattern, minutes));
  }

  @Override
  public synchronized String getAnnouncement(InetAddress address) {
    checkReload();
    String userAddress = address.getHostAddress();
    for (UserAccess userAccess : userList) {
      if (userAccess.matches(userAddress)) return userAccess.getMessage();
    }

    return null;
  }

  @Override
  public synchronized int getAccess(InetAddress address) {
    checkReload();

    String userAddress = address.getHostAddress();

    for (TempAdmin tempAdmin : tempAdminList) {
      if (tempAdmin.matches(userAddress) && !tempAdmin.isExpired()) return ACCESS_ADMIN;
    }

    for (TempModerator tempModerator : tempModeratorList) {
      if (tempModerator.matches(userAddress) && !tempModerator.isExpired()) return ACCESS_MODERATOR;
    }

    for (TempElevated tempElevated : tempElevatedList) {
      if (tempElevated.matches(userAddress) && !tempElevated.isExpired()) return ACCESS_ELEVATED;
    }

    for (UserAccess userAccess : userList) {
      if (userAccess.matches(userAddress)) return userAccess.getAccess();
    }

    return ACCESS_NORMAL;
  }

  @Override
  public synchronized boolean clearTemp(InetAddress address, boolean clearAll) {
    String userAddress = address.getHostAddress();
    boolean found = false;

    for (Silence silence : silenceList) {
      if (silence.matches(userAddress)) {
        silenceList.remove(silence);
        found = true;
      }
    }

    for (TempBan tempBan : tempBanList) {
      if (tempBan.matches(userAddress) && !tempBan.isExpired()) {
        tempBanList.remove(tempBan);
        found = true;
      }
    }

    if (clearAll) {
      for (TempElevated tempElevated : tempElevatedList) {
        if (tempElevated.matches(userAddress)) {
          tempElevatedList.remove(tempElevated);
          found = true;
        }
      }

      for (TempModerator tempModerator : tempModeratorList) {
        if (tempModerator.matches(userAddress)) {
          tempModeratorList.remove(tempModerator);
          found = true;
        }
      }

      for (TempAdmin tempAdmin : tempAdminList) {
        if (tempAdmin.matches(userAddress)) {
          tempAdminList.remove(tempAdmin);
          found = true;
        }
      }
    }

    return found;
  }

  @Override
  public synchronized boolean isSilenced(InetAddress address) {
    checkReload();

    String userAddress = address.getHostAddress();

    for (Silence silence : silenceList) {
      if (silence.matches(userAddress) && !silence.isExpired()) return true;
    }

    return false;
  }

  @Override
  public synchronized boolean isAddressAllowed(InetAddress address) {
    checkReload();

    String userAddress = address.getHostAddress();

    for (TempBan tempBan : tempBanList) {
      if (tempBan.matches(userAddress) && !tempBan.isExpired()) return false;
    }

    for (AddressAccess addressAccess : addressList) {
      if (addressAccess.matches(userAddress)) return addressAccess.getAccess();
    }

    return true;
  }

  @Override
  public synchronized boolean isEmulatorAllowed(String emulator) {
    checkReload();

    for (EmulatorAccess emulatorAccess : emulatorList) {
      if (emulatorAccess.matches(emulator)) return emulatorAccess.getAccess();
    }

    return true;
  }

  @Override
  public synchronized boolean isGameAllowed(String game) {
    checkReload();

    for (GameAccess gameAccess : gameList) {
      if (gameAccess.matches(game)) return gameAccess.getAccess();
    }

    return true;
  }

  protected class UserAccess {
    protected List<WildcardStringPattern> patterns;
    protected List<String> hostNames;
    protected List<String> resolvedAddresses;
    protected int access;
    protected String message;

    protected UserAccess(StringTokenizer st) throws Exception {
      if (st.countTokens() < 2 || st.countTokens() > 3)
        throw new Exception("Wrong number of tokens: " + st.countTokens());

      String accessStr = st.nextToken().toLowerCase();
      if (accessStr.equals("normal")) access = ACCESS_NORMAL;
      else if (accessStr.equals("elevated")) access = ACCESS_ELEVATED;
      else if (accessStr.equals("moderator")) access = ACCESS_MODERATOR;
      else if (accessStr.equals("admin")) access = ACCESS_ADMIN;
      else if (accessStr.equals("superadmin")) access = ACCESS_SUPERADMIN;
      else throw new AccessException("Unrecognized access token: " + accessStr);

      hostNames = new ArrayList<String>();
      resolvedAddresses = new ArrayList<String>();
      patterns = new ArrayList<WildcardStringPattern>();

      String s = st.nextToken().toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        String pat = pt.nextToken().toLowerCase();
        if (pat.startsWith("dns:")) {
          if (pat.length() <= 5) throw new AccessException("Malformatted DNS entry: " + s);
          String hostName = pat.substring(4);
          try {
            InetAddress a = InetAddress.getByName(hostName);
            log.debug("Resolved " + hostName + " to " + a.getHostAddress());
          } catch (Exception e) {
            log.warn(
                "Failed to resolve DNS entry to an address: " + hostName + ": " + e.getMessage());
          }
          hostNames.add(pat.substring(4));
        } else {
          patterns.add(new WildcardStringPattern(pat));
        }
      }

      refreshDNS();

      if (st.hasMoreTokens()) message = st.nextToken();
    }

    protected synchronized void refreshDNS() {
      resolvedAddresses.clear();
      for (String hostName : hostNames) {
        try {
          InetAddress address = InetAddress.getByName(hostName);
          resolvedAddresses.add(address.getHostAddress());
        } catch (Exception e) {
          log.debug("Failed to resolve DNS entry to an address: " + hostName, e);
        }
      }
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected int getAccess() {
      return access;
    }

    protected String getMessage() {
      return message;
    }

    protected synchronized boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }

      for (String resolvedAddress : resolvedAddresses) {
        if (resolvedAddress.equals(address)) return true;
      }

      return false;
    }
  }

  protected class AddressAccess {
    protected List<WildcardStringPattern> patterns;
    protected List<String> hostNames;
    protected List<String> resolvedAddresses;
    protected boolean access;

    protected AddressAccess(StringTokenizer st) throws Exception {
      if (st.countTokens() != 2) throw new Exception("Wrong number of tokens: " + st.countTokens());

      String accessStr = st.nextToken().toLowerCase();
      if (accessStr.equals("allow")) access = true;
      else if (accessStr.equals("deny")) access = false;
      else throw new AccessException("Unrecognized access token: " + accessStr);

      hostNames = new ArrayList<String>();
      resolvedAddresses = new ArrayList<String>();
      patterns = new ArrayList<WildcardStringPattern>();

      String s = st.nextToken().toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        String pat = pt.nextToken().toLowerCase();
        if (pat.startsWith("dns:")) {
          if (pat.length() <= 5) throw new AccessException("Malformatted DNS entry: " + s);
          String hostName = pat.substring(4);
          try {
            InetAddress a = InetAddress.getByName(hostName);
            log.debug("Resolved " + hostName + " to " + a.getHostAddress());
          } catch (Exception e) {
            log.warn(
                "Failed to resolve DNS entry to an address: " + hostName + ": " + e.getMessage());
          }
          hostNames.add(pat.substring(4));
        } else {
          patterns.add(new WildcardStringPattern(pat));
        }
      }

      refreshDNS();
    }

    protected synchronized void refreshDNS() {
      resolvedAddresses.clear();
      for (String hostName : hostNames) {
        try {
          InetAddress address = InetAddress.getByName(hostName);
          resolvedAddresses.add(address.getHostAddress());
        } catch (Exception e) {
          log.debug("Failed to resolve DNS entry to an address: " + hostName, e);
        }
      }
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected boolean getAccess() {
      return access;
    }

    protected synchronized boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }

      for (String resolvedAddress : resolvedAddresses) {
        if (resolvedAddress.equals(address)) return true;
      }

      return false;
    }
  }

  protected class EmulatorAccess {
    protected List<WildcardStringPattern> patterns;
    protected boolean access;

    protected EmulatorAccess(StringTokenizer st) throws Exception {
      if (st.countTokens() != 2) throw new Exception("Wrong number of tokens: " + st.countTokens());

      String accessStr = st.nextToken().toLowerCase();
      if (accessStr.equals("allow")) access = true;
      else if (accessStr.equals("deny")) access = false;
      else throw new AccessException("Unrecognized access token: " + accessStr);

      patterns = new ArrayList<WildcardStringPattern>();
      String s = st.nextToken().toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected boolean getAccess() {
      return access;
    }

    protected boolean matches(String emulator) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(emulator.toLowerCase())) return true;
      }
      return false;
    }
  }

  protected class GameAccess {
    protected List<WildcardStringPattern> patterns;
    protected boolean access;

    protected GameAccess(StringTokenizer st) throws Exception {
      if (st.countTokens() != 2) throw new Exception("Wrong number of tokens: " + st.countTokens());

      String accessStr = st.nextToken().toLowerCase();
      if (accessStr.equals("allow")) access = true;
      else if (accessStr.equals("deny")) access = false;
      else throw new AccessException("Unrecognized access token: " + accessStr);

      patterns = new ArrayList<WildcardStringPattern>();
      String s = st.nextToken().toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected boolean getAccess() {
      return access;
    }

    protected boolean matches(String game) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(game.toLowerCase())) return true;
      }
      return false;
    }
  }

  protected class TempBan {
    protected List<WildcardStringPattern> patterns;
    protected long startTime;
    protected int minutes;

    protected TempBan(String accessStr, int minutes) {
      patterns = new ArrayList<WildcardStringPattern>();
      String s = accessStr.toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }

      this.minutes = minutes;
      startTime = System.currentTimeMillis();
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected long getStartTime() {
      return startTime;
    }

    protected int getMinutes() {
      return minutes;
    }

    protected boolean isExpired() {
      if (System.currentTimeMillis() > (startTime + (minutes * 60000))) return true;
      return false;
    }

    protected boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }
      return false;
    }
  }

  protected class TempAdmin {
    protected List<WildcardStringPattern> patterns;
    protected long startTime;
    protected int minutes;

    protected TempAdmin(String accessStr, int minutes) {
      patterns = new ArrayList<WildcardStringPattern>();
      String s = accessStr.toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }

      this.minutes = minutes;
      startTime = System.currentTimeMillis();
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected long getStartTime() {
      return startTime;
    }

    protected int getMinutes() {
      return minutes;
    }

    protected boolean isExpired() {
      if (System.currentTimeMillis() > (startTime + (minutes * 60000))) return true;
      return false;
    }

    protected boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }
      return false;
    }
  }

  protected class TempModerator {
    protected List<WildcardStringPattern> patterns;
    protected long startTime;
    protected int minutes;

    protected TempModerator(String accessStr, int minutes) {
      patterns = new ArrayList<WildcardStringPattern>();
      String s = accessStr.toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }

      this.minutes = minutes;
      startTime = System.currentTimeMillis();
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected long getStartTime() {
      return startTime;
    }

    protected int getMinutes() {
      return minutes;
    }

    protected boolean isExpired() {
      if (System.currentTimeMillis() > (startTime + (minutes * 60000))) return true;
      return false;
    }

    protected boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }
      return false;
    }
  }

  protected class TempElevated {
    protected List<WildcardStringPattern> patterns;
    protected long startTime;
    protected int minutes;

    protected TempElevated(String accessStr, int minutes) {
      patterns = new ArrayList<WildcardStringPattern>();
      String s = accessStr.toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }

      this.minutes = minutes;
      startTime = System.currentTimeMillis();
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected long getStartTime() {
      return startTime;
    }

    protected int getMinutes() {
      return minutes;
    }

    protected boolean isExpired() {
      if (System.currentTimeMillis() > (startTime + (minutes * 60000))) return true;
      return false;
    }

    protected boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }
      return false;
    }
  }

  protected class Silence {
    protected List<WildcardStringPattern> patterns;
    protected long startTime;
    protected int minutes;

    protected Silence(String accessStr, int minutes) {
      patterns = new ArrayList<WildcardStringPattern>();
      String s = accessStr.toLowerCase();
      StringTokenizer pt = new StringTokenizer(s, "|");
      while (pt.hasMoreTokens()) {
        patterns.add(new WildcardStringPattern(pt.nextToken().toLowerCase()));
      }

      this.minutes = minutes;
      startTime = System.currentTimeMillis();
    }

    protected List<WildcardStringPattern> getPatterns() {
      return patterns;
    }

    protected long getStartTime() {
      return startTime;
    }

    protected int getMinutes() {
      return minutes;
    }

    protected boolean isExpired() {
      if (System.currentTimeMillis() > (startTime + (minutes * 60000))) return true;
      return false;
    }

    protected boolean matches(String address) {
      for (WildcardStringPattern pattern : patterns) {
        if (pattern.match(address)) return true;
      }
      return false;
    }
  }
}

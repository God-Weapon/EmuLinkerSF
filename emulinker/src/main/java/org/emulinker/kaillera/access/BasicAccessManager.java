/*
 package org.emulinker.kaillera.access;

 import java.net.InetAddress;
 import java.util.*;

 import org.apache.commons.configuration.*;
 import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
 import org.apache.commons.logging.*;

 import org.emulinker.kaillera.model.KailleraUser;
 import org.emulinker.util.EmuLinkerXMLConfig;

 public class BasicAccessManager implements AccessManager
 {
 private static Log			log				= LogFactory.getLog(BasicAccessManager.class);

 private String				accessFileName;
 private XMLConfiguration	config;
 private Map<String, Admin>	admins			= new HashMap<String, Admin>();
 private List<String>		bannedAddresses	= new ArrayList<String>();
 private List<String>		bannedNames		= new ArrayList<String>();

 public BasicAccessManager(Configuration config) throws NoSuchElementException
 {
 accessFileName = config.getString("accessManager.basic.fileName");

 try
 {
 this.config = new XMLConfiguration(EmuLinkerXMLConfig.class.getResource("/" + accessFileName));
 this.config.setReloadingStrategy(new AccessManagerReloader());
 }
 catch (ConfigurationException e)
 {
 log.error("AccessManager failed to load " + accessFileName + ": " + e.getMessage());
 return;
 }
 }

 private synchronized void loadAccess()
 {
 if (config == null)
 return;

 log.info("BasicAccessManager reloading permissions...");

 bannedNames = config.getList("banned.names.name");
 for (String name : bannedNames)
 log.info("The name \"" + name + "\" will be banned");

 bannedAddresses = config.getList("banned.addresses.address");
 for (String address : bannedAddresses)
 log.info("The address \"" + address + "\" will be banned");

 admins.clear();

 Iterator iter = config.getKeys("admins");
 while (iter.hasNext())
 {
 String entry = (String) iter.next();
 Scanner scanner = new Scanner(entry);
 scanner.useDelimiter("\\.");

 scanner.next();
 String adminName = scanner.next();

 if (admins.containsKey(adminName))
 continue;

 List<String> names = config.getList("admins." + adminName + ".names.name");
 List<String> addresses = config.getList("admins." + adminName + ".addresses.address");

 Admin admin = new Admin(adminName, names, addresses);
 log.info("Loading admin access: " + admin);
 admins.put(adminName, admin);
 }
 }

 public synchronized boolean isBanned(InetAddress remoteAddress)
 {
 // this won't actually reload the config unless it's been changed... this will trigger our loadAccess if there's a change
 if (config != null)
 config.reload();

 String hostAddress = remoteAddress.getHostAddress();
 for (String banned : bannedAddresses)
 {
 if (hostAddress.startsWith(banned))
 return true;
 }

 return false;
 }

 public synchronized boolean isBanned(String userName)
 {
 // this won't actually reload the config unless it's been changed... this will trigger our loadAccess if there's a change
 if (config != null)
 config.reload();

 String nameLower = userName.toLowerCase();
 for (String banned : bannedNames)
 {
 if (nameLower.startsWith(banned.toLowerCase()))
 return true;
 }

 return false;
 }

 public synchronized boolean isBanned(KailleraUser user)
 {
 return isBanned(user.getConnectSocketAddress().getAddress());
 }

 public synchronized boolean isAdmin(InetAddress remoteAddress)
 {
 // this won't actually reload the config unless it's been changed... this will trigger our loadAccess if there's a change
 if (config != null)
 config.reload();

 for (Admin admin : admins.values())
 {
 if (admin.checkAddress(remoteAddress))
 return true;
 }

 return false;
 }

 public synchronized boolean isAdmin(KailleraUser user)
 {
 return isAdmin(user.getConnectSocketAddress().getAddress());
 }

 public synchronized int getAccess(InetAddress remoteAddress, String userName) throws AccessException
 {
 // this won't actually reload the config unless it's been changed... this will trigger our loadAccess if there's a change
 if (config != null)
 config.reload();

 if (isBanned(remoteAddress))
 return AccessManager.DENIED;

 if (isBanned(userName))
 return AccessManager.DENIED;

 for (Admin admin : admins.values())
 {
 int access = admin.checkMatch(remoteAddress, userName);
 if (access == AccessManager.ADMIN)
 return AccessManager.ADMIN;
 else if (access == AccessManager.DENIED)
 return AccessManager.DENIED;
 }

 return AccessManager.USER;
 }

 private class Admin
 {
 private String			name;
 private List<String>	names;
 private List<String>	addresses;

 private Admin(String name, List<String> names, List<String> addresses)
 {
 this.name = name;
 this.names = names;
 this.addresses = addresses;
 }

 public String toString()
 {
 // this is very inefficient
 //			return "BasicAccessManager.Admin[name=" + name + " loginNames=" + Arrays.toString(names.toArray()) + " addresses=" + Arrays.toString(addresses.toArray()) + "]";
 return name;
 }

 private String getName()
 {
 return name;
 }

 private boolean checkName(String userName)
 {
 String nameLower = userName.toLowerCase();
 for (String adminName : names)
 {
 if (nameLower.startsWith(adminName.toLowerCase()))
 return true;
 }

 return false;
 }

 private boolean checkAddress(InetAddress remoteAddress)
 {
 String hostAddress = remoteAddress.getHostAddress();
 for (String adminAddress : addresses)
 {
 if (hostAddress.startsWith(adminAddress))
 return true;
 }

 return false;
 }

 private int checkMatch(InetAddress remoteAddress, String userName) throws AccessException
 {
 boolean nameMatches = checkName(userName);
 boolean addressMatches = checkAddress(remoteAddress);

 if (addressMatches)
 return AccessManager.ADMIN;
 else if (nameMatches && !addressMatches)
 throw new AccessException("Restricted UserName: " + userName);
 else
 return AccessManager.USER;
 }
 }

 private class AccessManagerReloader extends FileChangedReloadingStrategy
 {
 private AccessManagerReloader()
 {
 super();
 }

 public void init()
 {
 super.init();
 loadAccess();
 }

 public void reloadingPerformed()
 {
 super.reloadingPerformed();
 loadAccess();
 }

 public boolean reloadingRequired()
 {
 boolean reloadingRequired = super.reloadingRequired();
 if (reloadingRequired)
 config.clear();
 return reloadingRequired;
 }
 }
 }
 */

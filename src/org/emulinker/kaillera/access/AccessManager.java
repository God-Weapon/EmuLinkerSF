package org.emulinker.kaillera.access;

import java.net.InetAddress;

/**
 * An AccessManager is used retrieve, check, and store user access levels and permissions, and game and emulator 
 * filters.  This interface defines the static access levels and methods that an AccessManager must implement.  How the 
 * access permissions are stored, checked, and manipulated is left to the implementation class.<br>
 * <br>
 * Most of the main EmuLinker components are passed a handle to the current AccessManager and make calls to it upon 
 * user interactions.<br>
 * <br>
 * AccessManager is a top-level EmuLinker component; it's implementation class is loaded via PicoContainer upon startup.
 * 
 * @author Paul Cowan
 * @see www.emulinker.org
 */
public interface AccessManager
{
	public static int		ACCESS_BANNED	    = 0;
	public static int		ACCESS_NORMAL	    = 1;
	public static int		ACCESS_ELEVATED	    = 2;
	public static int		ACCESS_MODERATOR    = 3;
	public static int		ACCESS_ADMIN	    = 4;
	public static int		ACCESS_SUPERADMIN	= 5;

	public static String[]	ACCESS_NAMES	= { "Banned", "Normal", "Elevated", "Moderator", "Admin", "SuperAdmin"};

	/**
	 * Checks if address is allowed to connect.
	 * 
	 * @param	address		IP Address of client
	 * @return				true if address is allowed to connect
	 */
	public boolean isAddressAllowed(InetAddress address);

	/**
	 * Checks if address is silenced
	 * 
	 * @param	address		IP Address of client
	 * @return				true if address is silenced
	 */
	public boolean isSilenced(InetAddress address);

	/**
	 * Checks if client's emulator is allowed (not filtered)
	 * 
	 * @param	emulator	Emulator name of client
	 * @return				true if emulator is allowed
	 */
	public boolean isEmulatorAllowed(String emulator);

	/**
	 * Checks if client's game (ROM) is allowed (not filtered)
	 * 
	 * @param	game		Game name of client
	 * @return				true if game is allowed
	 */
	public boolean isGameAllowed(String game);

	/**
	 * Returns the client's assigned access level
	 * 
	 * @param	address		IP Address of client
	 * @return	The access level or the default access level if not found
	 */
	public int getAccess(InetAddress address);

	/**
	 * Returns a login announcement string
	 * 
	 * @param	address	IP Address of client
	 * @return	The login announcement, null if not defined
	 */
	public String getAnnouncement(InetAddress address);

	/**
	 * Temporairly adds a user to the nanned list using a pattern algorythm defined by the AccessManager implementation.
	 * While active, <code>isAddressAllowed</code> should return false, and <code>getAccess</code> should return 
	 * <code>ACCESS_BANNED</code>.
	 * 
	 * @param	pattern		A pattern to match to an address
	 * @param	minutes		Number of minutes this ban is valid from the time of addition
	 */
	public void addTempBan(String pattern, int minutes);

	/**
	 * Temporairly adds a user to the admin list using a pattern algorythm defined by the AccessManager implementation.  
	 * While active, <code>getAccess</code> should return <code>ACCESS_ADMIN</code>.
	 * 
	 * @param	pattern		A pattern to match to an address
	 * @param	minutes		Number of minutes this grant is valid from the time of addition
	 */
	public void addTempAdmin(String pattern, int minutes);
	
	public void addTempElevated(String pattern, int minutes);

	/**
	 * Temporairly adds a user to the silenced list using a pattern algorythm defined by the AccessManager 
	 * implementation.  While active, <code>isSilenced</code> should return <code>true</code>.
	 * 
	 * @param	pattern		A pattern to match to an address
	 * @param	minutes		Number of minutes this grant is valid from the time of addition
	 */
	public void addSilenced(String pattern, int minutes);
	
	public boolean clearTemp(InetAddress address);
}

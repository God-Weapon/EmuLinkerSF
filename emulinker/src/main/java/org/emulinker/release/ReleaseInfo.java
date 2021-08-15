package org.emulinker.release;

/**
 * Provides release information about the project.
 * 
 * ReleaseInfo is a top-level EmuLinker component; it's implementation class is loaded via PicoContainer upon startup.
 * 
 * @author Paul Cowan
 * @see www.emulinker.org
 */
public interface ReleaseInfo
{
	/**
	 * @return	The name of this software.
	 */
	public String getProductName();

	/**
	 * @return	Major version number
	 */
	public int getMajorVersion();

	/**
	 * @return	Minor version number
	 */
	public int getMinorVersion();

	/**
	 * @return	Build number
	 */
	public int getBuildNumber();

	/**
	 * @return	The release date of this software
	 */
	public String getReleaseDate();

	/**
	 * @return A string containing the full version information
	 */
	public String getVersionString();

	/**
	 * @return License information
	 */
	public String getLicenseInfo();

	/**
	 * @return A string containing software website iformation
	 */
	public String getWebsiteString();

	/**
	 * @return A string containg a welcome message intended to be display on software startup 
	 */
	public String getWelcome();
}

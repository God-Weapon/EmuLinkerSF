package org.emulinker.kaillera.access

import java.net.InetAddress

/**
 * An AccessManager is used retrieve, check, and store user access levels and permissions, and game
 * and emulator filters. This interface defines the static access levels and methods that an
 * AccessManager must implement. How the access permissions are stored, checked, and manipulated is
 * left to the implementation class.<br></br> <br></br> Most of the main EmuLinker components are
 * passed a handle to the current AccessManager and make calls to it upon user interactions.
 *
 * @author Paul Cowan
 * @see www.emulinker.org
 */
interface AccessManager {
  /**
   * Checks if address is allowed to connect.
   *
   * @param address IP Address of client
   * @return true if address is allowed to connect
   */
  fun isAddressAllowed(address: InetAddress?): Boolean

  /**
   * Checks if address is silenced
   *
   * @param address IP Address of client
   * @return true if address is silenced
   */
  fun isSilenced(address: InetAddress?): Boolean

  /**
   * Checks if client's emulator is allowed (not filtered)
   *
   * @param emulator Emulator name of client
   * @return true if emulator is allowed
   */
  fun isEmulatorAllowed(emulator: String?): Boolean

  /**
   * Checks if client's game (ROM) is allowed (not filtered)
   *
   * @param game Game name of client
   * @return true if game is allowed
   */
  fun isGameAllowed(game: String?): Boolean

  /**
   * Returns the client's assigned access level
   *
   * @param address IP Address of client
   * @return The access level or the default access level if not found
   */
  fun getAccess(address: InetAddress?): Int

  /**
   * Returns a login announcement string
   *
   * @param address IP Address of client
   * @return The login announcement, null if not defined
   */
  fun getAnnouncement(address: InetAddress?): String?

  /**
   * Temporairly adds a user to the nanned list using a pattern algorythm defined by the
   * AccessManager implementation. While active, `isAddressAllowed` should return false, and
   * `getAccess` should return `ACCESS_BANNED`.
   *
   * @param pattern A pattern to match to an address
   * @param minutes Number of minutes this ban is valid from the time of addition
   */
  fun addTempBan(pattern: String?, minutes: Int)

  /**
   * Temporairly adds a user to the admin list using a pattern algorythm defined by the
   * AccessManager implementation. While active, `getAccess` should return ` ACCESS_ADMIN`.
   *
   * @param pattern A pattern to match to an address
   * @param minutes Number of minutes this grant is valid from the time of addition
   */
  fun addTempAdmin(pattern: String?, minutes: Int)
  fun addTempModerator(pattern: String?, minutes: Int)
  fun addTempElevated(pattern: String?, minutes: Int)

  /**
   * Temporairly adds a user to the silenced list using a pattern algorythm defined by the
   * AccessManager implementation. While active, `isSilenced` should return `true ` * .
   *
   * @param pattern A pattern to match to an address
   * @param minutes Number of minutes this grant is valid from the time of addition
   */
  fun addSilenced(pattern: String?, minutes: Int)
  fun clearTemp(address: InetAddress?, clearAll: Boolean): Boolean

  companion object {
    const val ACCESS_BANNED = 0
    const val ACCESS_NORMAL = 1
    const val ACCESS_ELEVATED = 2
    const val ACCESS_MODERATOR = 3
    const val ACCESS_ADMIN = 4
    const val ACCESS_SUPERADMIN = 5

    val ACCESS_NAMES = arrayOf("Banned", "Normal", "Elevated", "Moderator", "Admin", "SuperAdmin")
  }
}

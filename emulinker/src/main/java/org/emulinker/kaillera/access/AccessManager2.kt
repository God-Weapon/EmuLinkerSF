package org.emulinker.kaillera.access

import com.google.common.flogger.FluentLogger
import java.io.*
import java.net.InetAddress
import java.net.URISyntaxException
import java.security.Security
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import org.emulinker.config.RuntimeFlags
import org.emulinker.util.WildcardStringPattern

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class AccessManager2 @Inject internal constructor(private val flags: RuntimeFlags) : AccessManager {
  companion object {
    init {
      Security.setProperty("networkaddress.cache.ttl", "60")
      Security.setProperty("networkaddress.cache.negative.ttl", "60")
    }
  }

  private val scope = CoroutineScope(Dispatchers.IO)

  private var stopFlag = false
  private var accessFile: File?
  private var lastLoadModifiedTime: Long = -1
  private val userList: MutableList<UserAccess> = CopyOnWriteArrayList()
  private val gameList: MutableList<GameAccess> = CopyOnWriteArrayList()
  private val emulatorList: MutableList<EmulatorAccess> = CopyOnWriteArrayList()
  private val addressList: MutableList<AddressAccess> = CopyOnWriteArrayList()
  private val tempBanList: MutableList<TempBan> = CopyOnWriteArrayList()
  private val tempAdminList: MutableList<TempAdmin> = CopyOnWriteArrayList()
  private val tempModeratorList: MutableList<TempModerator> = CopyOnWriteArrayList()
  private val tempElevatedList: MutableList<TempElevated> = CopyOnWriteArrayList()
  private val silenceList: MutableList<Silence> = CopyOnWriteArrayList()

  @Synchronized
  private fun checkReload() {
    if (accessFile != null && accessFile!!.lastModified() > lastLoadModifiedTime) loadAccess()
  }

  @Synchronized
  private fun loadAccess() {
    if (accessFile == null) return
    logger.atInfo().log("Reloading permissions...")
    lastLoadModifiedTime = accessFile!!.lastModified()
    userList.clear()
    gameList.clear()
    emulatorList.clear()
    addressList.clear()
    try {
      val file = FileInputStream(accessFile!!)
      val temp: Reader = InputStreamReader(file, flags.charset)
      val reader = BufferedReader(temp)
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        if (line.isNullOrBlank() || line!!.startsWith("#") || line!!.startsWith("//")) continue
        val st = StringTokenizer(line, ",")
        if (st.countTokens() < 3) {
          logger.atSevere().log("Failed to load access line, too few tokens: $line")
          continue
        }
        val type = st.nextToken()
        try {
          if (type.equals("user", ignoreCase = true)) userList.add(UserAccess(st))
          else if (type.equals("game", ignoreCase = true)) gameList.add(GameAccess(st))
          else if (type.equals("emulator", ignoreCase = true)) emulatorList.add(EmulatorAccess(st))
          else if (type.equals("ipaddress", ignoreCase = true)) addressList.add(AddressAccess(st))
          else throw Exception("Unrecognized access type: $type")
        } catch (e: Exception) {
          logger.atSevere().withCause(e).log("Failed to load access line: $line")
        }
      }
      reader.close()
    } catch (e: IOException) {
      logger.atSevere().withCause(e).log("Failed to load access file")
    }
  }

  override fun addTempBan(addressPattern: String, duration: Duration) {
    addTemporaryAttributeToList(tempBanList, TempBan(addressPattern, duration))
  }

  override fun addTempAdmin(addressPattern: String, duration: Duration) {
    addTemporaryAttributeToList(tempAdminList, TempAdmin(addressPattern, duration))
  }

  override fun addTempModerator(addressPattern: String, duration: Duration) {
    addTemporaryAttributeToList(tempModeratorList, TempModerator(addressPattern, duration))
  }

  override fun addTempElevated(addressPattern: String, duration: Duration) {
    addTemporaryAttributeToList(tempElevatedList, TempElevated(addressPattern, duration))
  }

  override fun addSilenced(addressPattern: String, duration: Duration) {
    addTemporaryAttributeToList(silenceList, Silence(addressPattern, duration))
  }

  private fun <T : TemporaryAttribute> addTemporaryAttributeToList(
      list: MutableList<T>, attribute: T
  ) {
    list.add(attribute)
    scope.launch {
      delay(attribute.duration)
      list.remove(attribute)
    }
  }

  @Synchronized
  override fun getAnnouncement(address: InetAddress?): String? {
    checkReload()
    val userAddress = address!!.hostAddress
    return userList.firstOrNull { it.matches(userAddress) }?.message
  }

  @Synchronized
  override fun getAccess(address: InetAddress?): Int {
    checkReload()
    val userAddress = address!!.hostAddress
    for (tempAdmin in tempAdminList) {
      if (tempAdmin.matches(userAddress) && !tempAdmin.isExpired) return AccessManager.ACCESS_ADMIN
    }
    for (tempModerator in tempModeratorList) {
      if (tempModerator.matches(userAddress) && !tempModerator.isExpired)
          return AccessManager.ACCESS_MODERATOR
    }
    for (tempElevated in tempElevatedList) {
      if (tempElevated.matches(userAddress) && !tempElevated.isExpired)
          return AccessManager.ACCESS_ELEVATED
    }
    return userList.firstOrNull { it.matches(userAddress) }?.access ?: AccessManager.ACCESS_NORMAL
  }

  @Synchronized
  override fun clearTemp(address: InetAddress?, clearAll: Boolean): Boolean {
    val userAddress = address!!.hostAddress
    var found = false
    for (silence in silenceList) {
      if (silence.matches(userAddress)) {
        silenceList.remove(silence)
        found = true
      }
    }
    for (tempBan in tempBanList) {
      if (tempBan.matches(userAddress) && !tempBan.isExpired) {
        tempBanList.remove(tempBan)
        found = true
      }
    }
    if (clearAll) {
      for (tempElevated in tempElevatedList) {
        if (tempElevated.matches(userAddress)) {
          tempElevatedList.remove(tempElevated)
          found = true
        }
      }
      for (tempModerator in tempModeratorList) {
        if (tempModerator.matches(userAddress)) {
          tempModeratorList.remove(tempModerator)
          found = true
        }
      }
      for (tempAdmin in tempAdminList) {
        if (tempAdmin.matches(userAddress)) {
          tempAdminList.remove(tempAdmin)
          found = true
        }
      }
    }
    return found
  }

  @Synchronized
  override fun isSilenced(address: InetAddress?): Boolean {
    checkReload()
    val userAddress = address!!.hostAddress
    for (silence in silenceList) {
      if (silence.matches(userAddress) && !silence.isExpired) return true
    }
    return false
  }

  @Synchronized
  override fun isAddressAllowed(address: InetAddress?): Boolean {
    checkReload()
    val userAddress = address!!.hostAddress
    for (tempBan in tempBanList) {
      if (tempBan.matches(userAddress) && !tempBan.isExpired) return false
    }
    for (addressAccess in addressList) {
      if (addressAccess.matches(userAddress)) return addressAccess.access
    }
    return true
  }

  @Synchronized
  override fun isEmulatorAllowed(emulator: String?): Boolean {
    checkReload()
    for (emulatorAccess in emulatorList) {
      if (emulatorAccess.matches(emulator)) return emulatorAccess.access
    }
    return true
  }

  @Synchronized
  override fun isGameAllowed(game: String?): Boolean {
    checkReload()
    return gameList.firstOrNull { it.matches(game) }?.access ?: true
  }

  private class UserAccess(st: StringTokenizer) {
    private var hostNames: MutableList<String>
    private var resolvedAddresses: MutableList<String>
    var access = 0
      private set
    var message: String? = null
      private set

    @Synchronized
    fun refreshDNS() {
      resolvedAddresses.clear()
      hostNames.forEach { hostName ->
        try {
          val address = InetAddress.getByName(hostName)
          resolvedAddresses.add(address.hostAddress)
        } catch (e: Exception) {
          logger.atFine().withCause(e).log("Failed to resolve DNS entry to an address: $hostName")
        }
      }
    }

    private var patterns: MutableList<WildcardStringPattern>

    @Synchronized
    fun matches(address: String): Boolean {
      return patterns.any { it.match(address) } || resolvedAddresses.any { it == address }
    }

    init {
      if (st.countTokens() < 2 || st.countTokens() > 3)
          throw Exception("Wrong number of tokens: " + st.countTokens())
      val accessStr = st.nextToken().lowercase(Locale.getDefault())
      access =
          when (accessStr) {
            "normal" -> AccessManager.ACCESS_NORMAL
            "elevated" -> AccessManager.ACCESS_ELEVATED
            "moderator" -> AccessManager.ACCESS_MODERATOR
            "admin" -> AccessManager.ACCESS_ADMIN
            "superadmin" -> AccessManager.ACCESS_SUPERADMIN
            else -> throw AccessException("Unrecognized access token: $accessStr")
          }
      hostNames = ArrayList()
      resolvedAddresses = ArrayList()
      patterns = ArrayList()
      val s = st.nextToken().lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        val pat = pt.nextToken().lowercase(Locale.getDefault())
        if (pat.startsWith("dns:")) {
          if (pat.length <= 5) throw AccessException("Malformatted DNS entry: $s")
          val hostName = pat.substring(4)
          try {
            val a = InetAddress.getByName(hostName)
            logger.atFine().log("Resolved " + hostName + " to " + a.hostAddress)
          } catch (e: Exception) {
            logger
                .atWarning()
                .withCause(e)
                .log("Failed to resolve DNS entry to an address: $hostName")
          }
          hostNames.add(pat.substring(4))
        } else {
          patterns.add(WildcardStringPattern(pat))
        }
      }
      refreshDNS()
      if (st.hasMoreTokens()) message = st.nextToken()
    }
  }

  private class AddressAccess(st: StringTokenizer) {
    private var hostNames: MutableList<String>
    private var resolvedAddresses: MutableList<String>
    var access = false
      private set

    @Synchronized
    fun refreshDNS() {
      resolvedAddresses.clear()
      for (hostName in hostNames) {
        try {
          val address = InetAddress.getByName(hostName)
          resolvedAddresses.add(address.hostAddress)
        } catch (e: Exception) {
          logger.atFine().withCause(e).log("Failed to resolve DNS entry to an address: $hostName")
        }
      }
    }

    private var patterns: MutableList<WildcardStringPattern>

    @Synchronized
    fun matches(address: String): Boolean {
      for (pattern in patterns) {
        if (pattern.match(address)) return true
      }
      for (resolvedAddress in resolvedAddresses) {
        if (resolvedAddress == address) return true
      }
      return false
    }

    init {
      if (st.countTokens() != 2) throw Exception("Wrong number of tokens: " + st.countTokens())
      val accessStr = st.nextToken().lowercase(Locale.getDefault())
      access =
          when (accessStr) {
            "allow" -> true
            "deny" -> false
            else -> throw AccessException("Unrecognized access token: $accessStr")
          }
      hostNames = ArrayList()
      resolvedAddresses = ArrayList()
      patterns = ArrayList()
      val s = st.nextToken().lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        val pat = pt.nextToken().lowercase(Locale.getDefault())
        if (pat.startsWith("dns:")) {
          if (pat.length <= 5) throw AccessException("Malformatted DNS entry: $s")
          val hostName = pat.substring(4)
          try {
            val a = InetAddress.getByName(hostName)
            logger.atFine().log("Resolved " + hostName + " to " + a.hostAddress)
          } catch (e: Exception) {
            logger
                .atWarning()
                .withCause(e)
                .log("Failed to resolve DNS entry to an address: $hostName")
          }
          hostNames.add(pat.substring(4))
        } else {
          patterns.add(WildcardStringPattern(pat))
        }
      }
      refreshDNS()
    }
  }

  private class EmulatorAccess(st: StringTokenizer) {
    var access = false
      private set

    private var patterns: MutableList<WildcardStringPattern>

    fun matches(emulator: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(emulator!!.lowercase(Locale.getDefault()))) return true
      }
      return false
    }

    init {
      if (st.countTokens() != 2) throw Exception("Wrong number of tokens: " + st.countTokens())
      val accessStr = st.nextToken().lowercase(Locale.getDefault())
      access =
          when (accessStr) {
            "allow" -> true
            "deny" -> false
            else -> throw AccessException("Unrecognized access token: $accessStr")
          }
      patterns = ArrayList()
      val s = st.nextToken().lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
    }
  }

  private class GameAccess(st: StringTokenizer) {
    var access = false
      private set

    private var patterns: MutableList<WildcardStringPattern>

    fun matches(game: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(game!!.lowercase(Locale.getDefault()))) return true
      }
      return false
    }

    init {
      if (st.countTokens() != 2) throw Exception("Wrong number of tokens: " + st.countTokens())
      val accessStr = st.nextToken().lowercase(Locale.getDefault())
      access =
          when (accessStr) {
            "allow" -> true
            "deny" -> false
            else -> throw AccessException("Unrecognized access token: $accessStr")
          }
      patterns = ArrayList()
      val s = st.nextToken().lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
    }
  }

  init {
    val url = AccessManager2::class.java.getResource("/access.cfg")
    requireNotNull(url) { "Resource not found: /access.conf" }
    accessFile =
        try {
          File(url.toURI())
        } catch (e: URISyntaxException) {
          throw IllegalStateException("Could not parse URI", e)
        }
    if (!accessFile!!.exists()) {
      throw IllegalStateException(FileNotFoundException("Resource not found: /access.conf"))
    }
    if (!accessFile!!.canRead()) {
      throw IllegalStateException(FileNotFoundException("Can not read: /access.conf"))
    }
    loadAccess()

    scope.launch {
      while (true) {
        delay(1.minutes)
        logger.atFine().log("Refreshing DNS for all users and addresses")
        userList.forEach { it.refreshDNS() }
        addressList.forEach { it.refreshDNS() }
      }
    }
  }
}

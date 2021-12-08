package org.emulinker.kaillera.access

import com.google.common.base.Preconditions.checkArgument
import com.google.common.base.Strings
import com.google.common.flogger.FluentLogger
import java.io.*
import java.net.InetAddress
import java.net.URISyntaxException
import java.security.Security
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Inject
import javax.inject.Singleton
import org.emulinker.config.RuntimeFlags
import org.emulinker.util.WildcardStringPattern

private val logger = FluentLogger.forEnclosingClass()

@Singleton
class AccessManager2
    @Inject
    internal constructor(
        private val threadPool: ThreadPoolExecutor, private val flags: RuntimeFlags
    ) : AccessManager, Runnable {
  companion object {
    init {
      Security.setProperty("networkaddress.cache.ttl", "60")
      Security.setProperty("networkaddress.cache.negative.ttl", "60")
    }
  }

  var isRunning = false
    private set
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
  fun start() {
    logger.atFine().log("AccessManager2 thread received start request!")
    logger
        .atFine()
        .log(
            "AccessManager2 thread starting (ThreadPool:${threadPool.activeCount}/${threadPool.poolSize})")
    threadPool.execute(this)
    Thread.yield()
  }

  @Synchronized
  fun stop() {
    logger.atFine().log("AccessManager2 thread received stop request!")
    if (!isRunning) {
      logger.atFine().log("KailleraServer thread stop request ignored: not running!")
      return
    }
    stopFlag = true
    userList.clear()
    gameList.clear()
    emulatorList.clear()
    addressList.clear()
    tempBanList.clear()
    tempElevatedList.clear()
    tempAdminList.clear()
    silenceList.clear()
  }

  override fun run() {
    isRunning = true
    logger.atFine().log("AccessManager2 thread running...")
    try {
      while (!stopFlag) {
        try {
          Thread.sleep((60 * 1000).toLong())
        } catch (e: InterruptedException) {
          logger.atSevere().withCause(e).log("Sleep Interrupted!")
        }
        if (stopFlag) break
        synchronized(this) {
          for (tempBan in tempBanList) {
            if (tempBan.isExpired) tempBanList.remove(tempBan)
          }
          for (tempAdmin in tempAdminList) {
            if (tempAdmin.isExpired) tempAdminList.remove(tempAdmin)
          }
          for (tempModerator in tempModeratorList) {
            if (tempModerator.isExpired) tempModeratorList.remove(tempModerator)
          }
          for (tempElevated in tempElevatedList) {
            if (tempElevated.isExpired) tempElevatedList.remove(tempElevated)
          }
          for (silence in silenceList) {
            if (silence.isExpired) silenceList.remove(silence)
          }
          for (userAccess in userList) {
            userAccess.refreshDNS()
          }
          for (addressAccess in addressList) {
            addressAccess.refreshDNS()
          }
        }
      }
    } catch (e: Throwable) {
      if (!stopFlag) {
        logger.atSevere().withCause(e).log("AccessManager2 thread caught unexpected exception")
      }
    } finally {
      isRunning = false
      logger.atFine().log("AccessManager2 thread exiting...")
    }
  }

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
        if (Strings.isNullOrEmpty(line) || line!!.startsWith("#") || line!!.startsWith("//"))
            continue
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

  override fun addTempBan(addressPattern: String, minutes: Int) {
    tempBanList.add(TempBan(addressPattern, minutes))
  }

  override fun addTempAdmin(addressPattern: String, minutes: Int) {
    tempAdminList.add(TempAdmin(addressPattern, minutes))
  }

  override fun addTempModerator(addressPattern: String, minutes: Int) {
    tempModeratorList.add(TempModerator(addressPattern, minutes))
  }

  override fun addTempElevated(addressPattern: String, minutes: Int) {
    tempElevatedList.add(TempElevated(addressPattern, minutes))
  }

  override fun addSilenced(addressPattern: String, minutes: Int) {
    silenceList.add(Silence(addressPattern, minutes))
  }

  @Synchronized
  override fun getAnnouncement(address: InetAddress?): String? {
    checkReload()
    val userAddress = address!!.hostAddress
    for (userAccess in userList) {
      if (userAccess.matches(userAddress)) return userAccess.message
    }
    return null
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
    for (userAccess in userList) {
      if (userAccess.matches(userAddress)) return userAccess.access
    }
    return AccessManager.ACCESS_NORMAL
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
    for (gameAccess in gameList) {
      if (gameAccess.matches(game)) return gameAccess.access
    }
    return true
  }

  protected inner class UserAccess(st: StringTokenizer) {
    protected var hostNames: MutableList<String>
    protected var resolvedAddresses: MutableList<String>
    var access = 0
      protected set
    var message: String? = null
      protected set

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
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

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
      if (st.countTokens() < 2 || st.countTokens() > 3)
          throw Exception("Wrong number of tokens: " + st.countTokens())
      val accessStr = st.nextToken().lowercase(Locale.getDefault())
      access =
          if (accessStr == "normal") AccessManager.ACCESS_NORMAL
          else if (accessStr == "elevated") AccessManager.ACCESS_ELEVATED
          else if (accessStr == "moderator") AccessManager.ACCESS_MODERATOR
          else if (accessStr == "admin") AccessManager.ACCESS_ADMIN
          else if (accessStr == "superadmin") AccessManager.ACCESS_SUPERADMIN
          else throw AccessException("Unrecognized access token: $accessStr")
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

  protected inner class AddressAccess(st: StringTokenizer) {
    protected var hostNames: MutableList<String>
    protected var resolvedAddresses: MutableList<String>
    var access = false
      protected set

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
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

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
          if (accessStr == "allow") true
          else if (accessStr == "deny") false
          else throw AccessException("Unrecognized access token: $accessStr")
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

  protected inner class EmulatorAccess(st: StringTokenizer) {
    var access = false
      protected set

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

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
          if (accessStr == "allow") true
          else if (accessStr == "deny") false
          else throw AccessException("Unrecognized access token: $accessStr")
      patterns = ArrayList()
      val s = st.nextToken().lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
    }
  }

  protected inner class GameAccess(st: StringTokenizer) {
    var access = false
      protected set

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

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
          if (accessStr == "allow") true
          else if (accessStr == "deny") false
          else throw AccessException("Unrecognized access token: $accessStr")
      patterns = ArrayList()
      val s = st.nextToken().lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
    }
  }

  protected inner class TempBan(accessStr: String?, minutes: Int) {
    protected var startTime: Long
    protected var minutes: Int

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

    val isExpired: Boolean
      get() = if (System.currentTimeMillis() > startTime + minutes * 60000) true else false

    fun matches(address: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(address)) return true
      }
      return false
    }

    init {
      patterns = ArrayList()
      val s = accessStr!!.lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
      this.minutes = minutes
      startTime = System.currentTimeMillis()
    }
  }

  protected inner class TempAdmin(accessStr: String?, minutes: Int) {
    protected var startTime: Long
    protected var minutes: Int

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

    val isExpired: Boolean
      get() = if (System.currentTimeMillis() > startTime + minutes * 60000) true else false

    fun matches(address: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(address)) return true
      }
      return false
    }

    init {
      patterns = ArrayList()
      val s = accessStr!!.lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
      this.minutes = minutes
      startTime = System.currentTimeMillis()
    }
  }

  protected inner class TempModerator(accessStr: String?, minutes: Int) {
    protected var startTime: Long
    protected var minutes: Int

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

    val isExpired: Boolean
      get() = if (System.currentTimeMillis() > startTime + minutes * 60000) true else false

    fun matches(address: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(address)) return true
      }
      return false
    }

    init {
      patterns = ArrayList()
      val s = accessStr!!.lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
      this.minutes = minutes
      startTime = System.currentTimeMillis()
    }
  }

  protected inner class TempElevated(accessStr: String?, minutes: Int) {
    protected var startTime: Long
    protected var minutes: Int

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

    val isExpired: Boolean
      get() = if (System.currentTimeMillis() > startTime + minutes * 60000) true else false

    fun matches(address: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(address)) return true
      }
      return false
    }

    init {
      patterns = ArrayList()
      val s = accessStr!!.lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
      this.minutes = minutes
      startTime = System.currentTimeMillis()
    }
  }

  protected inner class Silence(accessStr: String?, minutes: Int) {
    protected var startTime: Long
    protected var minutes: Int

    private var patterns: MutableList<WildcardStringPattern>
    protected fun getPatterns(): List<WildcardStringPattern> {
      return patterns
    }

    val isExpired: Boolean
      get() = if (System.currentTimeMillis() > startTime + minutes * 60000) true else false

    fun matches(address: String?): Boolean {
      for (pattern in patterns) {
        if (pattern.match(address)) return true
      }
      return false
    }

    init {
      patterns = ArrayList()
      val s = accessStr!!.lowercase(Locale.getDefault())
      val pt = StringTokenizer(s, "|")
      while (pt.hasMoreTokens()) {
        patterns.add(WildcardStringPattern(pt.nextToken().lowercase(Locale.getDefault())))
      }
      this.minutes = minutes
      startTime = System.currentTimeMillis()
    }
  }

  init {
    val url = AccessManager2::class.java.getResource("/access.cfg")
    checkArgument(url != null, "Resource not found: /access.conf")
    accessFile =
        try {
          File(url!!.toURI())
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
    threadPool.execute(this)
  }
}

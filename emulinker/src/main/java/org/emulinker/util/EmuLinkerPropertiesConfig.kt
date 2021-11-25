package org.emulinker.util

import org.apache.commons.configuration.PropertiesConfiguration

class EmuLinkerPropertiesConfig :
    PropertiesConfiguration(EmuLinkerPropertiesConfig::class.java.getResource("/emulinker.cfg")) {
  init {
    isThrowExceptionOnMissing = true
  }
}

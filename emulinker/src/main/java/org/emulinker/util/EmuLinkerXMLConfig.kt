package org.emulinker.util

import org.apache.commons.configuration.XMLConfiguration

class EmuLinkerXMLConfig :
    XMLConfiguration(EmuLinkerXMLConfig::class.java.getResource("/emulinker.xml")) {
  init {
    isThrowExceptionOnMissing = true
  }
}

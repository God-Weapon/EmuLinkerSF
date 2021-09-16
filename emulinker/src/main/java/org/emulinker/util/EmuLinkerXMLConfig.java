package org.emulinker.util;

import org.apache.commons.configuration.*;

public class EmuLinkerXMLConfig extends XMLConfiguration {
  public EmuLinkerXMLConfig() throws ConfigurationException {
    super(EmuLinkerXMLConfig.class.getResource("/emulinker.xml"));
    setThrowExceptionOnMissing(true);
  }
}

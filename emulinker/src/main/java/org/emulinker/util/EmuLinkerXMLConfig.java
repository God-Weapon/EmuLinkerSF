package org.emulinker.util;

import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;

public class EmuLinkerXMLConfig extends XMLConfiguration {
  // private static Log	log	= LogFactory.getLog(EmuLinkerXMLConfig.class);

  public EmuLinkerXMLConfig() throws ConfigurationException {
    super(EmuLinkerXMLConfig.class.getResource("/emulinker.xml"));
    setThrowExceptionOnMissing(true);
  }
}

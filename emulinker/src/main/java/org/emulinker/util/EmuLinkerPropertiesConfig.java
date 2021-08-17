package org.emulinker.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class EmuLinkerPropertiesConfig extends PropertiesConfiguration {

  public EmuLinkerPropertiesConfig() throws ConfigurationException {
    super(EmuLinkerPropertiesConfig.class.getResource("/emulinker.cfg"));
    setThrowExceptionOnMissing(true);
  }
}

package org.emulinker.util;

import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;

public class EmuLinkerPropertiesConfig extends PropertiesConfiguration
{
	//private static Log	log	= LogFactory.getLog(EmuLinkerPropertiesConfig.class);

	public EmuLinkerPropertiesConfig() throws ConfigurationException
	{
		super(EmuLinkerPropertiesConfig.class.getResource("/emulinker.cfg"));
		setThrowExceptionOnMissing(true);
	}
}

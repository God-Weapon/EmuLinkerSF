package org.emulinker.kaillera.model.impl;

import java.util.*;
import java.util.regex.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.emulinker.util.EmuUtil;

import org.emulinker.kaillera.model.*;

public interface AutoFireDetectorFactory
{
	public AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity);
}

package org.emulinker.kaillera.model.impl;

import java.util.*;
import java.util.regex.*;
import org.emulinker.kaillera.model.*;

public interface AutoFireDetectorFactory {
  public AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity);
}

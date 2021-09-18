package org.emulinker.kaillera.model.impl;

import org.emulinker.kaillera.model.KailleraGame;

public interface AutoFireDetectorFactory {
  public AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity);
}

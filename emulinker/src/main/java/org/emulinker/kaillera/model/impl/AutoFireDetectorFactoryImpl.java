package org.emulinker.kaillera.model.impl;

import javax.inject.Inject;
import org.emulinker.kaillera.model.*;

public final class AutoFireDetectorFactoryImpl implements AutoFireDetectorFactory {

  @Inject
  AutoFireDetectorFactoryImpl() {}

  @Override
  public AutoFireDetector getInstance(KailleraGame game, int defaultSensitivity) {
    return new AutoFireScanner2(game, defaultSensitivity);
  }
}

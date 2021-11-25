package org.emulinker.kaillera.model.impl

import javax.inject.Inject
import org.emulinker.kaillera.model.KailleraGame

class AutoFireDetectorFactoryImpl @Inject internal constructor() : AutoFireDetectorFactory {
  override fun getInstance(game: KailleraGame, defaultSensitivity: Int): AutoFireDetector {
    return AutoFireScanner2(game, defaultSensitivity)
  }
}

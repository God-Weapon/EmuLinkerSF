package org.emulinker.kaillera.model.impl

import org.emulinker.kaillera.model.KailleraGame

interface AutoFireDetectorFactory {
  fun getInstance(game: KailleraGame, defaultSensitivity: Int): AutoFireDetector
}

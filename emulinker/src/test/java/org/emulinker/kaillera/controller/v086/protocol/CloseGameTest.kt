package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CloseGameTest {
  @Test
  fun bodyLength() {
    val message = CloseGame(messageNumber = 42, gameId = 40, val1 = 1)

    assertThat(message.bodyLength).isEqualTo(5)
  }
}

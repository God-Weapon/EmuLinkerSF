package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.truth.Truth
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.Throws
import org.emulinker.kaillera.controller.v086.V086Utils
import org.emulinker.kaillera.pico.AppModule
import org.junit.Test

class V086BundleTest {
  @Test
  @Throws(Exception::class)
  fun parseUserInformationJapanese() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = Charset.forName("Shift_JIS")
    val hexInput =
        "01 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01"
    val lastMessageNumber = -1
    val parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber)
    Truth.assertThat(parsedBundle.getMessages()).hasLength(1)
    Truth.assertThat(parsedBundle.getMessages()[0]).isInstanceOf(UserInformation::class.java)
    val userInformation = parsedBundle.getMessages()[0] as UserInformation
    Truth.assertThat(userInformation.clientType).isEqualTo("Project 64k 0.13 (01 Aug 2003)")
    Truth.assertThat(userInformation.username).isEqualTo("鵺")
  }

  @Test
  @Throws(Exception::class)
  fun parseClientACK() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = StandardCharsets.UTF_8
    val hexInput =
        "02 01 00 12 00 06 00 00 00 00 00 01 00 00 00 02 00 00 00 03 00 00 00 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01"
    val lastMessageNumber = 0
    val parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber)
    Truth.assertThat(parsedBundle.getMessages()).hasLength(1)
    Truth.assertThat(parsedBundle.getMessages()[0]).isEqualTo(ClientACK(messageNumber = 1))
  }

  @Test
  @Throws(Exception::class)
  fun parseClientCreateGameRequest() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = StandardCharsets.UTF_8
    val hexInput =
        "03 0A 00 17 00 0A 00 53 6D 61 73 68 52 65 6D 69 78 30 2E 39 2E 37 00 00 FF FF FF FF 09 00 04 00 0B 00 FF FF 08 00 37 00 0A 00 4E 69 6E 74 65 6E 64 6F 20 41 6C 6C 2D 53 74 61 72 21 20 44 61 69 72 61 6E 74 6F 75 20 53 6D 61 73 68 20 42 72 6F 74 68 65 72 73 20 28 4A 29 00 00 FF FF FF FF"
    val lastMessageNumber = 9
    val parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber)
    Truth.assertThat(parsedBundle.getMessages()).hasLength(1)
    Truth.assertThat(parsedBundle.getMessages()[0]).isInstanceOf(CreateGame_Request::class.java)
    val message = parsedBundle.getMessages()[0] as CreateGame_Request
    Truth.assertThat(message).isEqualTo(CreateGame_Request(messageNumber = 10, "SmashRemix0.9.7"))
  }
}

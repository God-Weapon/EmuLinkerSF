package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.truth.Truth.assertThat
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import org.emulinker.kaillera.controller.v086.V086Utils
import org.emulinker.kaillera.model.ConnectionType
import org.emulinker.kaillera.model.GameStatus
import org.emulinker.kaillera.model.UserStatus
import org.emulinker.kaillera.pico.AppModule
import org.junit.Test

class V086BundleTest {

  @Test
  fun hexStringToByteBuffer() {
    val hexInput =
        "01 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01"
    val byteBuffer = V086Utils.hexStringToByteBuffer(hexInput)
    assertThat(V086Utils.toHex(byteBuffer)).isEqualTo(hexInput.replace(" ", ""))
  }

  @Test
  fun parseUserInformationJapanese() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = Charset.forName("Shift_JIS")
    val hexInput =
        "01 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01"
    val lastMessageNumber = -1
    val parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber)
    assertThat(parsedBundle.messages).hasLength(1)
    assertThat(parsedBundle.messages[0]).isInstanceOf(UserInformation::class.java)
    val userInformation = parsedBundle.messages[0] as UserInformation
    assertThat(userInformation.clientType).isEqualTo("Project 64k 0.13 (01 Aug 2003)")
    assertThat(userInformation.username).isEqualTo("鵺")
  }

  @Test
  fun parseClientACK() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = StandardCharsets.UTF_8
    val hexInput =
        "02 01 00 12 00 06 00 00 00 00 00 01 00 00 00 02 00 00 00 03 00 00 00 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01"
    val lastMessageNumber = 0
    val parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber)
    assertThat(parsedBundle.messages).hasLength(1)
    assertThat(parsedBundle.messages[0]).isEqualTo(ClientACK(messageNumber = 1))
  }

  @Test
  fun parseClientCreateGameRequest() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = StandardCharsets.UTF_8
    val hexInput =
        "03 0A 00 17 00 0A 00 53 6D 61 73 68 52 65 6D 69 78 30 2E 39 2E 37 00 00 FF FF FF FF 09 00 04 00 0B 00 FF FF 08 00 37 00 0A 00 4E 69 6E 74 65 6E 64 6F 20 41 6C 6C 2D 53 74 61 72 21 20 44 61 69 72 61 6E 74 6F 75 20 53 6D 61 73 68 20 42 72 6F 74 68 65 72 73 20 28 4A 29 00 00 FF FF FF FF"
    val lastMessageNumber = 9
    val parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber)
    assertThat(parsedBundle.messages).hasLength(1)
    assertThat(parsedBundle.messages[0]).isInstanceOf(CreateGame_Request::class.java)
    val message = parsedBundle.messages[0] as CreateGame_Request
    assertThat(message).isEqualTo(CreateGame_Request(messageNumber = 10, "SmashRemix0.9.7"))
  }

  @Test
  fun parseClientThingFromThatThing() {
    // TODO(nue): We should dagger-ize this and use the RuntimeFlags class.
    AppModule.charsetDoNotUse = Charset.forName("Shift_JIS")
    // f8 ac 65 18 56 28 28 bd 89 df ee 8d 08 00 45 00 00 b9 fa b0 00 00 73 11 2e 86 96 f9 6f af c0
    // a8 56 ac ed 33 cc df 00 a5 67 ce
    val hexInput =
        "03 06 00 11 00 17 53 65 72 76 65 72 00 92 e8 88 f5 33 30 96 bc 00 05 00 0b 00 02 6a 6a 00 8b 01 1f 00 00 00 01 04 00 74 00 04 00 01 00 00 00 01 00 00 00 74 65 73 74 00 13 00 00 00 02 88 01 01 4e 69 6e 74 65 6e 64 6f 20 41 6c 6c 2d 53 74 61 72 21 20 44 61 69 72 61 6e 74 6f 75 20 53 6d 61 73 68 20 42 72 6f 74 68 65 72 73 20 28 4a 29 00 3d 00 00 00 50 72 6f 6a 65 63 74 20 36 34 6b 20 30 2e 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 74 65 73 74 00 31 2f 32 00 02"
    val parsedBundle = V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput))
    assertThat(parsedBundle.messages)
        .isEqualTo(
            arrayOf(
                InformationMessage(messageNumber = 6, source = "Server", message = "定員30名"),
                UserJoined(
                    messageNumber = 5,
                    username = "jj",
                    userId = 395,
                    ping = 31,
                    connectionType = ConnectionType.LAN),
                ServerStatus(
                    messageNumber = 4,
                    users =
                        listOf(
                            ServerStatus.User(
                                username = "test",
                                ping = 19,
                                status = UserStatus.CONNECTING,
                                userId = 392,
                                connectionType = ConnectionType.LAN),
                        ),
                    games =
                        listOf(
                            ServerStatus.Game(
                                romName = "Nintendo All-Star! Dairantou Smash Brothers (J)",
                                gameId = 61,
                                clientType = "Project 64k 0.13 (01 Aug 2003)",
                                username = "test",
                                playerCountOutOfMax = "1/2",
                                status = GameStatus.PLAYING)))))
  }

  // TODO(nue): Move this into ServerStatusTest.kt. For some reason it will not pass if I do that.
  // I suspect a Kotlin bug related to nested data classes....
  @Test
  fun serverStatus_bodyLength() {
    assertThat(
            ServerStatus(
                    messageNumber = 4,
                    users =
                        listOf(
                            ServerStatus.User(
                                username = "test",
                                ping = 19,
                                status = UserStatus.CONNECTING,
                                userId = 392,
                                connectionType = ConnectionType.LAN),
                        ),
                    games =
                        listOf(
                            ServerStatus.Game(
                                romName = "Nintendo All-Star! Dairantou Smash Brothers (J)",
                                gameId = 61,
                                clientType = "Project 64k 0.13 (01 Aug 2003)",
                                username = "test",
                                playerCountOutOfMax = "1/2",
                                status = GameStatus.PLAYING)))
                .bodyLength)
        .isEqualTo(115)
  }
}

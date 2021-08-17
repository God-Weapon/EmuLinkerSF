package org.emulinker.kaillera.controller.v086.protocol;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.controller.v086.V086Utils;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.junit.Test;

public class V086BundleTest {
  @Test
  public void parseUserInformationJapanese() throws Exception {
    KailleraRelay.config = RuntimeFlags.builder().setCharset(Charset.forName("Shift_JIS")).build();

    String hexInput =
        "01 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01";
    int lastMessageNumber = -1;

    V086Bundle parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber);
    assertThat(parsedBundle.getMessages()).hasLength(1);
    assertThat(parsedBundle.getMessages()[0]).isInstanceOf(UserInformation.class);

    UserInformation userInformation = (UserInformation) parsedBundle.getMessages()[0];
    // TODO: Consider using AutoValue so we can just directly compare objects.
    assertThat(userInformation.getClientType()).isEqualTo("Project 64k 0.13 (01 Aug 2003)");
    assertThat(userInformation.getUserName()).isEqualTo("鵺");
  }

  @Test
  public void parseClientACK() throws Exception {
    KailleraRelay.config = RuntimeFlags.builder().setCharset(StandardCharsets.UTF_8).build();

    String hexInput =
        "02 01 00 12 00 06 00 00 00 00 00 01 00 00 00 02 00 00 00 03 00 00 00 00 00 24 00 03 EA 4B 00 50 72 6F 6A 65 63 74 20 36 34 6B 20 30 2E 31 33 20 28 30 31 20 41 75 67 20 32 30 30 33 29 00 01";
    int lastMessageNumber = 0;

    V086Bundle parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber);
    assertThat(parsedBundle.getMessages()).hasLength(1);
    assertThat(parsedBundle.getMessages()[0]).isInstanceOf(ClientACK.class);
  }

  @Test
  public void parseClientCreateGameRequest() throws Exception {
    KailleraRelay.config = RuntimeFlags.builder().setCharset(StandardCharsets.UTF_8).build();

    String hexInput =
        "03 0A 00 17 00 0A 00 53 6D 61 73 68 52 65 6D 69 78 30 2E 39 2E 37 00 00 FF FF FF FF 09 00 04 00 0B 00 FF FF 08 00 37 00 0A 00 4E 69 6E 74 65 6E 64 6F 20 41 6C 6C 2D 53 74 61 72 21 20 44 61 69 72 61 6E 74 6F 75 20 53 6D 61 73 68 20 42 72 6F 74 68 65 72 73 20 28 4A 29 00 00 FF FF FF FF";
    int lastMessageNumber = 9;

    V086Bundle parsedBundle =
        V086Bundle.parse(V086Utils.hexStringToByteBuffer(hexInput), lastMessageNumber);
    assertThat(parsedBundle.getMessages()).hasLength(1);
    assertThat(parsedBundle.getMessages()[0]).isInstanceOf(CreateGame_Request.class);

    CreateGame_Request message = (CreateGame_Request) parsedBundle.getMessages()[0];
    assertThat(message.toString()).isEqualTo("10:0A/Create Game Request[romName=SmashRemix0.9.7]");
  }
}

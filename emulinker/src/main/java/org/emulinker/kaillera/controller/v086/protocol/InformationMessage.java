package org.emulinker.kaillera.controller.v086.protocol;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.EmuUtil;

@AutoValue
public abstract class InformationMessage extends V086Message {
  public static final byte ID = 0x17;
  private static final String DESC = "Information Message";

  public abstract String source();

  public abstract String message();

  public static AutoValue_InformationMessage create(
      int messageNumber, String source, String message) throws MessageFormatException {
    V086Message.validateMessageNumber(messageNumber, DESC);

    if (Strings.isNullOrEmpty(source)) {
      throw new MessageFormatException("Invalid " + DESC + " format: source.length == 0");
    }

    if (Strings.isNullOrEmpty(message)) {
      throw new MessageFormatException("Invalid " + DESC + " format: message.length == 0");
    }
    return new AutoValue_InformationMessage(messageNumber, ID, DESC, source, message);
  }

  @Override
  public int getBodyLength() {
    return getNumBytes(source()) + getNumBytes(message()) + 2;
  }

  @Override
  public void writeBodyTo(ByteBuffer buffer) {
    EmuUtil.writeString(buffer, source(), 0x00, KailleraRelay.config.charset());
    EmuUtil.writeString(buffer, message(), 0x00, KailleraRelay.config.charset());
  }

  public static InformationMessage parse(int messageNumber, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    if (buffer.remaining() < 4) throw new ParseException("Failed byte count validation!");

    String source = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    if (buffer.remaining() < 2) throw new ParseException("Failed byte count validation!");

    String message = EmuUtil.readString(buffer, 0x00, KailleraRelay.config.charset());

    return InformationMessage.create(messageNumber, source, message);
  }
}

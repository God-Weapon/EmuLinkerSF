package org.emulinker.kaillera.controller.v086.protocol;

import com.google.common.flogger.FluentLogger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.emulinker.kaillera.controller.messaging.*;
import org.emulinker.kaillera.relay.KailleraRelay;
import org.emulinker.util.*;

public abstract class V086Message extends ByteBufferMessage {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public abstract int messageNumber();

  protected static void validateMessageNumber(int messageNumber, String description)
      throws MessageFormatException {
    if (messageNumber < 0 || messageNumber > 0xFFFF) {
      throw new MessageFormatException(
          "Invalid " + description + " format: Invalid message number: " + messageNumber);
    }
  }

  public abstract byte messageId();

  @Override
  public abstract String description();

  @Override
  public int getLength() {
    return getBodyLength() + 1;
    // return (getBodyLength() + 5);
  }

  /** Gets the number of bytes to represent the string in the charset defined in emulinker.config */
  protected int getNumBytes(String s) {
    return s.getBytes(KailleraRelay.config.charset()).length;
  }

  public abstract int getBodyLength();

  // TODO(nue): Figure out how to stuff this in the AutoValue toString.
  protected String getInfoString() {
    return (messageNumber() + ":" + EmuUtil.byteToHex(messageId()) + "/" + description());
  }

  @Override
  public void writeTo(ByteBuffer buffer) {
    int len = getLength();
    if (len > buffer.remaining()) {
      logger.atWarning().log(
          "Ran out of output buffer space, consider increasing the controllers.v086.bufferSize setting!");
    } else {
      UnsignedUtil.putUnsignedShort(buffer, messageNumber());
      // there no realistic reason to use unsigned here since a single packet can't be that large
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      ((Buffer) buffer).mark();
      UnsignedUtil.putUnsignedShort(buffer, len);
      //		buffer.putShort((short)getLength());
      buffer.put(messageId());
      writeBodyTo(buffer);
    }
  }

  protected abstract void writeBodyTo(ByteBuffer buffer);

  public static V086Message parse(int messageNumber, int messageLength, ByteBuffer buffer)
      throws ParseException, MessageFormatException {
    byte messageType = buffer.get();

    // removed to increase speed
    //		if (messageType < 0 || messageType > 0x17)
    //			throw new MessageFormatException("Invalid message type: " + messageType);

    V086Message message = null;
    switch (messageType) {
      case Quit.ID: // 01
        message = Quit.parse(messageNumber, buffer);
        break;

      case UserJoined.ID: // 02
        message = UserJoined.parse(messageNumber, buffer);
        break;

      case UserInformation.ID: // 03
        message = UserInformation.parse(messageNumber, buffer);
        break;

      case ServerStatus.ID: // 04
        message = ServerStatus.parse(messageNumber, buffer);
        break;

      case ServerACK.ID: // 05
        message = ServerACK.parse(messageNumber, buffer);
        break;

      case ClientACK.ID: // 06
        message = ClientACK.parse(messageNumber, buffer);
        break;

      case Chat.ID: // 07
        message = Chat.parse(messageNumber, buffer);
        break;

      case GameChat.ID: // 08
        message = GameChat.parse(messageNumber, buffer);
        break;

      case KeepAlive.ID: // 09
        message = KeepAlive.parse(messageNumber, buffer);
        break;

      case CreateGame.ID: // 0A
        message = CreateGame.parse(messageNumber, buffer);
        break;

      case QuitGame.ID: // 0B
        message = QuitGame.parse(messageNumber, buffer);
        break;

      case JoinGame.ID: // 0C
        message = JoinGame.parse(messageNumber, buffer);
        break;

      case PlayerInformation.ID: // 0D
        message = PlayerInformation.parse(messageNumber, buffer);
        break;

      case GameStatus.ID: // 0E
        message = GameStatus.parse(messageNumber, buffer);
        break;

      case GameKick.ID: // 0F
        message = GameKick.parse(messageNumber, buffer);
        break;

      case CloseGame.ID: // 10
        message = CloseGame.parse(messageNumber, buffer);
        break;

      case StartGame.ID: // 11
        message = StartGame.parse(messageNumber, buffer);
        break;

      case GameData.ID: // 12
        message = GameData.parse(messageNumber, buffer);
        break;

      case CachedGameData.ID: // 13
        message = CachedGameData.parse(messageNumber, buffer);
        break;

      case PlayerDrop.ID: // 14
        message = PlayerDrop.parse(messageNumber, buffer);
        break;

      case AllReady.ID: // 15
        message = AllReady.parse(messageNumber, buffer);
        break;

      case ConnectionRejected.ID: // 16
        message = ConnectionRejected.parse(messageNumber, buffer);
        break;

      case InformationMessage.ID: // 17
        message = InformationMessage.parse(messageNumber, buffer);
        break;

      default:
        throw new MessageFormatException("Invalid message type: " + messageType);
    }

    // removed to improve speed
    if (message.getLength() != messageLength) {
      //			throw new ParseException("Bundle contained length " + messageLength + " !=  parsed length
      // " + message.getLength());
      logger.atFine().log(
          "Bundle contained length " + messageLength + " !=  parsed length " + message.getLength());
    }

    return message;
  }
}

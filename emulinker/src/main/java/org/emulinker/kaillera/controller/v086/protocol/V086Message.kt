package org.emulinker.kaillera.controller.v086.protocol

import com.google.common.flogger.FluentLogger
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.Throws
import org.emulinker.kaillera.controller.messaging.ByteBufferMessage
import org.emulinker.kaillera.controller.messaging.MessageFormatException
import org.emulinker.kaillera.controller.messaging.ParseException
import org.emulinker.kaillera.pico.AppModule
import org.emulinker.util.EmuUtil
import org.emulinker.util.UnsignedUtil

private val logger = FluentLogger.forEnclosingClass()

abstract class V086Message : ByteBufferMessage() {
  abstract val messageNumber: Int

  @Deprecated("We should try to use a sealed class instead of relying on this messageId field")
  abstract val messageId: Byte

  // return (getBodyLength() + 5);
  override val length: Int
    get() = bodyLength + 1

  // return (getBodyLength() + 5);
  /** Gets the number of bytes to represent the string in the charset defined in emulinker.config */
  protected fun getNumBytes(s: String): Int {
    return s.toByteArray(AppModule.charsetDoNotUse).size
  }

  abstract val bodyLength: Int

  // TODO(nue): Figure out how to stuff this in the AutoValue toString.
  protected val infoString: String
    get() = messageNumber.toString() + ":" + EmuUtil.byteToHex(messageId) + "/" + shortName

  override fun writeTo(buffer: ByteBuffer?) {
    val len = length
    if (len > buffer!!.remaining()) {
      logger
          .atWarning()
          .log(
              "Ran out of output buffer space, consider increasing the controllers.v086.bufferSize setting!")
    } else {
      UnsignedUtil.putUnsignedShort(buffer, messageNumber)
      // there no realistic reason to use unsigned here since a single packet can't be that large
      // Cast to avoid issue with java version mismatch:
      // https://stackoverflow.com/a/61267496/2875073
      (buffer as Buffer?)!!.mark()
      UnsignedUtil.putUnsignedShort(buffer, len)
      //		buffer.putShort((short)getLength());
      buffer.put(messageId)
      writeBodyTo(buffer)
    }
  }

  protected abstract fun writeBodyTo(buffer: ByteBuffer)

  companion object {
    @JvmStatic
    @Throws(MessageFormatException::class)
    protected fun validateMessageNumber(messageNumber: Int, description: String) {
      if (messageNumber < 0 || messageNumber > 0xFFFF) {
        throw MessageFormatException(
            "Invalid $description format: Invalid message number: $messageNumber")
      }
    }

    @JvmStatic
    @Throws(ParseException::class, MessageFormatException::class)
    fun parse(messageNumber: Int, messageLength: Int, buffer: ByteBuffer): V086Message {

      // removed to increase speed
      //		if (messageType < 0 || messageType > 0x17)
      //			throw new MessageFormatException("Invalid message type: " + messageType);
      val message: V086Message =
          when (val messageType = buffer.get()
          ) {
            Quit.ID -> Quit.parse(messageNumber, buffer)
            UserJoined.ID -> UserJoined.parse(messageNumber, buffer)
            UserInformation.ID -> UserInformation.parse(messageNumber, buffer)
            ServerStatus.ID -> ServerStatus.parse(messageNumber, buffer)
            ServerACK.ID -> ServerACK.parse(messageNumber, buffer)
            ClientACK.ID -> ClientACK.parse(messageNumber, buffer)
            Chat.ID -> Chat.parse(messageNumber, buffer)
            GameChat.ID -> GameChat.parse(messageNumber, buffer)
            KeepAlive.ID -> KeepAlive.parse(messageNumber, buffer)
            CreateGame.ID -> CreateGame.parse(messageNumber, buffer)
            QuitGame.ID -> QuitGame.parse(messageNumber, buffer)
            JoinGame.ID -> JoinGame.parse(messageNumber, buffer)
            PlayerInformation.ID -> PlayerInformation.parse(messageNumber, buffer)
            GameStatus.ID -> GameStatus.parse(messageNumber, buffer)
            GameKick.ID -> GameKick.parse(messageNumber, buffer)
            CloseGame.ID -> CloseGame.parse(messageNumber, buffer)
            StartGame.ID -> StartGame.parse(messageNumber, buffer)
            GameData.ID -> GameData.parse(messageNumber, buffer)
            CachedGameData.ID -> CachedGameData.parse(messageNumber, buffer)
            PlayerDrop.ID -> PlayerDrop.parse(messageNumber, buffer)
            AllReady.ID -> AllReady.parse(messageNumber, buffer)
            ConnectionRejected.ID -> ConnectionRejected.parse(messageNumber, buffer)
            InformationMessage.ID -> InformationMessage.parse(messageNumber, buffer)
            else -> throw MessageFormatException("Invalid message type: $messageType")
          }

      // removed to improve speed
      if (message.length != messageLength) {
        //			throw new ParseException("Bundle contained length " + messageLength + " !=  parsed
        // lengthy
        // " + message.getLength());
        logger
            .atFine()
            .log("Bundle contained length $messageLength !=  parsed length ${message.length}")
      }
      return message
    }
  }
}

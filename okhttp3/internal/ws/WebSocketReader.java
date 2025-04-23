package okhttp3.internal.ws;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;

final class WebSocketReader {
   final boolean isClient;
   final BufferedSource source;
   final WebSocketReader.FrameCallback frameCallback;
   boolean closed;
   int opcode;
   long frameLength;
   boolean isFinalFrame;
   boolean isControlFrame;
   private final Buffer controlFrameBuffer = new Buffer();
   private final Buffer messageFrameBuffer = new Buffer();
   private final byte[] maskKey;
   private final Buffer.UnsafeCursor maskCursor;

   WebSocketReader(boolean isClient, BufferedSource source, WebSocketReader.FrameCallback frameCallback) {
      if (source == null) {
         throw new NullPointerException("source == null");
      } else if (frameCallback == null) {
         throw new NullPointerException("frameCallback == null");
      } else {
         this.isClient = isClient;
         this.source = source;
         this.frameCallback = frameCallback;
         this.maskKey = isClient ? null : new byte[4];
         this.maskCursor = isClient ? null : new Buffer.UnsafeCursor();
      }
   }

   void processNextFrame() throws IOException {
      this.readHeader();
      if (this.isControlFrame) {
         this.readControlFrame();
      } else {
         this.readMessageFrame();
      }

   }

   private void readHeader() throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         long timeoutBefore = this.source.timeout().timeoutNanos();
         this.source.timeout().clearTimeout();

         int b0;
         try {
            b0 = this.source.readByte() & 255;
         } finally {
            this.source.timeout().timeout(timeoutBefore, TimeUnit.NANOSECONDS);
         }

         this.opcode = b0 & 15;
         this.isFinalFrame = (b0 & 128) != 0;
         this.isControlFrame = (b0 & 8) != 0;
         if (this.isControlFrame && !this.isFinalFrame) {
            throw new ProtocolException("Control frames must be final.");
         } else {
            boolean reservedFlag1 = (b0 & 64) != 0;
            boolean reservedFlag2 = (b0 & 32) != 0;
            boolean reservedFlag3 = (b0 & 16) != 0;
            if (!reservedFlag1 && !reservedFlag2 && !reservedFlag3) {
               int b1 = this.source.readByte() & 255;
               boolean isMasked = (b1 & 128) != 0;
               if (isMasked == this.isClient) {
                  throw new ProtocolException(this.isClient ? "Server-sent frames must not be masked." : "Client-sent frames must be masked.");
               } else {
                  this.frameLength = (long)(b1 & 127);
                  if (this.frameLength == 126L) {
                     this.frameLength = (long)this.source.readShort() & 65535L;
                  } else if (this.frameLength == 127L) {
                     this.frameLength = this.source.readLong();
                     if (this.frameLength < 0L) {
                        throw new ProtocolException("Frame length 0x" + Long.toHexString(this.frameLength) + " > 0x7FFFFFFFFFFFFFFF");
                     }
                  }

                  if (this.isControlFrame && this.frameLength > 125L) {
                     throw new ProtocolException("Control frame must be less than 125B.");
                  } else {
                     if (isMasked) {
                        this.source.readFully(this.maskKey);
                     }

                  }
               }
            } else {
               throw new ProtocolException("Reserved flags are unsupported.");
            }
         }
      }
   }

   private void readControlFrame() throws IOException {
      if (this.frameLength > 0L) {
         this.source.readFully(this.controlFrameBuffer, this.frameLength);
         if (!this.isClient) {
            this.controlFrameBuffer.readAndWriteUnsafe(this.maskCursor);
            this.maskCursor.seek(0L);
            WebSocketProtocol.toggleMask(this.maskCursor, this.maskKey);
            this.maskCursor.close();
         }
      }

      switch(this.opcode) {
      case 8:
         int code = 1005;
         String reason = "";
         long bufferSize = this.controlFrameBuffer.size();
         if (bufferSize == 1L) {
            throw new ProtocolException("Malformed close payload length of 1.");
         }

         if (bufferSize != 0L) {
            code = this.controlFrameBuffer.readShort();
            reason = this.controlFrameBuffer.readUtf8();
            String codeExceptionMessage = WebSocketProtocol.closeCodeExceptionMessage(code);
            if (codeExceptionMessage != null) {
               throw new ProtocolException(codeExceptionMessage);
            }
         }

         this.frameCallback.onReadClose(code, reason);
         this.closed = true;
         break;
      case 9:
         this.frameCallback.onReadPing(this.controlFrameBuffer.readByteString());
         break;
      case 10:
         this.frameCallback.onReadPong(this.controlFrameBuffer.readByteString());
         break;
      default:
         throw new ProtocolException("Unknown control opcode: " + Integer.toHexString(this.opcode));
      }

   }

   private void readMessageFrame() throws IOException {
      int opcode = this.opcode;
      if (opcode != 1 && opcode != 2) {
         throw new ProtocolException("Unknown opcode: " + Integer.toHexString(opcode));
      } else {
         this.readMessage();
         if (opcode == 1) {
            this.frameCallback.onReadMessage(this.messageFrameBuffer.readUtf8());
         } else {
            this.frameCallback.onReadMessage(this.messageFrameBuffer.readByteString());
         }

      }
   }

   private void readUntilNonControlFrame() throws IOException {
      while(true) {
         if (!this.closed) {
            this.readHeader();
            if (this.isControlFrame) {
               this.readControlFrame();
               continue;
            }
         }

         return;
      }
   }

   private void readMessage() throws IOException {
      do {
         if (this.closed) {
            throw new IOException("closed");
         }

         if (this.frameLength > 0L) {
            this.source.readFully(this.messageFrameBuffer, this.frameLength);
            if (!this.isClient) {
               this.messageFrameBuffer.readAndWriteUnsafe(this.maskCursor);
               this.maskCursor.seek(this.messageFrameBuffer.size() - this.frameLength);
               WebSocketProtocol.toggleMask(this.maskCursor, this.maskKey);
               this.maskCursor.close();
            }
         }

         if (this.isFinalFrame) {
            return;
         }

         this.readUntilNonControlFrame();
      } while(this.opcode == 0);

      throw new ProtocolException("Expected continuation opcode. Got: " + Integer.toHexString(this.opcode));
   }

   public interface FrameCallback {
      void onReadMessage(String var1) throws IOException;

      void onReadMessage(ByteString var1) throws IOException;

      void onReadPing(ByteString var1);

      void onReadPong(ByteString var1);

      void onReadClose(int var1, String var2);
   }
}

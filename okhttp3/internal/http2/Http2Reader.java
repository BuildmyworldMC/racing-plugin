package okhttp3.internal.http2;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Source;
import okio.Timeout;

final class Http2Reader implements Closeable {
   static final Logger logger = Logger.getLogger(Http2.class.getName());
   private final BufferedSource source;
   private final Http2Reader.ContinuationSource continuation;
   private final boolean client;
   final Hpack.Reader hpackReader;

   Http2Reader(BufferedSource source, boolean client) {
      this.source = source;
      this.client = client;
      this.continuation = new Http2Reader.ContinuationSource(this.source);
      this.hpackReader = new Hpack.Reader(4096, this.continuation);
   }

   public void readConnectionPreface(Http2Reader.Handler handler) throws IOException {
      if (this.client) {
         if (!this.nextFrame(true, handler)) {
            throw Http2.ioException("Required SETTINGS preface not received");
         }
      } else {
         ByteString connectionPreface = this.source.readByteString((long)Http2.CONNECTION_PREFACE.size());
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(Util.format("<< CONNECTION %s", connectionPreface.hex()));
         }

         if (!Http2.CONNECTION_PREFACE.equals(connectionPreface)) {
            throw Http2.ioException("Expected a connection header but was %s", connectionPreface.utf8());
         }
      }

   }

   public boolean nextFrame(boolean requireSettings, Http2Reader.Handler handler) throws IOException {
      try {
         this.source.require(9L);
      } catch (IOException var7) {
         return false;
      }

      int length = readMedium(this.source);
      if (length >= 0 && length <= 16384) {
         byte type = (byte)(this.source.readByte() & 255);
         if (requireSettings && type != 4) {
            throw Http2.ioException("Expected a SETTINGS frame but was %s", type);
         } else {
            byte flags = (byte)(this.source.readByte() & 255);
            int streamId = this.source.readInt() & Integer.MAX_VALUE;
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(Http2.frameLog(true, streamId, length, type, flags));
            }

            switch(type) {
            case 0:
               this.readData(handler, length, flags, streamId);
               break;
            case 1:
               this.readHeaders(handler, length, flags, streamId);
               break;
            case 2:
               this.readPriority(handler, length, flags, streamId);
               break;
            case 3:
               this.readRstStream(handler, length, flags, streamId);
               break;
            case 4:
               this.readSettings(handler, length, flags, streamId);
               break;
            case 5:
               this.readPushPromise(handler, length, flags, streamId);
               break;
            case 6:
               this.readPing(handler, length, flags, streamId);
               break;
            case 7:
               this.readGoAway(handler, length, flags, streamId);
               break;
            case 8:
               this.readWindowUpdate(handler, length, flags, streamId);
               break;
            default:
               this.source.skip((long)length);
            }

            return true;
         }
      } else {
         throw Http2.ioException("FRAME_SIZE_ERROR: %s", length);
      }
   }

   private void readHeaders(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (streamId == 0) {
         throw Http2.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0");
      } else {
         boolean endStream = (flags & 1) != 0;
         short padding = (flags & 8) != 0 ? (short)(this.source.readByte() & 255) : 0;
         if ((flags & 32) != 0) {
            this.readPriority(handler, streamId);
            length -= 5;
         }

         length = lengthWithoutPadding(length, flags, padding);
         List<Header> headerBlock = this.readHeaderBlock(length, padding, flags, streamId);
         handler.headers(endStream, streamId, -1, headerBlock);
      }
   }

   private List<Header> readHeaderBlock(int length, short padding, byte flags, int streamId) throws IOException {
      this.continuation.length = this.continuation.left = length;
      this.continuation.padding = padding;
      this.continuation.flags = flags;
      this.continuation.streamId = streamId;
      this.hpackReader.readHeaders();
      return this.hpackReader.getAndResetHeaderList();
   }

   private void readData(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (streamId == 0) {
         throw Http2.ioException("PROTOCOL_ERROR: TYPE_DATA streamId == 0");
      } else {
         boolean inFinished = (flags & 1) != 0;
         boolean gzipped = (flags & 32) != 0;
         if (gzipped) {
            throw Http2.ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA");
         } else {
            short padding = (flags & 8) != 0 ? (short)(this.source.readByte() & 255) : 0;
            length = lengthWithoutPadding(length, flags, padding);
            handler.data(inFinished, streamId, this.source, length);
            this.source.skip((long)padding);
         }
      }
   }

   private void readPriority(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (length != 5) {
         throw Http2.ioException("TYPE_PRIORITY length: %d != 5", length);
      } else if (streamId == 0) {
         throw Http2.ioException("TYPE_PRIORITY streamId == 0");
      } else {
         this.readPriority(handler, streamId);
      }
   }

   private void readPriority(Http2Reader.Handler handler, int streamId) throws IOException {
      int w1 = this.source.readInt();
      boolean exclusive = (w1 & Integer.MIN_VALUE) != 0;
      int streamDependency = w1 & Integer.MAX_VALUE;
      int weight = (this.source.readByte() & 255) + 1;
      handler.priority(streamId, streamDependency, weight, exclusive);
   }

   private void readRstStream(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (length != 4) {
         throw Http2.ioException("TYPE_RST_STREAM length: %d != 4", length);
      } else if (streamId == 0) {
         throw Http2.ioException("TYPE_RST_STREAM streamId == 0");
      } else {
         int errorCodeInt = this.source.readInt();
         ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
         if (errorCode == null) {
            throw Http2.ioException("TYPE_RST_STREAM unexpected error code: %d", errorCodeInt);
         } else {
            handler.rstStream(streamId, errorCode);
         }
      }
   }

   private void readSettings(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (streamId != 0) {
         throw Http2.ioException("TYPE_SETTINGS streamId != 0");
      } else if ((flags & 1) != 0) {
         if (length != 0) {
            throw Http2.ioException("FRAME_SIZE_ERROR ack frame should be empty!");
         } else {
            handler.ackSettings();
         }
      } else if (length % 6 != 0) {
         throw Http2.ioException("TYPE_SETTINGS length %% 6 != 0: %s", length);
      } else {
         Settings settings = new Settings();

         for(int i = 0; i < length; i += 6) {
            int id = this.source.readShort() & '\uffff';
            int value = this.source.readInt();
            switch(id) {
            case 1:
            case 6:
            default:
               break;
            case 2:
               if (value != 0 && value != 1) {
                  throw Http2.ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1");
               }
               break;
            case 3:
               id = 4;
               break;
            case 4:
               id = 7;
               if (value < 0) {
                  throw Http2.ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1");
               }
               break;
            case 5:
               if (value < 16384 || value > 16777215) {
                  throw Http2.ioException("PROTOCOL_ERROR SETTINGS_MAX_FRAME_SIZE: %s", value);
               }
            }

            settings.set(id, value);
         }

         handler.settings(false, settings);
      }
   }

   private void readPushPromise(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (streamId == 0) {
         throw Http2.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0");
      } else {
         short padding = (flags & 8) != 0 ? (short)(this.source.readByte() & 255) : 0;
         int promisedStreamId = this.source.readInt() & Integer.MAX_VALUE;
         length -= 4;
         length = lengthWithoutPadding(length, flags, padding);
         List<Header> headerBlock = this.readHeaderBlock(length, padding, flags, streamId);
         handler.pushPromise(streamId, promisedStreamId, headerBlock);
      }
   }

   private void readPing(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (length != 8) {
         throw Http2.ioException("TYPE_PING length != 8: %s", length);
      } else if (streamId != 0) {
         throw Http2.ioException("TYPE_PING streamId != 0");
      } else {
         int payload1 = this.source.readInt();
         int payload2 = this.source.readInt();
         boolean ack = (flags & 1) != 0;
         handler.ping(ack, payload1, payload2);
      }
   }

   private void readGoAway(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (length < 8) {
         throw Http2.ioException("TYPE_GOAWAY length < 8: %s", length);
      } else if (streamId != 0) {
         throw Http2.ioException("TYPE_GOAWAY streamId != 0");
      } else {
         int lastStreamId = this.source.readInt();
         int errorCodeInt = this.source.readInt();
         int opaqueDataLength = length - 8;
         ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
         if (errorCode == null) {
            throw Http2.ioException("TYPE_GOAWAY unexpected error code: %d", errorCodeInt);
         } else {
            ByteString debugData = ByteString.EMPTY;
            if (opaqueDataLength > 0) {
               debugData = this.source.readByteString((long)opaqueDataLength);
            }

            handler.goAway(lastStreamId, errorCode, debugData);
         }
      }
   }

   private void readWindowUpdate(Http2Reader.Handler handler, int length, byte flags, int streamId) throws IOException {
      if (length != 4) {
         throw Http2.ioException("TYPE_WINDOW_UPDATE length !=4: %s", length);
      } else {
         long increment = (long)this.source.readInt() & 2147483647L;
         if (increment == 0L) {
            throw Http2.ioException("windowSizeIncrement was 0", increment);
         } else {
            handler.windowUpdate(streamId, increment);
         }
      }
   }

   public void close() throws IOException {
      this.source.close();
   }

   static int readMedium(BufferedSource source) throws IOException {
      return (source.readByte() & 255) << 16 | (source.readByte() & 255) << 8 | source.readByte() & 255;
   }

   static int lengthWithoutPadding(int length, byte flags, short padding) throws IOException {
      if ((flags & 8) != 0) {
         --length;
      }

      if (padding > length) {
         throw Http2.ioException("PROTOCOL_ERROR padding %s > remaining length %s", padding, length);
      } else {
         return (short)(length - padding);
      }
   }

   interface Handler {
      void data(boolean var1, int var2, BufferedSource var3, int var4) throws IOException;

      void headers(boolean var1, int var2, int var3, List<Header> var4);

      void rstStream(int var1, ErrorCode var2);

      void settings(boolean var1, Settings var2);

      void ackSettings();

      void ping(boolean var1, int var2, int var3);

      void goAway(int var1, ErrorCode var2, ByteString var3);

      void windowUpdate(int var1, long var2);

      void priority(int var1, int var2, int var3, boolean var4);

      void pushPromise(int var1, int var2, List<Header> var3) throws IOException;

      void alternateService(int var1, String var2, ByteString var3, String var4, int var5, long var6);
   }

   static final class ContinuationSource implements Source {
      private final BufferedSource source;
      int length;
      byte flags;
      int streamId;
      int left;
      short padding;

      ContinuationSource(BufferedSource source) {
         this.source = source;
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         while(this.left == 0) {
            this.source.skip((long)this.padding);
            this.padding = 0;
            if ((this.flags & 4) != 0) {
               return -1L;
            }

            this.readContinuationHeader();
         }

         long read = this.source.read(sink, Math.min(byteCount, (long)this.left));
         if (read == -1L) {
            return -1L;
         } else {
            this.left = (int)((long)this.left - read);
            return read;
         }
      }

      public Timeout timeout() {
         return this.source.timeout();
      }

      public void close() throws IOException {
      }

      private void readContinuationHeader() throws IOException {
         int previousStreamId = this.streamId;
         this.length = this.left = Http2Reader.readMedium(this.source);
         byte type = (byte)(this.source.readByte() & 255);
         this.flags = (byte)(this.source.readByte() & 255);
         if (Http2Reader.logger.isLoggable(Level.FINE)) {
            Http2Reader.logger.fine(Http2.frameLog(true, this.streamId, this.length, type, this.flags));
         }

         this.streamId = this.source.readInt() & Integer.MAX_VALUE;
         if (type != 9) {
            throw Http2.ioException("%s != TYPE_CONTINUATION", type);
         } else if (this.streamId != previousStreamId) {
            throw Http2.ioException("TYPE_CONTINUATION streamId changed");
         }
      }
   }
}

package okhttp3.internal.http2;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;

final class Http2Writer implements Closeable {
   private static final Logger logger = Logger.getLogger(Http2.class.getName());
   private final BufferedSink sink;
   private final boolean client;
   private final Buffer hpackBuffer;
   private int maxFrameSize;
   private boolean closed;
   final Hpack.Writer hpackWriter;

   Http2Writer(BufferedSink sink, boolean client) {
      this.sink = sink;
      this.client = client;
      this.hpackBuffer = new Buffer();
      this.hpackWriter = new Hpack.Writer(this.hpackBuffer);
      this.maxFrameSize = 16384;
   }

   public synchronized void connectionPreface() throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else if (this.client) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(Util.format(">> CONNECTION %s", Http2.CONNECTION_PREFACE.hex()));
         }

         this.sink.write(Http2.CONNECTION_PREFACE.toByteArray());
         this.sink.flush();
      }
   }

   public synchronized void applyAndAckSettings(Settings peerSettings) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         this.maxFrameSize = peerSettings.getMaxFrameSize(this.maxFrameSize);
         if (peerSettings.getHeaderTableSize() != -1) {
            this.hpackWriter.setHeaderTableSizeSetting(peerSettings.getHeaderTableSize());
         }

         int length = 0;
         byte type = 4;
         byte flags = 1;
         int streamId = 0;
         this.frameHeader(streamId, length, type, flags);
         this.sink.flush();
      }
   }

   public synchronized void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         this.hpackWriter.writeHeaders(requestHeaders);
         long byteCount = this.hpackBuffer.size();
         int length = (int)Math.min((long)(this.maxFrameSize - 4), byteCount);
         byte type = 5;
         byte flags = byteCount == (long)length ? 4 : 0;
         this.frameHeader(streamId, length + 4, type, (byte)flags);
         this.sink.writeInt(promisedStreamId & Integer.MAX_VALUE);
         this.sink.write(this.hpackBuffer, (long)length);
         if (byteCount > (long)length) {
            this.writeContinuationFrames(streamId, byteCount - (long)length);
         }

      }
   }

   public synchronized void flush() throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         this.sink.flush();
      }
   }

   public synchronized void rstStream(int streamId, ErrorCode errorCode) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else if (errorCode.httpCode == -1) {
         throw new IllegalArgumentException();
      } else {
         int length = 4;
         byte type = 3;
         byte flags = 0;
         this.frameHeader(streamId, length, type, flags);
         this.sink.writeInt(errorCode.httpCode);
         this.sink.flush();
      }
   }

   public int maxDataLength() {
      return this.maxFrameSize;
   }

   public synchronized void data(boolean outFinished, int streamId, Buffer source, int byteCount) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         byte flags = 0;
         if (outFinished) {
            flags = (byte)(flags | 1);
         }

         this.dataFrame(streamId, flags, source, byteCount);
      }
   }

   void dataFrame(int streamId, byte flags, Buffer buffer, int byteCount) throws IOException {
      byte type = 0;
      this.frameHeader(streamId, byteCount, type, flags);
      if (byteCount > 0) {
         this.sink.write(buffer, (long)byteCount);
      }

   }

   public synchronized void settings(Settings settings) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         int length = settings.size() * 6;
         byte type = 4;
         byte flags = 0;
         int streamId = 0;
         this.frameHeader(streamId, length, type, flags);

         for(int i = 0; i < 10; ++i) {
            if (settings.isSet(i)) {
               int id = i;
               if (i == 4) {
                  id = 3;
               } else if (i == 7) {
                  id = 4;
               }

               this.sink.writeShort(id);
               this.sink.writeInt(settings.get(i));
            }
         }

         this.sink.flush();
      }
   }

   public synchronized void ping(boolean ack, int payload1, int payload2) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         int length = 8;
         byte type = 6;
         byte flags = ack ? 1 : 0;
         int streamId = 0;
         this.frameHeader(streamId, length, type, (byte)flags);
         this.sink.writeInt(payload1);
         this.sink.writeInt(payload2);
         this.sink.flush();
      }
   }

   public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode, byte[] debugData) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else if (errorCode.httpCode == -1) {
         throw Http2.illegalArgument("errorCode.httpCode == -1");
      } else {
         int length = 8 + debugData.length;
         byte type = 7;
         byte flags = 0;
         int streamId = 0;
         this.frameHeader(streamId, length, type, flags);
         this.sink.writeInt(lastGoodStreamId);
         this.sink.writeInt(errorCode.httpCode);
         if (debugData.length > 0) {
            this.sink.write(debugData);
         }

         this.sink.flush();
      }
   }

   public synchronized void windowUpdate(int streamId, long windowSizeIncrement) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else if (windowSizeIncrement != 0L && windowSizeIncrement <= 2147483647L) {
         int length = 4;
         byte type = 8;
         byte flags = 0;
         this.frameHeader(streamId, length, type, flags);
         this.sink.writeInt((int)windowSizeIncrement);
         this.sink.flush();
      } else {
         throw Http2.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s", windowSizeIncrement);
      }
   }

   public void frameHeader(int streamId, int length, byte type, byte flags) throws IOException {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(Http2.frameLog(false, streamId, length, type, flags));
      }

      if (length > this.maxFrameSize) {
         throw Http2.illegalArgument("FRAME_SIZE_ERROR length > %d: %d", this.maxFrameSize, length);
      } else if ((streamId & Integer.MIN_VALUE) != 0) {
         throw Http2.illegalArgument("reserved bit set: %s", streamId);
      } else {
         writeMedium(this.sink, length);
         this.sink.writeByte(type & 255);
         this.sink.writeByte(flags & 255);
         this.sink.writeInt(streamId & Integer.MAX_VALUE);
      }
   }

   public synchronized void close() throws IOException {
      this.closed = true;
      this.sink.close();
   }

   private static void writeMedium(BufferedSink sink, int i) throws IOException {
      sink.writeByte(i >>> 16 & 255);
      sink.writeByte(i >>> 8 & 255);
      sink.writeByte(i & 255);
   }

   private void writeContinuationFrames(int streamId, long byteCount) throws IOException {
      while(byteCount > 0L) {
         int length = (int)Math.min((long)this.maxFrameSize, byteCount);
         byteCount -= (long)length;
         this.frameHeader(streamId, length, (byte)9, (byte)(byteCount == 0L ? 4 : 0));
         this.sink.write(this.hpackBuffer, (long)length);
      }

   }

   public synchronized void headers(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException {
      if (this.closed) {
         throw new IOException("closed");
      } else {
         this.hpackWriter.writeHeaders(headerBlock);
         long byteCount = this.hpackBuffer.size();
         int length = (int)Math.min((long)this.maxFrameSize, byteCount);
         byte type = 1;
         byte flags = byteCount == (long)length ? 4 : 0;
         if (outFinished) {
            flags = (byte)(flags | 1);
         }

         this.frameHeader(streamId, length, type, (byte)flags);
         this.sink.write(this.hpackBuffer, (long)length);
         if (byteCount > (long)length) {
            this.writeContinuationFrames(streamId, byteCount - (long)length);
         }

      }
   }
}

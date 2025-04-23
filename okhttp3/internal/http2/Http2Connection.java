package okhttp3.internal.http2;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.internal.NamedRunnable;
import okhttp3.internal.Util;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

public final class Http2Connection implements Closeable {
   static final int OKHTTP_CLIENT_WINDOW_SIZE = 16777216;
   private static final ExecutorService listenerExecutor;
   final boolean client;
   final Http2Connection.Listener listener;
   final Map<Integer, Http2Stream> streams = new LinkedHashMap();
   final String connectionName;
   int lastGoodStreamId;
   int nextStreamId;
   boolean shutdown;
   private final ScheduledExecutorService writerExecutor;
   private final ExecutorService pushExecutor;
   final PushObserver pushObserver;
   private boolean awaitingPong;
   long unacknowledgedBytesRead = 0L;
   long bytesLeftInWriteWindow;
   Settings okHttpSettings = new Settings();
   final Settings peerSettings = new Settings();
   boolean receivedInitialPeerSettings = false;
   final Socket socket;
   final Http2Writer writer;
   final Http2Connection.ReaderRunnable readerRunnable;
   final Set<Integer> currentPushRequests = new LinkedHashSet();

   Http2Connection(Http2Connection.Builder builder) {
      this.pushObserver = builder.pushObserver;
      this.client = builder.client;
      this.listener = builder.listener;
      this.nextStreamId = builder.client ? 1 : 2;
      if (builder.client) {
         this.nextStreamId += 2;
      }

      if (builder.client) {
         this.okHttpSettings.set(7, 16777216);
      }

      this.connectionName = builder.connectionName;
      this.writerExecutor = new ScheduledThreadPoolExecutor(1, Util.threadFactory(Util.format("OkHttp %s Writer", this.connectionName), false));
      if (builder.pingIntervalMillis != 0) {
         this.writerExecutor.scheduleAtFixedRate(new Http2Connection.PingRunnable(false, 0, 0), (long)builder.pingIntervalMillis, (long)builder.pingIntervalMillis, TimeUnit.MILLISECONDS);
      }

      this.pushExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory(Util.format("OkHttp %s Push Observer", this.connectionName), true));
      this.peerSettings.set(7, 65535);
      this.peerSettings.set(5, 16384);
      this.bytesLeftInWriteWindow = (long)this.peerSettings.getInitialWindowSize();
      this.socket = builder.socket;
      this.writer = new Http2Writer(builder.sink, this.client);
      this.readerRunnable = new Http2Connection.ReaderRunnable(new Http2Reader(builder.source, this.client));
   }

   public synchronized int openStreamCount() {
      return this.streams.size();
   }

   synchronized Http2Stream getStream(int id) {
      return (Http2Stream)this.streams.get(id);
   }

   synchronized Http2Stream removeStream(int streamId) {
      Http2Stream stream = (Http2Stream)this.streams.remove(streamId);
      this.notifyAll();
      return stream;
   }

   public synchronized int maxConcurrentStreams() {
      return this.peerSettings.getMaxConcurrentStreams(Integer.MAX_VALUE);
   }

   synchronized void updateConnectionFlowControl(long read) {
      this.unacknowledgedBytesRead += read;
      if (this.unacknowledgedBytesRead >= (long)(this.okHttpSettings.getInitialWindowSize() / 2)) {
         this.writeWindowUpdateLater(0, this.unacknowledgedBytesRead);
         this.unacknowledgedBytesRead = 0L;
      }

   }

   public Http2Stream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
      if (this.client) {
         throw new IllegalStateException("Client cannot push requests.");
      } else {
         return this.newStream(associatedStreamId, requestHeaders, out);
      }
   }

   public Http2Stream newStream(List<Header> requestHeaders, boolean out) throws IOException {
      return this.newStream(0, requestHeaders, out);
   }

   private Http2Stream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
      boolean outFinished = !out;
      boolean inFinished = false;
      boolean flushHeaders;
      Http2Stream stream;
      synchronized(this.writer) {
         int streamId;
         synchronized(this) {
            if (this.nextStreamId > 1073741823) {
               this.shutdown(ErrorCode.REFUSED_STREAM);
            }

            if (this.shutdown) {
               throw new ConnectionShutdownException();
            }

            streamId = this.nextStreamId;
            this.nextStreamId += 2;
            stream = new Http2Stream(streamId, this, outFinished, inFinished, (Headers)null);
            flushHeaders = !out || this.bytesLeftInWriteWindow == 0L || stream.bytesLeftInWriteWindow == 0L;
            if (stream.isOpen()) {
               this.streams.put(streamId, stream);
            }
         }

         if (associatedStreamId == 0) {
            this.writer.headers(outFinished, streamId, requestHeaders);
         } else {
            if (this.client) {
               throw new IllegalArgumentException("client streams shouldn't have associated stream IDs");
            }

            this.writer.pushPromise(associatedStreamId, streamId, requestHeaders);
         }
      }

      if (flushHeaders) {
         this.writer.flush();
      }

      return stream;
   }

   void writeHeaders(int streamId, boolean outFinished, List<Header> alternating) throws IOException {
      this.writer.headers(outFinished, streamId, alternating);
   }

   public void writeData(int streamId, boolean outFinished, Buffer buffer, long byteCount) throws IOException {
      if (byteCount == 0L) {
         this.writer.data(outFinished, streamId, buffer, 0);
      } else {
         while(byteCount > 0L) {
            int toWrite;
            synchronized(this) {
               try {
                  while(this.bytesLeftInWriteWindow <= 0L) {
                     if (!this.streams.containsKey(streamId)) {
                        throw new IOException("stream closed");
                     }

                     this.wait();
                  }
               } catch (InterruptedException var10) {
                  Thread.currentThread().interrupt();
                  throw new InterruptedIOException();
               }

               toWrite = (int)Math.min(byteCount, this.bytesLeftInWriteWindow);
               toWrite = Math.min(toWrite, this.writer.maxDataLength());
               this.bytesLeftInWriteWindow -= (long)toWrite;
            }

            byteCount -= (long)toWrite;
            this.writer.data(outFinished && byteCount == 0L, streamId, buffer, toWrite);
         }

      }
   }

   void writeSynResetLater(final int streamId, final ErrorCode errorCode) {
      try {
         this.writerExecutor.execute(new NamedRunnable("OkHttp %s stream %d", new Object[]{this.connectionName, streamId}) {
            public void execute() {
               try {
                  Http2Connection.this.writeSynReset(streamId, errorCode);
               } catch (IOException var2) {
                  Http2Connection.this.failConnection();
               }

            }
         });
      } catch (RejectedExecutionException var4) {
      }

   }

   void writeSynReset(int streamId, ErrorCode statusCode) throws IOException {
      this.writer.rstStream(streamId, statusCode);
   }

   void writeWindowUpdateLater(final int streamId, final long unacknowledgedBytesRead) {
      try {
         this.writerExecutor.execute(new NamedRunnable("OkHttp Window Update %s stream %d", new Object[]{this.connectionName, streamId}) {
            public void execute() {
               try {
                  Http2Connection.this.writer.windowUpdate(streamId, unacknowledgedBytesRead);
               } catch (IOException var2) {
                  Http2Connection.this.failConnection();
               }

            }
         });
      } catch (RejectedExecutionException var5) {
      }

   }

   void writePing(boolean reply, int payload1, int payload2) {
      if (!reply) {
         boolean failedDueToMissingPong;
         synchronized(this) {
            failedDueToMissingPong = this.awaitingPong;
            this.awaitingPong = true;
         }

         if (failedDueToMissingPong) {
            this.failConnection();
            return;
         }
      }

      try {
         this.writer.ping(reply, payload1, payload2);
      } catch (IOException var7) {
         this.failConnection();
      }

   }

   void writePingAndAwaitPong() throws InterruptedException {
      this.writePing(false, 1330343787, -257978967);
      this.awaitPong();
   }

   synchronized void awaitPong() throws InterruptedException {
      while(this.awaitingPong) {
         this.wait();
      }

   }

   public void flush() throws IOException {
      this.writer.flush();
   }

   public void shutdown(ErrorCode statusCode) throws IOException {
      synchronized(this.writer) {
         int lastGoodStreamId;
         synchronized(this) {
            if (this.shutdown) {
               return;
            }

            this.shutdown = true;
            lastGoodStreamId = this.lastGoodStreamId;
         }

         this.writer.goAway(lastGoodStreamId, statusCode, Util.EMPTY_BYTE_ARRAY);
      }
   }

   public void close() throws IOException {
      this.close(ErrorCode.NO_ERROR, ErrorCode.CANCEL);
   }

   void close(ErrorCode connectionCode, ErrorCode streamCode) throws IOException {
      assert !Thread.holdsLock(this);

      IOException thrown = null;

      try {
         this.shutdown(connectionCode);
      } catch (IOException var11) {
         thrown = var11;
      }

      Http2Stream[] streamsToClose = null;
      synchronized(this) {
         if (!this.streams.isEmpty()) {
            streamsToClose = (Http2Stream[])this.streams.values().toArray(new Http2Stream[this.streams.size()]);
            this.streams.clear();
         }
      }

      if (streamsToClose != null) {
         Http2Stream[] var5 = streamsToClose;
         int var6 = streamsToClose.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            Http2Stream stream = var5[var7];

            try {
               stream.close(streamCode);
            } catch (IOException var13) {
               if (thrown != null) {
                  thrown = var13;
               }
            }
         }
      }

      try {
         this.writer.close();
      } catch (IOException var12) {
         if (thrown == null) {
            thrown = var12;
         }
      }

      try {
         this.socket.close();
      } catch (IOException var10) {
         thrown = var10;
      }

      this.writerExecutor.shutdown();
      this.pushExecutor.shutdown();
      if (thrown != null) {
         throw thrown;
      }
   }

   private void failConnection() {
      try {
         this.close(ErrorCode.PROTOCOL_ERROR, ErrorCode.PROTOCOL_ERROR);
      } catch (IOException var2) {
      }

   }

   public void start() throws IOException {
      this.start(true);
   }

   void start(boolean sendConnectionPreface) throws IOException {
      if (sendConnectionPreface) {
         this.writer.connectionPreface();
         this.writer.settings(this.okHttpSettings);
         int windowSize = this.okHttpSettings.getInitialWindowSize();
         if (windowSize != 65535) {
            this.writer.windowUpdate(0, (long)(windowSize - '\uffff'));
         }
      }

      (new Thread(this.readerRunnable)).start();
   }

   public void setSettings(Settings settings) throws IOException {
      synchronized(this.writer) {
         synchronized(this) {
            if (this.shutdown) {
               throw new ConnectionShutdownException();
            }

            this.okHttpSettings.merge(settings);
         }

         this.writer.settings(settings);
      }
   }

   public synchronized boolean isShutdown() {
      return this.shutdown;
   }

   boolean pushedStream(int streamId) {
      return streamId != 0 && (streamId & 1) == 0;
   }

   void pushRequestLater(final int streamId, final List<Header> requestHeaders) {
      synchronized(this) {
         if (this.currentPushRequests.contains(streamId)) {
            this.writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
            return;
         }

         this.currentPushRequests.add(streamId);
      }

      try {
         this.pushExecutorExecute(new NamedRunnable("OkHttp %s Push Request[%s]", new Object[]{this.connectionName, streamId}) {
            public void execute() {
               boolean cancel = Http2Connection.this.pushObserver.onRequest(streamId, requestHeaders);

               try {
                  if (cancel) {
                     Http2Connection.this.writer.rstStream(streamId, ErrorCode.CANCEL);
                     synchronized(Http2Connection.this) {
                        Http2Connection.this.currentPushRequests.remove(streamId);
                     }
                  }
               } catch (IOException var5) {
               }

            }
         });
      } catch (RejectedExecutionException var5) {
      }

   }

   void pushHeadersLater(final int streamId, final List<Header> requestHeaders, final boolean inFinished) {
      try {
         this.pushExecutorExecute(new NamedRunnable("OkHttp %s Push Headers[%s]", new Object[]{this.connectionName, streamId}) {
            public void execute() {
               boolean cancel = Http2Connection.this.pushObserver.onHeaders(streamId, requestHeaders, inFinished);

               try {
                  if (cancel) {
                     Http2Connection.this.writer.rstStream(streamId, ErrorCode.CANCEL);
                  }

                  if (cancel || inFinished) {
                     synchronized(Http2Connection.this) {
                        Http2Connection.this.currentPushRequests.remove(streamId);
                     }
                  }
               } catch (IOException var5) {
               }

            }
         });
      } catch (RejectedExecutionException var5) {
      }

   }

   void pushDataLater(final int streamId, BufferedSource source, final int byteCount, final boolean inFinished) throws IOException {
      final Buffer buffer = new Buffer();
      source.require((long)byteCount);
      source.read(buffer, (long)byteCount);
      if (buffer.size() != (long)byteCount) {
         throw new IOException(buffer.size() + " != " + byteCount);
      } else {
         this.pushExecutorExecute(new NamedRunnable("OkHttp %s Push Data[%s]", new Object[]{this.connectionName, streamId}) {
            public void execute() {
               try {
                  boolean cancel = Http2Connection.this.pushObserver.onData(streamId, buffer, byteCount, inFinished);
                  if (cancel) {
                     Http2Connection.this.writer.rstStream(streamId, ErrorCode.CANCEL);
                  }

                  if (cancel || inFinished) {
                     synchronized(Http2Connection.this) {
                        Http2Connection.this.currentPushRequests.remove(streamId);
                     }
                  }
               } catch (IOException var5) {
               }

            }
         });
      }
   }

   void pushResetLater(final int streamId, final ErrorCode errorCode) {
      this.pushExecutorExecute(new NamedRunnable("OkHttp %s Push Reset[%s]", new Object[]{this.connectionName, streamId}) {
         public void execute() {
            Http2Connection.this.pushObserver.onReset(streamId, errorCode);
            synchronized(Http2Connection.this) {
               Http2Connection.this.currentPushRequests.remove(streamId);
            }
         }
      });
   }

   private synchronized void pushExecutorExecute(NamedRunnable namedRunnable) {
      if (!this.isShutdown()) {
         this.pushExecutor.execute(namedRunnable);
      }

   }

   static {
      listenerExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp Http2Connection", true));
   }

   public abstract static class Listener {
      public static final Http2Connection.Listener REFUSE_INCOMING_STREAMS = new Http2Connection.Listener() {
         public void onStream(Http2Stream stream) throws IOException {
            stream.close(ErrorCode.REFUSED_STREAM);
         }
      };

      public abstract void onStream(Http2Stream var1) throws IOException;

      public void onSettings(Http2Connection connection) {
      }
   }

   class ReaderRunnable extends NamedRunnable implements Http2Reader.Handler {
      final Http2Reader reader;

      ReaderRunnable(Http2Reader reader) {
         super("OkHttp %s", Http2Connection.this.connectionName);
         this.reader = reader;
      }

      protected void execute() {
         ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
         ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;

         try {
            this.reader.readConnectionPreface(this);

            while(true) {
               if (!this.reader.nextFrame(false, this)) {
                  connectionErrorCode = ErrorCode.NO_ERROR;
                  streamErrorCode = ErrorCode.CANCEL;
                  break;
               }
            }
         } catch (IOException var12) {
            connectionErrorCode = ErrorCode.PROTOCOL_ERROR;
            streamErrorCode = ErrorCode.PROTOCOL_ERROR;
         } finally {
            try {
               Http2Connection.this.close(connectionErrorCode, streamErrorCode);
            } catch (IOException var11) {
            }

            Util.closeQuietly((Closeable)this.reader);
         }

      }

      public void data(boolean inFinished, int streamId, BufferedSource source, int length) throws IOException {
         if (Http2Connection.this.pushedStream(streamId)) {
            Http2Connection.this.pushDataLater(streamId, source, length, inFinished);
         } else {
            Http2Stream dataStream = Http2Connection.this.getStream(streamId);
            if (dataStream == null) {
               Http2Connection.this.writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
               Http2Connection.this.updateConnectionFlowControl((long)length);
               source.skip((long)length);
            } else {
               dataStream.receiveData(source, length);
               if (inFinished) {
                  dataStream.receiveHeaders(Util.EMPTY_HEADERS, true);
               }

            }
         }
      }

      public void headers(boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock) {
         if (Http2Connection.this.pushedStream(streamId)) {
            Http2Connection.this.pushHeadersLater(streamId, headerBlock, inFinished);
         } else {
            Http2Stream stream;
            synchronized(Http2Connection.this) {
               stream = Http2Connection.this.getStream(streamId);
               if (stream == null) {
                  if (Http2Connection.this.shutdown) {
                     return;
                  }

                  if (streamId <= Http2Connection.this.lastGoodStreamId) {
                     return;
                  }

                  if (streamId % 2 == Http2Connection.this.nextStreamId % 2) {
                     return;
                  }

                  Headers headers = Util.toHeaders(headerBlock);
                  final Http2Stream newStream = new Http2Stream(streamId, Http2Connection.this, false, inFinished, headers);
                  Http2Connection.this.lastGoodStreamId = streamId;
                  Http2Connection.this.streams.put(streamId, newStream);
                  Http2Connection.listenerExecutor.execute(new NamedRunnable("OkHttp %s stream %d", new Object[]{Http2Connection.this.connectionName, streamId}) {
                     public void execute() {
                        try {
                           Http2Connection.this.listener.onStream(newStream);
                        } catch (IOException var4) {
                           Platform.get().log(4, "Http2Connection.Listener failure for " + Http2Connection.this.connectionName, var4);

                           try {
                              newStream.close(ErrorCode.PROTOCOL_ERROR);
                           } catch (IOException var3) {
                           }
                        }

                     }
                  });
                  return;
               }
            }

            stream.receiveHeaders(Util.toHeaders(headerBlock), inFinished);
         }
      }

      public void rstStream(int streamId, ErrorCode errorCode) {
         if (Http2Connection.this.pushedStream(streamId)) {
            Http2Connection.this.pushResetLater(streamId, errorCode);
         } else {
            Http2Stream rstStream = Http2Connection.this.removeStream(streamId);
            if (rstStream != null) {
               rstStream.receiveRstStream(errorCode);
            }

         }
      }

      public void settings(boolean clearPrevious, Settings newSettings) {
         long delta = 0L;
         Http2Stream[] streamsToNotify = null;
         int priorWriteWindowSize;
         int peerInitialWindowSize;
         synchronized(Http2Connection.this) {
            priorWriteWindowSize = Http2Connection.this.peerSettings.getInitialWindowSize();
            if (clearPrevious) {
               Http2Connection.this.peerSettings.clear();
            }

            Http2Connection.this.peerSettings.merge(newSettings);
            this.applyAndAckSettings(newSettings);
            peerInitialWindowSize = Http2Connection.this.peerSettings.getInitialWindowSize();
            if (peerInitialWindowSize != -1 && peerInitialWindowSize != priorWriteWindowSize) {
               delta = (long)(peerInitialWindowSize - priorWriteWindowSize);
               if (!Http2Connection.this.receivedInitialPeerSettings) {
                  Http2Connection.this.receivedInitialPeerSettings = true;
               }

               if (!Http2Connection.this.streams.isEmpty()) {
                  streamsToNotify = (Http2Stream[])Http2Connection.this.streams.values().toArray(new Http2Stream[Http2Connection.this.streams.size()]);
               }
            }

            Http2Connection.listenerExecutor.execute(new NamedRunnable("OkHttp %s settings", new Object[]{Http2Connection.this.connectionName}) {
               public void execute() {
                  Http2Connection.this.listener.onSettings(Http2Connection.this);
               }
            });
         }

         if (streamsToNotify != null && delta != 0L) {
            Http2Stream[] var6 = streamsToNotify;
            priorWriteWindowSize = streamsToNotify.length;

            for(peerInitialWindowSize = 0; peerInitialWindowSize < priorWriteWindowSize; ++peerInitialWindowSize) {
               Http2Stream stream = var6[peerInitialWindowSize];
               synchronized(stream) {
                  stream.addBytesToWriteWindow(delta);
               }
            }
         }

      }

      private void applyAndAckSettings(final Settings peerSettings) {
         try {
            Http2Connection.this.writerExecutor.execute(new NamedRunnable("OkHttp %s ACK Settings", new Object[]{Http2Connection.this.connectionName}) {
               public void execute() {
                  try {
                     Http2Connection.this.writer.applyAndAckSettings(peerSettings);
                  } catch (IOException var2) {
                     Http2Connection.this.failConnection();
                  }

               }
            });
         } catch (RejectedExecutionException var3) {
         }

      }

      public void ackSettings() {
      }

      public void ping(boolean reply, int payload1, int payload2) {
         if (reply) {
            synchronized(Http2Connection.this) {
               Http2Connection.this.awaitingPong = false;
               Http2Connection.this.notifyAll();
            }
         } else {
            try {
               Http2Connection.this.writerExecutor.execute(Http2Connection.this.new PingRunnable(true, payload1, payload2));
            } catch (RejectedExecutionException var6) {
            }
         }

      }

      public void goAway(int lastGoodStreamId, ErrorCode errorCode, ByteString debugData) {
         if (debugData.size() > 0) {
         }

         Http2Stream[] streamsCopy;
         synchronized(Http2Connection.this) {
            streamsCopy = (Http2Stream[])Http2Connection.this.streams.values().toArray(new Http2Stream[Http2Connection.this.streams.size()]);
            Http2Connection.this.shutdown = true;
         }

         Http2Stream[] var5 = streamsCopy;
         int var6 = streamsCopy.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            Http2Stream http2Stream = var5[var7];
            if (http2Stream.getId() > lastGoodStreamId && http2Stream.isLocallyInitiated()) {
               http2Stream.receiveRstStream(ErrorCode.REFUSED_STREAM);
               Http2Connection.this.removeStream(http2Stream.getId());
            }
         }

      }

      public void windowUpdate(int streamId, long windowSizeIncrement) {
         if (streamId == 0) {
            synchronized(Http2Connection.this) {
               Http2Connection var10000 = Http2Connection.this;
               var10000.bytesLeftInWriteWindow += windowSizeIncrement;
               Http2Connection.this.notifyAll();
            }
         } else {
            Http2Stream stream = Http2Connection.this.getStream(streamId);
            if (stream != null) {
               synchronized(stream) {
                  stream.addBytesToWriteWindow(windowSizeIncrement);
               }
            }
         }

      }

      public void priority(int streamId, int streamDependency, int weight, boolean exclusive) {
      }

      public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) {
         Http2Connection.this.pushRequestLater(promisedStreamId, requestHeaders);
      }

      public void alternateService(int streamId, String origin, ByteString protocol, String host, int port, long maxAge) {
      }
   }

   public static class Builder {
      Socket socket;
      String connectionName;
      BufferedSource source;
      BufferedSink sink;
      Http2Connection.Listener listener;
      PushObserver pushObserver;
      boolean client;
      int pingIntervalMillis;

      public Builder(boolean client) {
         this.listener = Http2Connection.Listener.REFUSE_INCOMING_STREAMS;
         this.pushObserver = PushObserver.CANCEL;
         this.client = client;
      }

      public Http2Connection.Builder socket(Socket socket) throws IOException {
         SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
         String connectionName = remoteSocketAddress instanceof InetSocketAddress ? ((InetSocketAddress)remoteSocketAddress).getHostName() : remoteSocketAddress.toString();
         return this.socket(socket, connectionName, Okio.buffer(Okio.source(socket)), Okio.buffer(Okio.sink(socket)));
      }

      public Http2Connection.Builder socket(Socket socket, String connectionName, BufferedSource source, BufferedSink sink) {
         this.socket = socket;
         this.connectionName = connectionName;
         this.source = source;
         this.sink = sink;
         return this;
      }

      public Http2Connection.Builder listener(Http2Connection.Listener listener) {
         this.listener = listener;
         return this;
      }

      public Http2Connection.Builder pushObserver(PushObserver pushObserver) {
         this.pushObserver = pushObserver;
         return this;
      }

      public Http2Connection.Builder pingIntervalMillis(int pingIntervalMillis) {
         this.pingIntervalMillis = pingIntervalMillis;
         return this;
      }

      public Http2Connection build() {
         return new Http2Connection(this);
      }
   }

   final class PingRunnable extends NamedRunnable {
      final boolean reply;
      final int payload1;
      final int payload2;

      PingRunnable(boolean reply, int payload1, int payload2) {
         super("OkHttp %s ping %08x%08x", Http2Connection.this.connectionName, payload1, payload2);
         this.reply = reply;
         this.payload1 = payload1;
         this.payload2 = payload2;
      }

      public void execute() {
         Http2Connection.this.writePing(this.reply, this.payload1, this.payload2);
      }
   }
}

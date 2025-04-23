package okhttp3.internal.http1;

import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.StreamAllocation;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.RealResponseBody;
import okhttp3.internal.http.RequestLine;
import okhttp3.internal.http.StatusLine;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingTimeout;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

public final class Http1Codec implements HttpCodec {
   private static final int STATE_IDLE = 0;
   private static final int STATE_OPEN_REQUEST_BODY = 1;
   private static final int STATE_WRITING_REQUEST_BODY = 2;
   private static final int STATE_READ_RESPONSE_HEADERS = 3;
   private static final int STATE_OPEN_RESPONSE_BODY = 4;
   private static final int STATE_READING_RESPONSE_BODY = 5;
   private static final int STATE_CLOSED = 6;
   private static final int HEADER_LIMIT = 262144;
   final OkHttpClient client;
   final StreamAllocation streamAllocation;
   final BufferedSource source;
   final BufferedSink sink;
   int state = 0;
   private long headerLimit = 262144L;
   private Headers trailers;

   public Http1Codec(OkHttpClient client, StreamAllocation streamAllocation, BufferedSource source, BufferedSink sink) {
      this.client = client;
      this.streamAllocation = streamAllocation;
      this.source = source;
      this.sink = sink;
   }

   public Sink createRequestBody(Request request, long contentLength) {
      if ("chunked".equalsIgnoreCase(request.header("Transfer-Encoding"))) {
         return this.newChunkedSink();
      } else if (contentLength != -1L) {
         return this.newFixedLengthSink(contentLength);
      } else {
         throw new IllegalStateException("Cannot stream a request body without chunked encoding or a known content length!");
      }
   }

   public void cancel() {
      RealConnection connection = this.streamAllocation.connection();
      if (connection != null) {
         connection.cancel();
      }

   }

   public void writeRequestHeaders(Request request) throws IOException {
      String requestLine = RequestLine.get(request, this.streamAllocation.connection().route().proxy().type());
      this.writeRequest(request.headers(), requestLine);
   }

   public ResponseBody openResponseBody(Response response) throws IOException {
      this.streamAllocation.eventListener.responseBodyStart(this.streamAllocation.call);
      String contentType = response.header("Content-Type");
      Source source;
      if (!HttpHeaders.hasBody(response)) {
         source = this.newFixedLengthSource(0L);
         return new RealResponseBody(contentType, 0L, Okio.buffer(source));
      } else if ("chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
         source = this.newChunkedSource(response.request().url());
         return new RealResponseBody(contentType, -1L, Okio.buffer(source));
      } else {
         long contentLength = HttpHeaders.contentLength(response);
         if (contentLength != -1L) {
            Source source = this.newFixedLengthSource(contentLength);
            return new RealResponseBody(contentType, contentLength, Okio.buffer(source));
         } else {
            return new RealResponseBody(contentType, -1L, Okio.buffer(this.newUnknownLengthSource()));
         }
      }
   }

   public Headers trailers() throws IOException {
      if (this.state != 6) {
         throw new IllegalStateException("too early; can't read the trailers yet");
      } else {
         return this.trailers != null ? this.trailers : Util.EMPTY_HEADERS;
      }
   }

   public boolean isClosed() {
      return this.state == 6;
   }

   public void flushRequest() throws IOException {
      this.sink.flush();
   }

   public void finishRequest() throws IOException {
      this.sink.flush();
   }

   public void writeRequest(Headers headers, String requestLine) throws IOException {
      if (this.state != 0) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.sink.writeUtf8(requestLine).writeUtf8("\r\n");
         int i = 0;

         for(int size = headers.size(); i < size; ++i) {
            this.sink.writeUtf8(headers.name(i)).writeUtf8(": ").writeUtf8(headers.value(i)).writeUtf8("\r\n");
         }

         this.sink.writeUtf8("\r\n");
         this.state = 1;
      }
   }

   public Response.Builder readResponseHeaders(boolean expectContinue) throws IOException {
      if (this.state != 1 && this.state != 3) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         try {
            StatusLine statusLine = StatusLine.parse(this.readHeaderLine());
            Response.Builder responseBuilder = (new Response.Builder()).protocol(statusLine.protocol).code(statusLine.code).message(statusLine.message).headers(this.readHeaders());
            if (expectContinue && statusLine.code == 100) {
               return null;
            } else if (statusLine.code == 100) {
               this.state = 3;
               return responseBuilder;
            } else {
               this.state = 4;
               return responseBuilder;
            }
         } catch (EOFException var4) {
            throw new IOException("unexpected end of stream on " + this.streamAllocation, var4);
         }
      }
   }

   private String readHeaderLine() throws IOException {
      String line = this.source.readUtf8LineStrict(this.headerLimit);
      this.headerLimit -= (long)line.length();
      return line;
   }

   public Headers readHeaders() throws IOException {
      Headers.Builder headers = new Headers.Builder();

      String line;
      while((line = this.readHeaderLine()).length() != 0) {
         Internal.instance.addLenient(headers, line);
      }

      return headers.build();
   }

   public Sink newChunkedSink() {
      if (this.state != 1) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 2;
         return new Http1Codec.ChunkedSink();
      }
   }

   public Sink newFixedLengthSink(long contentLength) {
      if (this.state != 1) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 2;
         return new Http1Codec.FixedLengthSink(contentLength);
      }
   }

   public Source newFixedLengthSource(long length) throws IOException {
      if (this.state != 4) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 5;
         return new Http1Codec.FixedLengthSource(length);
      }
   }

   public Source newChunkedSource(HttpUrl url) throws IOException {
      if (this.state != 4) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 5;
         return new Http1Codec.ChunkedSource(url);
      }
   }

   public Source newUnknownLengthSource() throws IOException {
      if (this.state != 4) {
         throw new IllegalStateException("state: " + this.state);
      } else if (this.streamAllocation == null) {
         throw new IllegalStateException("streamAllocation == null");
      } else {
         this.state = 5;
         this.streamAllocation.noNewStreams();
         return new Http1Codec.UnknownLengthSource();
      }
   }

   void detachTimeout(ForwardingTimeout timeout) {
      Timeout oldDelegate = timeout.delegate();
      timeout.setDelegate(Timeout.NONE);
      oldDelegate.clearDeadline();
      oldDelegate.clearTimeout();
   }

   private class UnknownLengthSource extends Http1Codec.AbstractSource {
      private boolean inputExhausted;

      UnknownLengthSource() {
         super(null);
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (this.inputExhausted) {
            return -1L;
         } else {
            long read = super.read(sink, byteCount);
            if (read == -1L) {
               this.inputExhausted = true;
               this.endOfInput(true, (IOException)null);
               return -1L;
            } else {
               return read;
            }
         }
      }

      public void close() throws IOException {
         if (!this.closed) {
            if (!this.inputExhausted) {
               this.endOfInput(false, (IOException)null);
            }

            this.closed = true;
         }
      }
   }

   private class ChunkedSource extends Http1Codec.AbstractSource {
      private static final long NO_CHUNK_YET = -1L;
      private final HttpUrl url;
      private long bytesRemainingInChunk = -1L;
      private boolean hasMoreChunks = true;

      ChunkedSource(HttpUrl url) {
         super(null);
         this.url = url;
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (!this.hasMoreChunks) {
            return -1L;
         } else {
            if (this.bytesRemainingInChunk == 0L || this.bytesRemainingInChunk == -1L) {
               this.readChunkSize();
               if (!this.hasMoreChunks) {
                  return -1L;
               }
            }

            long read = super.read(sink, Math.min(byteCount, this.bytesRemainingInChunk));
            if (read == -1L) {
               ProtocolException e = new ProtocolException("unexpected end of stream");
               this.endOfInput(false, e);
               throw e;
            } else {
               this.bytesRemainingInChunk -= read;
               return read;
            }
         }
      }

      private void readChunkSize() throws IOException {
         if (this.bytesRemainingInChunk != -1L) {
            Http1Codec.this.source.readUtf8LineStrict();
         }

         try {
            this.bytesRemainingInChunk = Http1Codec.this.source.readHexadecimalUnsignedLong();
            String extensions = Http1Codec.this.source.readUtf8LineStrict().trim();
            if (this.bytesRemainingInChunk < 0L || !extensions.isEmpty() && !extensions.startsWith(";")) {
               throw new ProtocolException("expected chunk size and optional extensions but was \"" + this.bytesRemainingInChunk + extensions + "\"");
            }
         } catch (NumberFormatException var2) {
            throw new ProtocolException(var2.getMessage());
         }

         if (this.bytesRemainingInChunk == 0L) {
            this.hasMoreChunks = false;
            Http1Codec.this.trailers = Http1Codec.this.readHeaders();
            HttpHeaders.receiveHeaders(Http1Codec.this.client.cookieJar(), this.url, Http1Codec.this.trailers);
            this.endOfInput(true, (IOException)null);
         }

      }

      public void close() throws IOException {
         if (!this.closed) {
            if (this.hasMoreChunks && !Util.discard(this, 100, TimeUnit.MILLISECONDS)) {
               this.endOfInput(false, (IOException)null);
            }

            this.closed = true;
         }
      }
   }

   private class FixedLengthSource extends Http1Codec.AbstractSource {
      private long bytesRemaining;

      FixedLengthSource(long length) throws IOException {
         super(null);
         this.bytesRemaining = length;
         if (this.bytesRemaining == 0L) {
            this.endOfInput(true, (IOException)null);
         }

      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (this.bytesRemaining == 0L) {
            return -1L;
         } else {
            long read = super.read(sink, Math.min(this.bytesRemaining, byteCount));
            if (read == -1L) {
               ProtocolException e = new ProtocolException("unexpected end of stream");
               this.endOfInput(false, e);
               throw e;
            } else {
               this.bytesRemaining -= read;
               if (this.bytesRemaining == 0L) {
                  this.endOfInput(true, (IOException)null);
               }

               return read;
            }
         }
      }

      public void close() throws IOException {
         if (!this.closed) {
            if (this.bytesRemaining != 0L && !Util.discard(this, 100, TimeUnit.MILLISECONDS)) {
               this.endOfInput(false, (IOException)null);
            }

            this.closed = true;
         }
      }
   }

   private abstract class AbstractSource implements Source {
      protected final ForwardingTimeout timeout;
      protected boolean closed;
      protected long bytesRead;

      private AbstractSource() {
         this.timeout = new ForwardingTimeout(Http1Codec.this.source.timeout());
         this.bytesRead = 0L;
      }

      public Timeout timeout() {
         return this.timeout;
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         try {
            long read = Http1Codec.this.source.read(sink, byteCount);
            if (read > 0L) {
               this.bytesRead += read;
            }

            return read;
         } catch (IOException var6) {
            this.endOfInput(false, var6);
            throw var6;
         }
      }

      protected final void endOfInput(boolean reuseConnection, IOException e) throws IOException {
         if (Http1Codec.this.state != 6) {
            if (Http1Codec.this.state != 5) {
               throw new IllegalStateException("state: " + Http1Codec.this.state);
            } else {
               Http1Codec.this.detachTimeout(this.timeout);
               Http1Codec.this.state = 6;
               if (Http1Codec.this.streamAllocation != null) {
                  Http1Codec.this.streamAllocation.streamFinished(!reuseConnection, Http1Codec.this, this.bytesRead, e);
               }

            }
         }
      }

      // $FF: synthetic method
      AbstractSource(Object x1) {
         this();
      }
   }

   private final class ChunkedSink implements Sink {
      private final ForwardingTimeout timeout;
      private boolean closed;

      ChunkedSink() {
         this.timeout = new ForwardingTimeout(Http1Codec.this.sink.timeout());
      }

      public Timeout timeout() {
         return this.timeout;
      }

      public void write(Buffer source, long byteCount) throws IOException {
         if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (byteCount != 0L) {
            Http1Codec.this.sink.writeHexadecimalUnsignedLong(byteCount);
            Http1Codec.this.sink.writeUtf8("\r\n");
            Http1Codec.this.sink.write(source, byteCount);
            Http1Codec.this.sink.writeUtf8("\r\n");
         }
      }

      public synchronized void flush() throws IOException {
         if (!this.closed) {
            Http1Codec.this.sink.flush();
         }
      }

      public synchronized void close() throws IOException {
         if (!this.closed) {
            this.closed = true;
            Http1Codec.this.sink.writeUtf8("0\r\n\r\n");
            Http1Codec.this.detachTimeout(this.timeout);
            Http1Codec.this.state = 3;
         }
      }
   }

   private final class FixedLengthSink implements Sink {
      private final ForwardingTimeout timeout;
      private boolean closed;
      private long bytesRemaining;

      FixedLengthSink(long bytesRemaining) {
         this.timeout = new ForwardingTimeout(Http1Codec.this.sink.timeout());
         this.bytesRemaining = bytesRemaining;
      }

      public Timeout timeout() {
         return this.timeout;
      }

      public void write(Buffer source, long byteCount) throws IOException {
         if (this.closed) {
            throw new IllegalStateException("closed");
         } else {
            Util.checkOffsetAndCount(source.size(), 0L, byteCount);
            if (byteCount > this.bytesRemaining) {
               throw new ProtocolException("expected " + this.bytesRemaining + " bytes but received " + byteCount);
            } else {
               Http1Codec.this.sink.write(source, byteCount);
               this.bytesRemaining -= byteCount;
            }
         }
      }

      public void flush() throws IOException {
         if (!this.closed) {
            Http1Codec.this.sink.flush();
         }
      }

      public void close() throws IOException {
         if (!this.closed) {
            this.closed = true;
            if (this.bytesRemaining > 0L) {
               throw new ProtocolException("unexpected end of stream");
            } else {
               Http1Codec.this.detachTimeout(this.timeout);
               Http1Codec.this.state = 3;
            }
         }
      }
   }
}

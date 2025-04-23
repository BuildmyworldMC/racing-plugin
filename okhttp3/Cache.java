package okhttp3;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okhttp3.internal.cache.CacheRequest;
import okhttp3.internal.cache.CacheStrategy;
import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.cache.InternalCache;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.http.StatusLine;
import okhttp3.internal.io.FileSystem;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.Okio;
import okio.Sink;
import okio.Source;

public final class Cache implements Closeable, Flushable {
   private static final int VERSION = 201105;
   private static final int ENTRY_METADATA = 0;
   private static final int ENTRY_BODY = 1;
   private static final int ENTRY_COUNT = 2;
   final InternalCache internalCache;
   final DiskLruCache cache;
   int writeSuccessCount;
   int writeAbortCount;
   private int networkCount;
   private int hitCount;
   private int requestCount;

   public Cache(File directory, long maxSize) {
      this(directory, maxSize, FileSystem.SYSTEM);
   }

   Cache(File directory, long maxSize, FileSystem fileSystem) {
      this.internalCache = new InternalCache() {
         @Nullable
         public Response get(Request request) throws IOException {
            return Cache.this.get(request);
         }

         @Nullable
         public CacheRequest put(Response response) throws IOException {
            return Cache.this.put(response);
         }

         public void remove(Request request) throws IOException {
            Cache.this.remove(request);
         }

         public void update(Response cached, Response network) {
            Cache.this.update(cached, network);
         }

         public void trackConditionalCacheHit() {
            Cache.this.trackConditionalCacheHit();
         }

         public void trackResponse(CacheStrategy cacheStrategy) {
            Cache.this.trackResponse(cacheStrategy);
         }
      };
      this.cache = DiskLruCache.create(fileSystem, directory, 201105, 2, maxSize);
   }

   public static String key(HttpUrl url) {
      return ByteString.encodeUtf8(url.toString()).md5().hex();
   }

   @Nullable
   Response get(Request request) {
      String key = key(request.url());

      DiskLruCache.Snapshot snapshot;
      try {
         snapshot = this.cache.get(key);
         if (snapshot == null) {
            return null;
         }
      } catch (IOException var7) {
         return null;
      }

      Cache.Entry entry;
      try {
         entry = new Cache.Entry(snapshot.getSource(0));
      } catch (IOException var6) {
         Util.closeQuietly((Closeable)snapshot);
         return null;
      }

      Response response = entry.response(snapshot);
      if (!entry.matches(request, response)) {
         Util.closeQuietly((Closeable)response.body());
         return null;
      } else {
         return response;
      }
   }

   @Nullable
   CacheRequest put(Response response) {
      String requestMethod = response.request().method();
      if (HttpMethod.invalidatesCache(response.request().method())) {
         try {
            this.remove(response.request());
         } catch (IOException var6) {
         }

         return null;
      } else if (!requestMethod.equals("GET")) {
         return null;
      } else if (HttpHeaders.hasVaryAll(response)) {
         return null;
      } else {
         Cache.Entry entry = new Cache.Entry(response);
         DiskLruCache.Editor editor = null;

         try {
            editor = this.cache.edit(key(response.request().url()));
            if (editor == null) {
               return null;
            } else {
               entry.writeTo(editor);
               return new Cache.CacheRequestImpl(editor);
            }
         } catch (IOException var7) {
            this.abortQuietly(editor);
            return null;
         }
      }
   }

   void remove(Request request) throws IOException {
      this.cache.remove(key(request.url()));
   }

   void update(Response cached, Response network) {
      Cache.Entry entry = new Cache.Entry(network);
      DiskLruCache.Snapshot snapshot = ((Cache.CacheResponseBody)cached.body()).snapshot;
      DiskLruCache.Editor editor = null;

      try {
         editor = snapshot.edit();
         if (editor != null) {
            entry.writeTo(editor);
            editor.commit();
         }
      } catch (IOException var7) {
         this.abortQuietly(editor);
      }

   }

   private void abortQuietly(@Nullable DiskLruCache.Editor editor) {
      try {
         if (editor != null) {
            editor.abort();
         }
      } catch (IOException var3) {
      }

   }

   public void initialize() throws IOException {
      this.cache.initialize();
   }

   public void delete() throws IOException {
      this.cache.delete();
   }

   public void evictAll() throws IOException {
      this.cache.evictAll();
   }

   public Iterator<String> urls() throws IOException {
      return new Iterator<String>() {
         final Iterator<DiskLruCache.Snapshot> delegate;
         @Nullable
         String nextUrl;
         boolean canRemove;

         {
            this.delegate = Cache.this.cache.snapshots();
         }

         public boolean hasNext() {
            if (this.nextUrl != null) {
               return true;
            } else {
               this.canRemove = false;

               while(this.delegate.hasNext()) {
                  try {
                     DiskLruCache.Snapshot snapshot = (DiskLruCache.Snapshot)this.delegate.next();
                     Throwable var2 = null;

                     boolean var4;
                     try {
                        BufferedSource metadata = Okio.buffer(snapshot.getSource(0));
                        this.nextUrl = metadata.readUtf8LineStrict();
                        var4 = true;
                     } catch (Throwable var14) {
                        var2 = var14;
                        throw var14;
                     } finally {
                        if (snapshot != null) {
                           if (var2 != null) {
                              try {
                                 snapshot.close();
                              } catch (Throwable var13) {
                                 var2.addSuppressed(var13);
                              }
                           } else {
                              snapshot.close();
                           }
                        }

                     }

                     return var4;
                  } catch (IOException var16) {
                  }
               }

               return false;
            }
         }

         public String next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               String result = this.nextUrl;
               this.nextUrl = null;
               this.canRemove = true;
               return result;
            }
         }

         public void remove() {
            if (!this.canRemove) {
               throw new IllegalStateException("remove() before next()");
            } else {
               this.delegate.remove();
            }
         }
      };
   }

   public synchronized int writeAbortCount() {
      return this.writeAbortCount;
   }

   public synchronized int writeSuccessCount() {
      return this.writeSuccessCount;
   }

   public long size() throws IOException {
      return this.cache.size();
   }

   public long maxSize() {
      return this.cache.getMaxSize();
   }

   public void flush() throws IOException {
      this.cache.flush();
   }

   public void close() throws IOException {
      this.cache.close();
   }

   public File directory() {
      return this.cache.getDirectory();
   }

   public boolean isClosed() {
      return this.cache.isClosed();
   }

   synchronized void trackResponse(CacheStrategy cacheStrategy) {
      ++this.requestCount;
      if (cacheStrategy.networkRequest != null) {
         ++this.networkCount;
      } else if (cacheStrategy.cacheResponse != null) {
         ++this.hitCount;
      }

   }

   synchronized void trackConditionalCacheHit() {
      ++this.hitCount;
   }

   public synchronized int networkCount() {
      return this.networkCount;
   }

   public synchronized int hitCount() {
      return this.hitCount;
   }

   public synchronized int requestCount() {
      return this.requestCount;
   }

   static int readInt(BufferedSource source) throws IOException {
      try {
         long result = source.readDecimalLong();
         String line = source.readUtf8LineStrict();
         if (result >= 0L && result <= 2147483647L && line.isEmpty()) {
            return (int)result;
         } else {
            throw new IOException("expected an int but was \"" + result + line + "\"");
         }
      } catch (NumberFormatException var4) {
         throw new IOException(var4.getMessage());
      }
   }

   private static class CacheResponseBody extends ResponseBody {
      final DiskLruCache.Snapshot snapshot;
      private final BufferedSource bodySource;
      @Nullable
      private final String contentType;
      @Nullable
      private final String contentLength;

      CacheResponseBody(final DiskLruCache.Snapshot snapshot, String contentType, String contentLength) {
         this.snapshot = snapshot;
         this.contentType = contentType;
         this.contentLength = contentLength;
         Source source = snapshot.getSource(1);
         this.bodySource = Okio.buffer((Source)(new ForwardingSource(source) {
            public void close() throws IOException {
               snapshot.close();
               super.close();
            }
         }));
      }

      public MediaType contentType() {
         return this.contentType != null ? MediaType.parse(this.contentType) : null;
      }

      public long contentLength() {
         try {
            return this.contentLength != null ? Long.parseLong(this.contentLength) : -1L;
         } catch (NumberFormatException var2) {
            return -1L;
         }
      }

      public BufferedSource source() {
         return this.bodySource;
      }
   }

   private static final class Entry {
      private static final String SENT_MILLIS = Platform.get().getPrefix() + "-Sent-Millis";
      private static final String RECEIVED_MILLIS = Platform.get().getPrefix() + "-Received-Millis";
      private final String url;
      private final Headers varyHeaders;
      private final String requestMethod;
      private final Protocol protocol;
      private final int code;
      private final String message;
      private final Headers responseHeaders;
      @Nullable
      private final Handshake handshake;
      private final long sentRequestMillis;
      private final long receivedResponseMillis;

      Entry(Source in) throws IOException {
         try {
            BufferedSource source = Okio.buffer(in);
            this.url = source.readUtf8LineStrict();
            this.requestMethod = source.readUtf8LineStrict();
            Headers.Builder varyHeadersBuilder = new Headers.Builder();
            int varyRequestHeaderLineCount = Cache.readInt(source);

            for(int i = 0; i < varyRequestHeaderLineCount; ++i) {
               varyHeadersBuilder.addLenient(source.readUtf8LineStrict());
            }

            this.varyHeaders = varyHeadersBuilder.build();
            StatusLine statusLine = StatusLine.parse(source.readUtf8LineStrict());
            this.protocol = statusLine.protocol;
            this.code = statusLine.code;
            this.message = statusLine.message;
            Headers.Builder responseHeadersBuilder = new Headers.Builder();
            int responseHeaderLineCount = Cache.readInt(source);
            int i = 0;

            while(true) {
               if (i >= responseHeaderLineCount) {
                  String sendRequestMillisString = responseHeadersBuilder.get(SENT_MILLIS);
                  String receivedResponseMillisString = responseHeadersBuilder.get(RECEIVED_MILLIS);
                  responseHeadersBuilder.removeAll(SENT_MILLIS);
                  responseHeadersBuilder.removeAll(RECEIVED_MILLIS);
                  this.sentRequestMillis = sendRequestMillisString != null ? Long.parseLong(sendRequestMillisString) : 0L;
                  this.receivedResponseMillis = receivedResponseMillisString != null ? Long.parseLong(receivedResponseMillisString) : 0L;
                  this.responseHeaders = responseHeadersBuilder.build();
                  if (this.isHttps()) {
                     String blank = source.readUtf8LineStrict();
                     if (blank.length() > 0) {
                        throw new IOException("expected \"\" but was \"" + blank + "\"");
                     }

                     String cipherSuiteString = source.readUtf8LineStrict();
                     CipherSuite cipherSuite = CipherSuite.forJavaName(cipherSuiteString);
                     List<Certificate> peerCertificates = this.readCertificateList(source);
                     List<Certificate> localCertificates = this.readCertificateList(source);
                     TlsVersion tlsVersion = !source.exhausted() ? TlsVersion.forJavaName(source.readUtf8LineStrict()) : TlsVersion.SSL_3_0;
                     this.handshake = Handshake.get(tlsVersion, cipherSuite, peerCertificates, localCertificates);
                  } else {
                     this.handshake = null;
                  }
                  break;
               }

               responseHeadersBuilder.addLenient(source.readUtf8LineStrict());
               ++i;
            }
         } finally {
            in.close();
         }

      }

      Entry(Response response) {
         this.url = response.request().url().toString();
         this.varyHeaders = HttpHeaders.varyHeaders(response);
         this.requestMethod = response.request().method();
         this.protocol = response.protocol();
         this.code = response.code();
         this.message = response.message();
         this.responseHeaders = response.headers();
         this.handshake = response.handshake();
         this.sentRequestMillis = response.sentRequestAtMillis();
         this.receivedResponseMillis = response.receivedResponseAtMillis();
      }

      public void writeTo(DiskLruCache.Editor editor) throws IOException {
         BufferedSink sink = Okio.buffer(editor.newSink(0));
         sink.writeUtf8(this.url).writeByte(10);
         sink.writeUtf8(this.requestMethod).writeByte(10);
         sink.writeDecimalLong((long)this.varyHeaders.size()).writeByte(10);
         int i = 0;

         int size;
         for(size = this.varyHeaders.size(); i < size; ++i) {
            sink.writeUtf8(this.varyHeaders.name(i)).writeUtf8(": ").writeUtf8(this.varyHeaders.value(i)).writeByte(10);
         }

         sink.writeUtf8((new StatusLine(this.protocol, this.code, this.message)).toString()).writeByte(10);
         sink.writeDecimalLong((long)(this.responseHeaders.size() + 2)).writeByte(10);
         i = 0;

         for(size = this.responseHeaders.size(); i < size; ++i) {
            sink.writeUtf8(this.responseHeaders.name(i)).writeUtf8(": ").writeUtf8(this.responseHeaders.value(i)).writeByte(10);
         }

         sink.writeUtf8(SENT_MILLIS).writeUtf8(": ").writeDecimalLong(this.sentRequestMillis).writeByte(10);
         sink.writeUtf8(RECEIVED_MILLIS).writeUtf8(": ").writeDecimalLong(this.receivedResponseMillis).writeByte(10);
         if (this.isHttps()) {
            sink.writeByte(10);
            sink.writeUtf8(this.handshake.cipherSuite().javaName()).writeByte(10);
            this.writeCertList(sink, this.handshake.peerCertificates());
            this.writeCertList(sink, this.handshake.localCertificates());
            sink.writeUtf8(this.handshake.tlsVersion().javaName()).writeByte(10);
         }

         sink.close();
      }

      private boolean isHttps() {
         return this.url.startsWith("https://");
      }

      private List<Certificate> readCertificateList(BufferedSource source) throws IOException {
         int length = Cache.readInt(source);
         if (length == -1) {
            return Collections.emptyList();
         } else {
            try {
               CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
               List<Certificate> result = new ArrayList(length);

               for(int i = 0; i < length; ++i) {
                  String line = source.readUtf8LineStrict();
                  Buffer bytes = new Buffer();
                  bytes.write(ByteString.decodeBase64(line));
                  result.add(certificateFactory.generateCertificate(bytes.inputStream()));
               }

               return result;
            } catch (CertificateException var8) {
               throw new IOException(var8.getMessage());
            }
         }
      }

      private void writeCertList(BufferedSink sink, List<Certificate> certificates) throws IOException {
         try {
            sink.writeDecimalLong((long)certificates.size()).writeByte(10);
            int i = 0;

            for(int size = certificates.size(); i < size; ++i) {
               byte[] bytes = ((Certificate)certificates.get(i)).getEncoded();
               String line = ByteString.of(bytes).base64();
               sink.writeUtf8(line).writeByte(10);
            }

         } catch (CertificateEncodingException var7) {
            throw new IOException(var7.getMessage());
         }
      }

      public boolean matches(Request request, Response response) {
         return this.url.equals(request.url().toString()) && this.requestMethod.equals(request.method()) && HttpHeaders.varyMatches(response, this.varyHeaders, request);
      }

      public Response response(DiskLruCache.Snapshot snapshot) {
         String contentType = this.responseHeaders.get("Content-Type");
         String contentLength = this.responseHeaders.get("Content-Length");
         Request cacheRequest = (new Request.Builder()).url(this.url).method(this.requestMethod, (RequestBody)null).headers(this.varyHeaders).build();
         return (new Response.Builder()).request(cacheRequest).protocol(this.protocol).code(this.code).message(this.message).headers(this.responseHeaders).body(new Cache.CacheResponseBody(snapshot, contentType, contentLength)).handshake(this.handshake).sentRequestAtMillis(this.sentRequestMillis).receivedResponseAtMillis(this.receivedResponseMillis).build();
      }
   }

   private final class CacheRequestImpl implements CacheRequest {
      private final DiskLruCache.Editor editor;
      private Sink cacheOut;
      private Sink body;
      boolean done;

      CacheRequestImpl(final DiskLruCache.Editor editor) {
         this.editor = editor;
         this.cacheOut = editor.newSink(1);
         this.body = new ForwardingSink(this.cacheOut) {
            public void close() throws IOException {
               synchronized(Cache.this) {
                  if (CacheRequestImpl.this.done) {
                     return;
                  }

                  CacheRequestImpl.this.done = true;
                  ++Cache.this.writeSuccessCount;
               }

               super.close();
               editor.commit();
            }
         };
      }

      public void abort() {
         synchronized(Cache.this) {
            if (this.done) {
               return;
            }

            this.done = true;
            ++Cache.this.writeAbortCount;
         }

         Util.closeQuietly((Closeable)this.cacheOut);

         try {
            this.editor.abort();
         } catch (IOException var3) {
         }

      }

      public Sink body() {
         return this.body;
      }
   }
}

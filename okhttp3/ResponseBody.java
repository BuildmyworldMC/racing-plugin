package okhttp3;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;

public abstract class ResponseBody implements Closeable {
   @Nullable
   private Reader reader;

   @Nullable
   public abstract MediaType contentType();

   public abstract long contentLength();

   public final InputStream byteStream() {
      return this.source().inputStream();
   }

   public abstract BufferedSource source();

   public final byte[] bytes() throws IOException {
      long contentLength = this.contentLength();
      if (contentLength > 2147483647L) {
         throw new IOException("Cannot buffer entire body for content length: " + contentLength);
      } else {
         BufferedSource source = this.source();
         Throwable var5 = null;

         byte[] bytes;
         try {
            bytes = source.readByteArray();
         } catch (Throwable var10) {
            var5 = var10;
            throw var10;
         } finally {
            if (source != null) {
               $closeResource(var5, source);
            }

         }

         if (contentLength != -1L && contentLength != (long)bytes.length) {
            throw new IOException("Content-Length (" + contentLength + ") and stream length (" + bytes.length + ") disagree");
         } else {
            return bytes;
         }
      }
   }

   public final Reader charStream() {
      Reader r = this.reader;
      return r != null ? r : (this.reader = new ResponseBody.BomAwareReader(this.source(), this.charset()));
   }

   public final String string() throws IOException {
      BufferedSource source = this.source();
      Throwable var2 = null;

      String var4;
      try {
         Charset charset = Util.bomAwareCharset(source, this.charset());
         var4 = source.readString(charset);
      } catch (Throwable var8) {
         var2 = var8;
         throw var8;
      } finally {
         if (source != null) {
            $closeResource(var2, source);
         }

      }

      return var4;
   }

   private Charset charset() {
      MediaType contentType = this.contentType();
      return contentType != null ? contentType.charset(StandardCharsets.UTF_8) : StandardCharsets.UTF_8;
   }

   public void close() {
      Util.closeQuietly((Closeable)this.source());
   }

   public static ResponseBody create(@Nullable MediaType contentType, String content) {
      Charset charset = StandardCharsets.UTF_8;
      if (contentType != null) {
         charset = contentType.charset();
         if (charset == null) {
            charset = StandardCharsets.UTF_8;
            contentType = MediaType.parse(contentType + "; charset=utf-8");
         }
      }

      Buffer buffer = (new Buffer()).writeString(content, charset);
      return create(contentType, buffer.size(), buffer);
   }

   public static ResponseBody create(@Nullable MediaType contentType, byte[] content) {
      Buffer buffer = (new Buffer()).write(content);
      return create(contentType, (long)content.length, buffer);
   }

   public static ResponseBody create(@Nullable MediaType contentType, ByteString content) {
      Buffer buffer = (new Buffer()).write(content);
      return create(contentType, (long)content.size(), buffer);
   }

   public static ResponseBody create(@Nullable final MediaType contentType, final long contentLength, final BufferedSource content) {
      if (content == null) {
         throw new NullPointerException("source == null");
      } else {
         return new ResponseBody() {
            @Nullable
            public MediaType contentType() {
               return contentType;
            }

            public long contentLength() {
               return contentLength;
            }

            public BufferedSource source() {
               return content;
            }
         };
      }
   }

   // $FF: synthetic method
   private static void $closeResource(Throwable x0, AutoCloseable x1) {
      if (x0 != null) {
         try {
            x1.close();
         } catch (Throwable var3) {
            x0.addSuppressed(var3);
         }
      } else {
         x1.close();
      }

   }

   static final class BomAwareReader extends Reader {
      private final BufferedSource source;
      private final Charset charset;
      private boolean closed;
      @Nullable
      private Reader delegate;

      BomAwareReader(BufferedSource source, Charset charset) {
         this.source = source;
         this.charset = charset;
      }

      public int read(char[] cbuf, int off, int len) throws IOException {
         if (this.closed) {
            throw new IOException("Stream closed");
         } else {
            Reader delegate = this.delegate;
            if (delegate == null) {
               Charset charset = Util.bomAwareCharset(this.source, this.charset);
               delegate = this.delegate = new InputStreamReader(this.source.inputStream(), charset);
            }

            return delegate.read(cbuf, off, len);
         }
      }

      public void close() throws IOException {
         this.closed = true;
         if (this.delegate != null) {
            this.delegate.close();
         } else {
            this.source.close();
         }

      }
   }
}

package okhttp3;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.ByteString;
import okio.Okio;
import okio.Source;

public abstract class RequestBody {
   @Nullable
   public abstract MediaType contentType();

   public long contentLength() throws IOException {
      return -1L;
   }

   public abstract void writeTo(BufferedSink var1) throws IOException;

   public static RequestBody create(@Nullable MediaType contentType, String content) {
      Charset charset = StandardCharsets.UTF_8;
      if (contentType != null) {
         charset = contentType.charset();
         if (charset == null) {
            charset = StandardCharsets.UTF_8;
            contentType = MediaType.parse(contentType + "; charset=utf-8");
         }
      }

      byte[] bytes = content.getBytes(charset);
      return create(contentType, bytes);
   }

   public static RequestBody create(@Nullable final MediaType contentType, final ByteString content) {
      return new RequestBody() {
         @Nullable
         public MediaType contentType() {
            return contentType;
         }

         public long contentLength() throws IOException {
            return (long)content.size();
         }

         public void writeTo(BufferedSink sink) throws IOException {
            sink.write(content);
         }
      };
   }

   public static RequestBody create(@Nullable MediaType contentType, byte[] content) {
      return create(contentType, content, 0, content.length);
   }

   public static RequestBody create(@Nullable final MediaType contentType, final byte[] content, final int offset, final int byteCount) {
      if (content == null) {
         throw new NullPointerException("content == null");
      } else {
         Util.checkOffsetAndCount((long)content.length, (long)offset, (long)byteCount);
         return new RequestBody() {
            @Nullable
            public MediaType contentType() {
               return contentType;
            }

            public long contentLength() {
               return (long)byteCount;
            }

            public void writeTo(BufferedSink sink) throws IOException {
               sink.write(content, offset, byteCount);
            }
         };
      }
   }

   public static RequestBody create(@Nullable final MediaType contentType, final File file) {
      if (file == null) {
         throw new NullPointerException("file == null");
      } else {
         return new RequestBody() {
            @Nullable
            public MediaType contentType() {
               return contentType;
            }

            public long contentLength() {
               return file.length();
            }

            public void writeTo(BufferedSink sink) throws IOException {
               Source source = Okio.source(file);
               Throwable var3 = null;

               try {
                  sink.writeAll(source);
               } catch (Throwable var12) {
                  var3 = var12;
                  throw var12;
               } finally {
                  if (source != null) {
                     if (var3 != null) {
                        try {
                           source.close();
                        } catch (Throwable var11) {
                           var3.addSuppressed(var11);
                        }
                     } else {
                        source.close();
                     }
                  }

               }

            }
         };
      }
   }
}

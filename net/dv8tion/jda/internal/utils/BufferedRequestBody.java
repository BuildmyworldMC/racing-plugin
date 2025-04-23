package net.dv8tion.jda.internal.utils;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

public class BufferedRequestBody extends RequestBody {
   private final Source source;
   private final MediaType type;
   private byte[] data;

   public BufferedRequestBody(Source source, MediaType type) {
      this.source = source;
      this.type = type;
   }

   @Nullable
   public MediaType contentType() {
      return this.type;
   }

   public void writeTo(@Nonnull BufferedSink sink) throws IOException {
      if (this.data != null) {
         sink.write(this.data);
      } else {
         BufferedSource s = Okio.buffer(this.source);

         try {
            this.data = s.readByteArray();
            sink.write(this.data);
         } catch (Throwable var6) {
            if (s != null) {
               try {
                  s.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (s != null) {
            s.close();
         }

      }
   }
}

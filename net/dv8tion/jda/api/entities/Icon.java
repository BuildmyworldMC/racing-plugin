package net.dv8tion.jda.api.entities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.Nonnull;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.IOUtil;

public class Icon {
   protected final String encoding;

   protected Icon(@Nonnull Icon.IconType type, @Nonnull String base64Encoding) {
      this.encoding = type.getHeader() + base64Encoding;
   }

   @Nonnull
   public String getEncoding() {
      return this.encoding;
   }

   @Nonnull
   public static Icon from(@Nonnull File file) throws IOException {
      Checks.notNull(file, "Provided File");
      Checks.check(file.exists(), "Provided file does not exist!");
      int index = file.getName().lastIndexOf(46);
      if (index < 0) {
         return from(file, Icon.IconType.JPEG);
      } else {
         String ext = file.getName().substring(index + 1);
         Icon.IconType type = Icon.IconType.fromExtension(ext);
         return from(file, type);
      }
   }

   @Nonnull
   public static Icon from(@Nonnull InputStream stream) throws IOException {
      return from(stream, Icon.IconType.JPEG);
   }

   @Nonnull
   public static Icon from(@Nonnull byte[] data) {
      return from(data, Icon.IconType.JPEG);
   }

   @Nonnull
   public static Icon from(@Nonnull File file, @Nonnull Icon.IconType type) throws IOException {
      Checks.notNull(file, "Provided File");
      Checks.notNull(type, "IconType");
      Checks.check(file.exists(), "Provided file does not exist!");
      return from(IOUtil.readFully(file), type);
   }

   @Nonnull
   public static Icon from(@Nonnull InputStream stream, @Nonnull Icon.IconType type) throws IOException {
      Checks.notNull(stream, "InputStream");
      Checks.notNull(type, "IconType");
      return from(IOUtil.readFully(stream), type);
   }

   @Nonnull
   public static Icon from(@Nonnull byte[] data, @Nonnull Icon.IconType type) {
      Checks.notNull(data, "Provided byte[]");
      Checks.notNull(type, "IconType");
      return new Icon(type, new String(Base64.getEncoder().encode(data), StandardCharsets.UTF_8));
   }

   public static enum IconType {
      JPEG("image/jpeg"),
      PNG("image/png"),
      WEBP("image/webp"),
      GIF("image/gif"),
      UNKNOWN("image/jpeg");

      private final String mime;
      private final String header;

      private IconType(@Nonnull String mime) {
         this.mime = mime;
         this.header = "data:" + mime + ";base64,";
      }

      @Nonnull
      public String getMIME() {
         return this.mime;
      }

      @Nonnull
      public String getHeader() {
         return this.header;
      }

      @Nonnull
      public static Icon.IconType fromMIME(@Nonnull String mime) {
         Checks.notNull(mime, "MIME Type");
         Icon.IconType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Icon.IconType type = var1[var3];
            if (type.mime.equalsIgnoreCase(mime)) {
               return type;
            }
         }

         return UNKNOWN;
      }

      @Nonnull
      public static Icon.IconType fromExtension(@Nonnull String extension) {
         Checks.notNull(extension, "Extension Type");
         String var1 = extension.toLowerCase();
         byte var2 = -1;
         switch(var1.hashCode()) {
         case 102340:
            if (var1.equals("gif")) {
               var2 = 8;
            }
            break;
         case 105133:
            if (var1.equals("jfi")) {
               var2 = 3;
            }
            break;
         case 105223:
            if (var1.equals("jif")) {
               var2 = 1;
            }
            break;
         case 105439:
            if (var1.equals("jpe")) {
               var2 = 0;
            }
            break;
         case 105441:
            if (var1.equals("jpg")) {
               var2 = 4;
            }
            break;
         case 111145:
            if (var1.equals("png")) {
               var2 = 6;
            }
            break;
         case 3259225:
            if (var1.equals("jfif")) {
               var2 = 2;
            }
            break;
         case 3268712:
            if (var1.equals("jpeg")) {
               var2 = 5;
            }
            break;
         case 3645340:
            if (var1.equals("webp")) {
               var2 = 7;
            }
         }

         switch(var2) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
            return JPEG;
         case 6:
            return PNG;
         case 7:
            return WEBP;
         case 8:
            return GIF;
         default:
            return UNKNOWN;
         }
      }
   }
}

package net.dv8tion.jda.internal.audio;

import java.util.Iterator;
import net.dv8tion.jda.api.utils.data.DataArray;

public enum AudioEncryption {
   XSALSA20_POLY1305_LITE,
   XSALSA20_POLY1305_SUFFIX,
   XSALSA20_POLY1305;

   private final String key = this.name().toLowerCase();

   public String getKey() {
      return this.key;
   }

   public static AudioEncryption getPreferredMode(DataArray array) {
      AudioEncryption encryption = null;
      Iterator var2 = array.iterator();

      while(var2.hasNext()) {
         Object o = var2.next();

         try {
            String name = String.valueOf(o).toUpperCase();
            AudioEncryption e = valueOf(name);
            if (encryption == null || e.ordinal() < encryption.ordinal()) {
               encryption = e;
            }
         } catch (IllegalArgumentException var6) {
         }
      }

      return encryption;
   }
}

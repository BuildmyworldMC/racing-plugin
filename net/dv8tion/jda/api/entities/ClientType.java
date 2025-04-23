package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;

public enum ClientType {
   DESKTOP("desktop"),
   MOBILE("mobile"),
   WEB("web"),
   UNKNOWN("unknown");

   private final String key;

   private ClientType(String key) {
      this.key = key;
   }

   public String getKey() {
      return this.key;
   }

   @Nonnull
   public static ClientType fromKey(@Nonnull String key) {
      ClientType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ClientType type = var1[var3];
         if (type.key.equals(key)) {
            return type;
         }
      }

      return UNKNOWN;
   }
}

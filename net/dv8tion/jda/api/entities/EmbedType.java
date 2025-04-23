package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;

public enum EmbedType {
   IMAGE("image"),
   VIDEO("video"),
   LINK("link"),
   RICH("rich"),
   UNKNOWN("");

   private final String key;

   private EmbedType(String key) {
      this.key = key;
   }

   @Nonnull
   public static EmbedType fromKey(String key) {
      EmbedType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         EmbedType type = var1[var3];
         if (type.key.equals(key)) {
            return type;
         }
      }

      return UNKNOWN;
   }
}

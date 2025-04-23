package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;

public enum WebhookType {
   UNKNOWN(-1),
   INCOMING(1),
   FOLLOWER(2);

   private final int key;

   private WebhookType(int key) {
      this.key = key;
   }

   public int getKey() {
      return this.key;
   }

   @Nonnull
   public static WebhookType fromKey(int key) {
      WebhookType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         WebhookType type = var1[var3];
         if (type.key == key) {
            return type;
         }
      }

      return UNKNOWN;
   }
}

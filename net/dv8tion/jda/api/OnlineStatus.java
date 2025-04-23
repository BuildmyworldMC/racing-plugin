package net.dv8tion.jda.api;

public enum OnlineStatus {
   ONLINE("online"),
   IDLE("idle"),
   DO_NOT_DISTURB("dnd"),
   INVISIBLE("invisible"),
   OFFLINE("offline"),
   UNKNOWN("");

   private final String key;

   private OnlineStatus(String key) {
      this.key = key;
   }

   public String getKey() {
      return this.key;
   }

   public static OnlineStatus fromKey(String key) {
      OnlineStatus[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         OnlineStatus onlineStatus = var1[var3];
         if (onlineStatus.key.equalsIgnoreCase(key)) {
            return onlineStatus;
         }
      }

      return UNKNOWN;
   }
}

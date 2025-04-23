package net.dv8tion.jda.internal.utils.config.flags;

import java.util.EnumSet;

public enum ConfigFlag {
   RAW_EVENTS,
   USE_RELATIVE_RATELIMIT(true),
   RETRY_TIMEOUT(true),
   BULK_DELETE_SPLIT(true),
   SHUTDOWN_HOOK(true),
   MDC_CONTEXT(true),
   AUTO_RECONNECT(true);

   private final boolean isDefault;

   private ConfigFlag() {
      this(false);
   }

   private ConfigFlag(boolean isDefault) {
      this.isDefault = isDefault;
   }

   public static EnumSet<ConfigFlag> getDefault() {
      EnumSet<ConfigFlag> set = EnumSet.noneOf(ConfigFlag.class);
      ConfigFlag[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ConfigFlag flag = var1[var3];
         if (flag.isDefault) {
            set.add(flag);
         }
      }

      return set;
   }
}

package net.dv8tion.jda.api.audio;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum SpeakingMode {
   VOICE(1),
   SOUNDSHARE(2),
   PRIORITY(4);

   private final int raw;

   private SpeakingMode(int raw) {
      this.raw = raw;
   }

   public int getRaw() {
      return this.raw;
   }

   @Nonnull
   public static EnumSet<SpeakingMode> getModes(int mask) {
      EnumSet<SpeakingMode> modes = EnumSet.noneOf(SpeakingMode.class);
      if (mask == 0) {
         return modes;
      } else {
         SpeakingMode[] values = values();
         SpeakingMode[] var3 = values;
         int var4 = values.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            SpeakingMode mode = var3[var5];
            if ((mode.raw & mask) == mode.raw) {
               modes.add(mode);
            }
         }

         return modes;
      }
   }

   public static int getRaw(@Nullable SpeakingMode... modes) {
      if (modes != null && modes.length != 0) {
         int mask = 0;
         SpeakingMode[] var2 = modes;
         int var3 = modes.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            SpeakingMode m = var2[var4];
            mask |= m.raw;
         }

         return mask;
      } else {
         return 0;
      }
   }

   public static int getRaw(@Nullable Collection<SpeakingMode> modes) {
      if (modes == null) {
         return 0;
      } else {
         int raw = 0;

         SpeakingMode mode;
         for(Iterator var2 = modes.iterator(); var2.hasNext(); raw |= mode.getRaw()) {
            mode = (SpeakingMode)var2.next();
         }

         return raw;
      }
   }
}

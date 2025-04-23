package net.dv8tion.jda.api.utils;

import javax.annotation.Nonnull;
import net.dv8tion.jda.internal.utils.Checks;

@FunctionalInterface
public interface ChunkingFilter {
   ChunkingFilter ALL = (x) -> {
      return true;
   };
   ChunkingFilter NONE = (x) -> {
      return false;
   };

   boolean filter(long var1);

   @Nonnull
   static ChunkingFilter include(@Nonnull long... ids) {
      Checks.notNull(ids, "ID array");
      return ids.length == 0 ? NONE : (guild) -> {
         long[] var3 = ids;
         int var4 = ids.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            long id = var3[var5];
            if (id == guild) {
               return true;
            }
         }

         return false;
      };
   }

   @Nonnull
   static ChunkingFilter exclude(@Nonnull long... ids) {
      Checks.notNull(ids, "ID array");
      return ids.length == 0 ? ALL : (guild) -> {
         long[] var3 = ids;
         int var4 = ids.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            long id = var3[var5];
            if (id == guild) {
               return false;
            }
         }

         return true;
      };
   }
}

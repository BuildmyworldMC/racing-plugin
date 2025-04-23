package net.dv8tion.jda.api.entities;

import java.util.EnumSet;
import javax.annotation.Nonnull;

public enum ChannelType {
   TEXT(0, 0, true),
   PRIVATE(1, -1),
   VOICE(2, 1, true),
   GROUP(3, -1),
   CATEGORY(4, 2, true),
   STORE(6, 0, true),
   UNKNOWN(-1, -2);

   private final int sortBucket;
   private final int id;
   private final boolean isGuild;

   private ChannelType(int id, int sortBucket) {
      this(id, sortBucket, false);
   }

   private ChannelType(int id, int sortBucket, boolean isGuild) {
      this.id = id;
      this.sortBucket = sortBucket;
      this.isGuild = isGuild;
   }

   public int getSortBucket() {
      return this.sortBucket;
   }

   public int getId() {
      return this.id;
   }

   public boolean isGuild() {
      return this.isGuild;
   }

   @Nonnull
   public static ChannelType fromId(int id) {
      if (id == 5) {
         return TEXT;
      } else {
         ChannelType[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            ChannelType type = var1[var3];
            if (type.id == id) {
               return type;
            }
         }

         return UNKNOWN;
      }
   }

   @Nonnull
   public static EnumSet<ChannelType> fromSortBucket(int bucket) {
      EnumSet<ChannelType> types = EnumSet.noneOf(ChannelType.class);
      ChannelType[] var2 = values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ChannelType type = var2[var4];
         if (type.getSortBucket() == bucket) {
            types.add(type);
         }
      }

      return types;
   }
}

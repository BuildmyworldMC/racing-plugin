package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;

public enum MessageType {
   DEFAULT(0),
   RECIPIENT_ADD(1),
   RECIPIENT_REMOVE(2),
   CALL(3),
   CHANNEL_NAME_CHANGE(4),
   CHANNEL_ICON_CHANGE(5),
   CHANNEL_PINNED_ADD(6),
   GUILD_MEMBER_JOIN(7),
   GUILD_MEMBER_BOOST(8),
   GUILD_BOOST_TIER_1(9),
   GUILD_BOOST_TIER_2(10),
   GUILD_BOOST_TIER_3(11),
   CHANNEL_FOLLOW_ADD(12),
   INLINE_REPLY(19),
   UNKNOWN(-1);

   protected final int id;

   private MessageType(int id) {
      this.id = id;
   }

   public int getId() {
      return this.id;
   }

   @Nonnull
   public static MessageType fromId(int id) {
      MessageType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         MessageType type = var1[var3];
         if (type.id == id) {
            return type;
         }
      }

      return UNKNOWN;
   }
}

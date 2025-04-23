package net.dv8tion.jda.api.entities;

import javax.annotation.Nonnull;

public interface TeamMember {
   @Nonnull
   User getUser();

   @Nonnull
   TeamMember.MembershipState getMembershipState();

   @Nonnull
   default String getTeamId() {
      return Long.toUnsignedString(this.getTeamIdLong());
   }

   long getTeamIdLong();

   public static enum MembershipState {
      INVITED(1),
      ACCEPTED(2),
      UNKNOWN(-1);

      private final int key;

      private MembershipState(int key) {
         this.key = key;
      }

      public int getKey() {
         return this.key;
      }

      @Nonnull
      public static TeamMember.MembershipState fromKey(int key) {
         TeamMember.MembershipState[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            TeamMember.MembershipState state = var1[var3];
            if (state.key == key) {
               return state;
            }
         }

         return UNKNOWN;
      }
   }
}

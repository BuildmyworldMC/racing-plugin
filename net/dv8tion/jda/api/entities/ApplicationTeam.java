package net.dv8tion.jda.api.entities;

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.utils.Checks;

public interface ApplicationTeam extends ISnowflake {
   String ICON_URL = "https://cdn.discordapp.com/team-icons/%s/%s.png";

   @Nullable
   default TeamMember getOwner() {
      return this.getMemberById(this.getOwnerIdLong());
   }

   @Nonnull
   default String getOwnerId() {
      return Long.toUnsignedString(this.getOwnerIdLong());
   }

   long getOwnerIdLong();

   @Nullable
   String getIconId();

   @Nullable
   default String getIconUrl() {
      String iconId = this.getIconId();
      return iconId == null ? null : String.format("https://cdn.discordapp.com/team-icons/%s/%s.png", this.getId(), iconId);
   }

   @Nonnull
   List<TeamMember> getMembers();

   default boolean isMember(@Nonnull User user) {
      return this.getMember(user) != null;
   }

   @Nullable
   default TeamMember getMember(@Nonnull User user) {
      Checks.notNull(user, "User");
      return this.getMemberById(user.getIdLong());
   }

   @Nullable
   default TeamMember getMemberById(@Nonnull String userId) {
      return this.getMemberById(MiscUtil.parseSnowflake(userId));
   }

   @Nullable
   default TeamMember getMemberById(long userId) {
      Iterator var3 = this.getMembers().iterator();

      TeamMember member;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         member = (TeamMember)var3.next();
      } while(member.getUser().getIdLong() != userId);

      return member;
   }
}

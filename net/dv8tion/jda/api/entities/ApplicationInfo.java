package net.dv8tion.jda.api.entities;

import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

public interface ApplicationInfo extends ISnowflake {
   boolean doesBotRequireCodeGrant();

   @Nonnull
   String getDescription();

   @Nullable
   String getIconId();

   @Nullable
   String getIconUrl();

   @Nullable
   ApplicationTeam getTeam();

   @Nonnull
   default String getInviteUrl(@Nullable Collection<Permission> permissions) {
      return this.getInviteUrl((String)null, (Collection)permissions);
   }

   @Nonnull
   default String getInviteUrl(@Nullable Permission... permissions) {
      return this.getInviteUrl((String)null, (Permission[])permissions);
   }

   @Nonnull
   String getInviteUrl(@Nullable String var1, @Nullable Collection<Permission> var2);

   @Nonnull
   default String getInviteUrl(long guildId, @Nullable Collection<Permission> permissions) {
      return this.getInviteUrl(Long.toUnsignedString(guildId), permissions);
   }

   @Nonnull
   default String getInviteUrl(@Nullable String guildId, @Nullable Permission... permissions) {
      return this.getInviteUrl(guildId, (Collection)(permissions == null ? null : Arrays.asList(permissions)));
   }

   @Nonnull
   default String getInviteUrl(long guildId, @Nullable Permission... permissions) {
      return this.getInviteUrl(Long.toUnsignedString(guildId), permissions);
   }

   @Nonnull
   JDA getJDA();

   @Nonnull
   String getName();

   @Nonnull
   User getOwner();

   boolean isBotPublic();
}

package net.dv8tion.jda.api.entities;

import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

public interface GuildChannel extends ISnowflake, Comparable<GuildChannel> {
   @Nonnull
   ChannelType getType();

   @Nonnull
   String getName();

   @Nonnull
   Guild getGuild();

   @Nullable
   Category getParent();

   @Nonnull
   List<Member> getMembers();

   int getPosition();

   int getPositionRaw();

   @Nonnull
   JDA getJDA();

   @Nullable
   PermissionOverride getPermissionOverride(@Nonnull IPermissionHolder var1);

   @Nonnull
   List<PermissionOverride> getPermissionOverrides();

   @Nonnull
   List<PermissionOverride> getMemberPermissionOverrides();

   @Nonnull
   List<PermissionOverride> getRolePermissionOverrides();

   boolean isSynced();

   @Nonnull
   @CheckReturnValue
   ChannelAction<? extends GuildChannel> createCopy(@Nonnull Guild var1);

   @Nonnull
   @CheckReturnValue
   default ChannelAction<? extends GuildChannel> createCopy() {
      return this.createCopy(this.getGuild());
   }

   @Nonnull
   ChannelManager getManager();

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();

   @Nonnull
   @CheckReturnValue
   PermissionOverrideAction createPermissionOverride(@Nonnull IPermissionHolder var1);

   @Nonnull
   @CheckReturnValue
   PermissionOverrideAction putPermissionOverride(@Nonnull IPermissionHolder var1);

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction upsertPermissionOverride(@Nonnull IPermissionHolder permissionHolder) {
      PermissionOverride override = this.getPermissionOverride(permissionHolder);
      return override != null ? override.getManager() : this.putPermissionOverride(permissionHolder);
   }

   @Nonnull
   @CheckReturnValue
   InviteAction createInvite();

   @Nonnull
   @CheckReturnValue
   RestAction<List<Invite>> retrieveInvites();
}

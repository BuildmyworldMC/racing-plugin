package net.dv8tion.jda.api.entities;

import java.util.EnumSet;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

public interface PermissionOverride extends ISnowflake {
   long getAllowedRaw();

   long getInheritRaw();

   long getDeniedRaw();

   @Nonnull
   EnumSet<Permission> getAllowed();

   @Nonnull
   EnumSet<Permission> getInherit();

   @Nonnull
   EnumSet<Permission> getDenied();

   @Nonnull
   JDA getJDA();

   @Nullable
   IPermissionHolder getPermissionHolder();

   @Nullable
   Member getMember();

   @Nullable
   Role getRole();

   @Nonnull
   GuildChannel getChannel();

   @Nonnull
   Guild getGuild();

   boolean isMemberOverride();

   boolean isRoleOverride();

   @Nonnull
   PermissionOverrideAction getManager();

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> delete();
}

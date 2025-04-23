package net.dv8tion.jda.api.requests.restaction;

import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.utils.Checks;

public interface PermissionOverrideAction extends AuditableRestAction<PermissionOverride> {
   @Nonnull
   PermissionOverrideAction setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   PermissionOverrideAction timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   PermissionOverrideAction deadline(long var1);

   @Nonnull
   default PermissionOverrideAction reset() {
      return this.resetAllow().resetDeny();
   }

   @Nonnull
   PermissionOverrideAction resetAllow();

   @Nonnull
   PermissionOverrideAction resetDeny();

   @Nonnull
   GuildChannel getChannel();

   @Nullable
   Role getRole();

   @Nullable
   Member getMember();

   @Nonnull
   default Guild getGuild() {
      return this.getChannel().getGuild();
   }

   long getAllow();

   @Nonnull
   default EnumSet<Permission> getAllowedPermissions() {
      return Permission.getPermissions(this.getAllow());
   }

   long getDeny();

   @Nonnull
   default EnumSet<Permission> getDeniedPermissions() {
      return Permission.getPermissions(this.getDeny());
   }

   long getInherited();

   @Nonnull
   default EnumSet<Permission> getInheritedPermissions() {
      return Permission.getPermissions(this.getInherited());
   }

   boolean isMember();

   boolean isRole();

   @Nonnull
   @CheckReturnValue
   PermissionOverrideAction setAllow(long var1);

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction setAllow(@Nullable Collection<Permission> permissions) {
      if (permissions != null && !permissions.isEmpty()) {
         Checks.noneNull(permissions, "Permissions");
         return this.setAllow(Permission.getRaw(permissions));
      } else {
         return this.setAllow(0L);
      }
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction setAllow(@Nullable Permission... permissions) {
      if (permissions != null && permissions.length != 0) {
         Checks.noneNull((Object[])permissions, "Permissions");
         return this.setAllow(Permission.getRaw(permissions));
      } else {
         return this.setAllow(0L);
      }
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction grant(long allowBits) {
      return this.setAllow(this.getAllow() | allowBits);
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction grant(@Nonnull Collection<Permission> permissions) {
      return this.setAllow(this.getAllow() | Permission.getRaw(permissions));
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction grant(@Nonnull Permission... permissions) {
      return this.setAllow(this.getAllow() | Permission.getRaw(permissions));
   }

   @Nonnull
   @CheckReturnValue
   PermissionOverrideAction setDeny(long var1);

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction setDeny(@Nullable Collection<Permission> permissions) {
      if (permissions != null && !permissions.isEmpty()) {
         Checks.noneNull(permissions, "Permissions");
         return this.setDeny(Permission.getRaw(permissions));
      } else {
         return this.setDeny(0L);
      }
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction setDeny(@Nullable Permission... permissions) {
      if (permissions != null && permissions.length != 0) {
         Checks.noneNull((Object[])permissions, "Permissions");
         return this.setDeny(Permission.getRaw(permissions));
      } else {
         return this.setDeny(0L);
      }
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction deny(long denyBits) {
      return this.setDeny(this.getDeny() | denyBits);
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction deny(@Nonnull Collection<Permission> permissions) {
      return this.setDeny(this.getDeny() | Permission.getRaw(permissions));
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction deny(@Nonnull Permission... permissions) {
      return this.setDeny(this.getDeny() | Permission.getRaw(permissions));
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction clear(long inheritedBits) {
      return this.setDeny(this.getDeny() & ~inheritedBits).setAllow(this.getAllow() & ~inheritedBits);
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction clear(@Nonnull Collection<Permission> permissions) {
      return this.clear(Permission.getRaw(permissions));
   }

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction clear(@Nonnull Permission... permissions) {
      return this.clear(Permission.getRaw(permissions));
   }

   @Nonnull
   @CheckReturnValue
   PermissionOverrideAction setPermissions(long var1, long var3);

   @Nonnull
   @CheckReturnValue
   default PermissionOverrideAction setPermissions(@Nullable Collection<Permission> grantPermissions, @Nullable Collection<Permission> denyPermissions) {
      return this.setAllow(grantPermissions).setDeny(denyPermissions);
   }
}

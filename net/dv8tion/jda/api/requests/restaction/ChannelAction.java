package net.dv8tion.jda.api.requests.restaction;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.internal.utils.Checks;

public interface ChannelAction<T extends GuildChannel> extends AuditableRestAction<T> {
   @Nonnull
   ChannelAction<T> setCheck(@Nullable BooleanSupplier var1);

   @Nonnull
   ChannelAction<T> timeout(long var1, @Nonnull TimeUnit var3);

   @Nonnull
   ChannelAction<T> deadline(long var1);

   @Nonnull
   Guild getGuild();

   @Nonnull
   ChannelType getType();

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setName(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setParent(@Nullable Category var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setPosition(@Nullable Integer var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setTopic(@Nullable String var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setNSFW(boolean var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setSlowmode(int var1);

   @Nonnull
   @CheckReturnValue
   default ChannelAction<T> addPermissionOverride(@Nonnull IPermissionHolder target, @Nullable Collection<Permission> allow, @Nullable Collection<Permission> deny) {
      long allowRaw = allow != null ? Permission.getRaw(allow) : 0L;
      long denyRaw = deny != null ? Permission.getRaw(deny) : 0L;
      return this.addPermissionOverride(target, allowRaw, denyRaw);
   }

   @Nonnull
   @CheckReturnValue
   default ChannelAction<T> addPermissionOverride(@Nonnull IPermissionHolder target, long allow, long deny) {
      Checks.notNull(target, "Override Role/Member");
      if (target instanceof Role) {
         return this.addRolePermissionOverride(target.getIdLong(), allow, deny);
      } else if (target instanceof Member) {
         return this.addMemberPermissionOverride(target.getIdLong(), allow, deny);
      } else {
         throw new IllegalArgumentException("Cannot add override for " + target.getClass().getSimpleName());
      }
   }

   @Nonnull
   @CheckReturnValue
   default ChannelAction<T> addMemberPermissionOverride(long memberId, @Nullable Collection<Permission> allow, @Nullable Collection<Permission> deny) {
      long allowRaw = allow != null ? Permission.getRaw(allow) : 0L;
      long denyRaw = deny != null ? Permission.getRaw(deny) : 0L;
      return this.addMemberPermissionOverride(memberId, allowRaw, denyRaw);
   }

   @Nonnull
   @CheckReturnValue
   default ChannelAction<T> addRolePermissionOverride(long roleId, @Nullable Collection<Permission> allow, @Nullable Collection<Permission> deny) {
      long allowRaw = allow != null ? Permission.getRaw(allow) : 0L;
      long denyRaw = deny != null ? Permission.getRaw(deny) : 0L;
      return this.addRolePermissionOverride(roleId, allowRaw, denyRaw);
   }

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> addMemberPermissionOverride(long var1, long var3, long var5);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> addRolePermissionOverride(long var1, long var3, long var5);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setBitrate(@Nullable Integer var1);

   @Nonnull
   @CheckReturnValue
   ChannelAction<T> setUserlimit(@Nullable Integer var1);
}

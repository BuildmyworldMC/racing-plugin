package net.dv8tion.jda.api.managers;

import java.util.Collection;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;

public interface ChannelManager extends Manager<ChannelManager> {
   long NAME = 1L;
   long PARENT = 2L;
   long TOPIC = 4L;
   long POSITION = 8L;
   long NSFW = 16L;
   long USERLIMIT = 32L;
   long BITRATE = 64L;
   long PERMISSION = 128L;
   long SLOWMODE = 256L;

   @Nonnull
   ChannelManager reset(long var1);

   @Nonnull
   ChannelManager reset(long... var1);

   @Nonnull
   GuildChannel getChannel();

   @Nonnull
   default ChannelType getType() {
      return this.getChannel().getType();
   }

   @Nonnull
   default Guild getGuild() {
      return this.getChannel().getGuild();
   }

   @Nonnull
   @CheckReturnValue
   ChannelManager clearOverridesAdded();

   @Nonnull
   @CheckReturnValue
   ChannelManager clearOverridesRemoved();

   @Nonnull
   @CheckReturnValue
   ChannelManager putPermissionOverride(@Nonnull IPermissionHolder var1, long var2, long var4);

   @Nonnull
   @CheckReturnValue
   default ChannelManager putPermissionOverride(@Nonnull IPermissionHolder permHolder, @Nullable Collection<Permission> allow, @Nullable Collection<Permission> deny) {
      long allowRaw = allow == null ? 0L : Permission.getRaw(allow);
      long denyRaw = deny == null ? 0L : Permission.getRaw(deny);
      return this.putPermissionOverride(permHolder, allowRaw, denyRaw);
   }

   @Nonnull
   @CheckReturnValue
   ChannelManager removePermissionOverride(@Nonnull IPermissionHolder var1);

   @Nonnull
   @CheckReturnValue
   default ChannelManager sync() {
      if (this.getChannel().getParent() == null) {
         throw new IllegalStateException("sync() requires a parent category");
      } else {
         return this.sync(this.getChannel().getParent());
      }
   }

   @Nonnull
   @CheckReturnValue
   ChannelManager sync(@Nonnull GuildChannel var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setName(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setParent(@Nullable Category var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setPosition(int var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setTopic(@Nullable String var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setNSFW(boolean var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setSlowmode(int var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setUserLimit(int var1);

   @Nonnull
   @CheckReturnValue
   ChannelManager setBitrate(int var1);
}

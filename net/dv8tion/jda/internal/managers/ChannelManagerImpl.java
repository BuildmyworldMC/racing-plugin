package net.dv8tion.jda.internal.managers;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.LongStream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.AbstractChannelImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.PermOverrideData;
import net.dv8tion.jda.internal.utils.Checks;
import okhttp3.RequestBody;

public class ChannelManagerImpl extends ManagerBase<ChannelManager> implements ChannelManager {
   protected GuildChannel channel;
   protected String name;
   protected String parent;
   protected String topic;
   protected int position;
   protected boolean nsfw;
   protected int slowmode;
   protected int userlimit;
   protected int bitrate;
   protected final Object lock = new Object();
   protected final TLongObjectHashMap<PermOverrideData> overridesAdd;
   protected final TLongSet overridesRem;

   public ChannelManagerImpl(GuildChannel channel) {
      super(channel.getJDA(), Route.Channels.MODIFY_CHANNEL.compile(channel.getId()));
      JDA jda = channel.getJDA();
      ChannelType type = channel.getType();
      this.channel = channel;
      if (isPermissionChecksEnabled()) {
         this.checkPermissions();
      }

      this.overridesAdd = new TLongObjectHashMap();
      this.overridesRem = new TLongHashSet();
   }

   @Nonnull
   public GuildChannel getChannel() {
      GuildChannel realChannel = this.api.getGuildChannelById(this.channel.getType(), this.channel.getIdLong());
      if (realChannel != null) {
         this.channel = realChannel;
      }

      return this.channel;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl reset(long fields) {
      super.reset(fields);
      if ((fields & 1L) == 1L) {
         this.name = null;
      }

      if ((fields & 2L) == 2L) {
         this.parent = null;
      }

      if ((fields & 4L) == 4L) {
         this.topic = null;
      }

      if ((fields & 128L) == 128L) {
         this.withLock(this.lock, (lock) -> {
            this.overridesRem.clear();
            this.overridesAdd.clear();
         });
      }

      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl reset(long... fields) {
      super.reset(fields);
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl reset() {
      super.reset();
      this.name = null;
      this.parent = null;
      this.topic = null;
      this.withLock(this.lock, (lock) -> {
         this.overridesRem.clear();
         this.overridesAdd.clear();
      });
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl clearOverridesAdded() {
      this.withLock(this.lock, (lock) -> {
         this.overridesAdd.clear();
         if (this.overridesRem.isEmpty()) {
            this.set &= -129L;
         }

      });
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl clearOverridesRemoved() {
      this.withLock(this.lock, (lock) -> {
         this.overridesRem.clear();
         if (this.overridesAdd.isEmpty()) {
            this.set &= -129L;
         }

      });
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl putPermissionOverride(@Nonnull IPermissionHolder permHolder, long allow, long deny) {
      Checks.notNull(permHolder, "PermissionHolder");
      Checks.check(permHolder.getGuild().equals(this.getGuild()), "PermissionHolder is not from the same Guild!");
      if (isPermissionChecksEnabled() && !this.getGuild().getSelfMember().hasPermission(this.getChannel(), new Permission[]{Permission.MANAGE_PERMISSIONS})) {
         throw new InsufficientPermissionException(this.getChannel(), Permission.MANAGE_PERMISSIONS);
      } else {
         long id = this.getId(permHolder);
         int type = permHolder instanceof Role ? 0 : 1;
         this.withLock(this.lock, (lock) -> {
            this.overridesRem.remove(id);
            this.overridesAdd.put(id, new PermOverrideData(type, id, allow, deny));
            this.set |= 128L;
         });
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl removePermissionOverride(@Nonnull IPermissionHolder permHolder) {
      Checks.notNull(permHolder, "PermissionHolder");
      Checks.check(permHolder.getGuild().equals(this.getGuild()), "PermissionHolder is not from the same Guild!");
      if (isPermissionChecksEnabled() && !this.getGuild().getSelfMember().hasPermission(this.getChannel(), new Permission[]{Permission.MANAGE_PERMISSIONS})) {
         throw new InsufficientPermissionException(this.getChannel(), Permission.MANAGE_PERMISSIONS);
      } else {
         long id = this.getId(permHolder);
         this.withLock(this.lock, (lock) -> {
            this.overridesRem.add(id);
            this.overridesAdd.remove(id);
            this.set |= 128L;
         });
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl sync(@Nonnull GuildChannel syncSource) {
      Checks.notNull(syncSource, "SyncSource");
      Checks.check(this.getGuild().equals(syncSource.getGuild()), "Sync only works for channels of same guild");
      if (syncSource.equals(this.getChannel())) {
         return this;
      } else if (isPermissionChecksEnabled() && !this.getGuild().getSelfMember().hasPermission(this.getChannel(), new Permission[]{Permission.MANAGE_PERMISSIONS})) {
         throw new InsufficientPermissionException(this.getChannel(), Permission.MANAGE_PERMISSIONS);
      } else {
         this.withLock(this.lock, (lock) -> {
            this.overridesRem.clear();
            this.overridesAdd.clear();
            LongStream var10000 = this.getChannel().getPermissionOverrides().stream().mapToLong(ISnowflake::getIdLong);
            TLongSet var10001 = this.overridesRem;
            Objects.requireNonNull(var10001);
            var10000.forEach(var10001::add);
            syncSource.getPermissionOverrides().forEach((override) -> {
               int type = override.isRoleOverride() ? 0 : 1;
               long id = override.getIdLong();
               this.overridesRem.remove(id);
               this.overridesAdd.put(id, new PermOverrideData(type, id, override.getAllowedRaw(), override.getDeniedRaw()));
            });
            this.set |= 128L;
         });
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setName(@Nonnull String name) {
      Checks.notBlank(name, "Name");
      Checks.check(name.length() > 0 && name.length() <= 100, "Name must be between 1-100 characters long");
      if (this.getType() == ChannelType.TEXT) {
         Checks.noWhitespace(name, "Name");
      }

      this.name = name;
      this.set |= 1L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setParent(Category category) {
      if (category != null) {
         if (this.getType() == ChannelType.CATEGORY) {
            throw new IllegalStateException("Cannot set the parent of a category");
         }

         Checks.check(category.getGuild().equals(this.getGuild()), "Category is not from the same guild");
      }

      this.parent = category == null ? null : category.getId();
      this.set |= 2L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setPosition(int position) {
      this.position = position;
      this.set |= 8L;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setTopic(String topic) {
      if (this.getType() != ChannelType.TEXT) {
         throw new IllegalStateException("Can only set topic on text channels");
      } else {
         Checks.check(topic == null || topic.length() <= 1024, "Topic must be less or equal to 1024 characters in length");
         this.topic = topic;
         this.set |= 4L;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setNSFW(boolean nsfw) {
      if (this.getType() != ChannelType.TEXT) {
         throw new IllegalStateException("Can only set nsfw on text channels");
      } else {
         this.nsfw = nsfw;
         this.set |= 16L;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setSlowmode(int slowmode) {
      if (this.getType() != ChannelType.TEXT) {
         throw new IllegalStateException("Can only set slowmode on text channels");
      } else {
         Checks.check(slowmode <= 21600 && slowmode >= 0, "Slowmode per user must be between 0 and %d (seconds)!", (int)21600);
         this.slowmode = slowmode;
         this.set |= 256L;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setUserLimit(int userLimit) {
      if (this.getType() != ChannelType.VOICE) {
         throw new IllegalStateException("Can only set userlimit on voice channels");
      } else {
         Checks.notNegative(userLimit, "Userlimit");
         Checks.check(userLimit <= 99, "Userlimit may not be greater than 99");
         this.userlimit = userLimit;
         this.set |= 32L;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelManagerImpl setBitrate(int bitrate) {
      if (this.getType() != ChannelType.VOICE) {
         throw new IllegalStateException("Can only set bitrate on voice channels");
      } else {
         int maxBitrate = this.getGuild().getMaxBitrate();
         Checks.check(bitrate >= 8000, "Bitrate must be greater or equal to 8000");
         Checks.check(bitrate <= maxBitrate, "Bitrate must be less or equal to %s", (Object)maxBitrate);
         this.bitrate = bitrate;
         this.set |= 64L;
         return this;
      }
   }

   protected RequestBody finalizeData() {
      DataObject frame = DataObject.empty().put("name", this.getChannel().getName());
      if (this.shouldUpdate(1L)) {
         frame.put("name", this.name);
      }

      if (this.shouldUpdate(8L)) {
         frame.put("position", this.position);
      }

      if (this.shouldUpdate(4L)) {
         frame.put("topic", this.topic);
      }

      if (this.shouldUpdate(16L)) {
         frame.put("nsfw", this.nsfw);
      }

      if (this.shouldUpdate(256L)) {
         frame.put("rate_limit_per_user", this.slowmode);
      }

      if (this.shouldUpdate(32L)) {
         frame.put("user_limit", this.userlimit);
      }

      if (this.shouldUpdate(64L)) {
         frame.put("bitrate", this.bitrate);
      }

      if (this.shouldUpdate(2L)) {
         frame.put("parent_id", this.parent);
      }

      this.withLock(this.lock, (lock) -> {
         if (this.shouldUpdate(128L)) {
            frame.put("permission_overwrites", this.getOverrides());
         }

      });
      this.reset();
      return this.getRequestBody(frame);
   }

   protected boolean checkPermissions() {
      Member selfMember = this.getGuild().getSelfMember();
      GuildChannel channel = this.getChannel();
      if (!selfMember.hasPermission(channel, new Permission[]{Permission.VIEW_CHANNEL})) {
         throw new MissingAccessException(channel, Permission.VIEW_CHANNEL);
      } else if (!selfMember.hasAccess(channel)) {
         throw new MissingAccessException(channel, Permission.VOICE_CONNECT);
      } else if (!selfMember.hasPermission(channel, new Permission[]{Permission.MANAGE_CHANNEL})) {
         throw new InsufficientPermissionException(channel, Permission.MANAGE_CHANNEL);
      } else {
         return super.checkPermissions();
      }
   }

   protected Collection<PermOverrideData> getOverrides() {
      TLongObjectHashMap<PermOverrideData> data = new TLongObjectHashMap(this.overridesAdd);
      AbstractChannelImpl<?, ?> impl = (AbstractChannelImpl)this.getChannel();
      impl.getOverrideMap().forEachEntry((id, override) -> {
         if (!this.overridesRem.remove(id) && !data.containsKey(id)) {
            data.put(id, new PermOverrideData(override));
         }

         return true;
      });
      return data.valueCollection();
   }

   protected long getId(IPermissionHolder holder) {
      return holder instanceof Role ? ((Role)holder).getIdLong() : ((Member)holder).getUser().getIdLong();
   }
}

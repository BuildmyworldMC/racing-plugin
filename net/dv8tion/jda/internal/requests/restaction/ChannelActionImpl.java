package net.dv8tion.jda.internal.requests.restaction;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import okhttp3.RequestBody;

public class ChannelActionImpl<T extends GuildChannel> extends AuditableRestActionImpl<T> implements ChannelAction<T> {
   protected final TLongObjectMap<PermOverrideData> overrides = new TLongObjectHashMap();
   protected final Guild guild;
   protected final ChannelType type;
   protected final Class<T> clazz;
   protected String name;
   protected Category parent;
   protected Integer position;
   protected String topic = null;
   protected Boolean nsfw = null;
   protected Integer slowmode = null;
   protected Integer bitrate = null;
   protected Integer userlimit = null;

   public ChannelActionImpl(Class<T> clazz, String name, Guild guild, ChannelType type) {
      super(guild.getJDA(), Route.Guilds.CREATE_CHANNEL.compile(guild.getId()));
      this.clazz = clazz;
      this.guild = guild;
      this.type = type;
      this.name = name;
   }

   @Nonnull
   public ChannelActionImpl<T> setCheck(BooleanSupplier checks) {
      return (ChannelActionImpl)super.setCheck(checks);
   }

   @Nonnull
   public ChannelActionImpl<T> timeout(long timeout, @Nonnull TimeUnit unit) {
      return (ChannelActionImpl)super.timeout(timeout, unit);
   }

   @Nonnull
   public ChannelActionImpl<T> deadline(long timestamp) {
      return (ChannelActionImpl)super.deadline(timestamp);
   }

   @Nonnull
   public Guild getGuild() {
      return this.guild;
   }

   @Nonnull
   public ChannelType getType() {
      return this.type;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setName(@Nonnull String name) {
      Checks.notNull(name, "Channel name");
      if (name.length() >= 1 && name.length() <= 100) {
         this.name = name;
         return this;
      } else {
         throw new IllegalArgumentException("Provided channel name must be 1 to 100 characters in length");
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setParent(Category category) {
      Checks.check(category == null || category.getGuild().equals(this.guild), "Category is not from same guild!");
      this.parent = category;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setPosition(Integer position) {
      Checks.check(position == null || position >= 0, "Position must be >= 0!");
      this.position = position;
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setTopic(String topic) {
      if (this.type != ChannelType.TEXT) {
         throw new UnsupportedOperationException("Can only set the topic for a TextChannel!");
      } else if (topic != null && topic.length() > 1024) {
         throw new IllegalArgumentException("Channel Topic must not be greater than 1024 in length!");
      } else {
         this.topic = topic;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setNSFW(boolean nsfw) {
      if (this.type != ChannelType.TEXT) {
         throw new UnsupportedOperationException("Can only set nsfw for a TextChannel!");
      } else {
         this.nsfw = nsfw;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setSlowmode(int slowmode) {
      if (this.type != ChannelType.TEXT) {
         throw new UnsupportedOperationException("Can only set slowmode on text channels");
      } else {
         Checks.check(slowmode <= 21600 && slowmode >= 0, "Slowmode must be between 0 and %d (seconds)!", (int)21600);
         this.slowmode = slowmode;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> addMemberPermissionOverride(long userId, long allow, long deny) {
      return this.addOverride(userId, 1, allow, deny);
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> addRolePermissionOverride(long roleId, long allow, long deny) {
      return this.addOverride(roleId, 0, allow, deny);
   }

   private ChannelActionImpl<T> addOverride(long targetId, int type, long allow, long deny) {
      Checks.notNegative(allow, "Granted permissions value");
      Checks.notNegative(deny, "Denied permissions value");
      Checks.check(allow <= Permission.ALL_PERMISSIONS, "Specified allow value may not be greater than a full permission set");
      Checks.check(deny <= Permission.ALL_PERMISSIONS, "Specified deny value may not be greater than a full permission set");
      this.overrides.put(targetId, new PermOverrideData(type, targetId, allow, deny));
      return this;
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setBitrate(Integer bitrate) {
      if (this.type != ChannelType.VOICE) {
         throw new UnsupportedOperationException("Can only set the bitrate for a VoiceChannel!");
      } else {
         if (bitrate != null) {
            int maxBitrate = this.getGuild().getMaxBitrate();
            if (bitrate < 8000) {
               throw new IllegalArgumentException("Bitrate must be greater than 8000.");
            }

            if (bitrate > maxBitrate) {
               throw new IllegalArgumentException("Bitrate must be less than " + maxBitrate);
            }
         }

         this.bitrate = bitrate;
         return this;
      }
   }

   @Nonnull
   @CheckReturnValue
   public ChannelActionImpl<T> setUserlimit(Integer userlimit) {
      if (this.type != ChannelType.VOICE) {
         throw new UnsupportedOperationException("Can only set the userlimit for a VoiceChannel!");
      } else if (userlimit == null || userlimit >= 0 && userlimit <= 99) {
         this.userlimit = userlimit;
         return this;
      } else {
         throw new IllegalArgumentException("Userlimit must be between 0-99!");
      }
   }

   protected RequestBody finalizeData() {
      DataObject object = DataObject.empty();
      object.put("name", this.name);
      object.put("type", this.type.getId());
      object.put("permission_overwrites", DataArray.fromCollection(this.overrides.valueCollection()));
      if (this.position != null) {
         object.put("position", this.position);
      }

      switch(this.type) {
      case VOICE:
         if (this.bitrate != null) {
            object.put("bitrate", this.bitrate);
         }

         if (this.userlimit != null) {
            object.put("user_limit", this.userlimit);
         }
         break;
      case TEXT:
         if (this.topic != null && !this.topic.isEmpty()) {
            object.put("topic", this.topic);
         }

         if (this.nsfw != null) {
            object.put("nsfw", this.nsfw);
         }

         if (this.slowmode != null) {
            object.put("rate_limit_per_user", this.slowmode);
         }
      }

      if (this.type != ChannelType.CATEGORY && this.parent != null) {
         object.put("parent_id", this.parent.getId());
      }

      return this.getRequestBody(object);
   }

   protected void handleSuccess(Response response, Request<T> request) {
      EntityBuilder builder = this.api.getEntityBuilder();
      Object channel;
      switch(this.type) {
      case VOICE:
         channel = builder.createVoiceChannel(response.getObject(), this.guild.getIdLong());
         break;
      case TEXT:
         channel = builder.createTextChannel(response.getObject(), this.guild.getIdLong());
         break;
      case CATEGORY:
         channel = builder.createCategory(response.getObject(), this.guild.getIdLong());
         break;
      default:
         request.onFailure((Throwable)(new IllegalStateException("Created channel of unknown type!")));
         return;
      }

      request.onSuccess((GuildChannel)this.clazz.cast(channel));
   }
}

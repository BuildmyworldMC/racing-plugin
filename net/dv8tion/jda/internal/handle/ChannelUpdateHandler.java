package net.dv8tion.jda.internal.handle;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdatePermissionsEvent;
import net.dv8tion.jda.api.events.channel.category.update.CategoryUpdatePositionEvent;
import net.dv8tion.jda.api.events.channel.store.update.StoreChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.store.update.StoreChannelUpdatePermissionsEvent;
import net.dv8tion.jda.api.events.channel.store.update.StoreChannelUpdatePositionEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNSFWEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNewsEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateParentEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdatePermissionsEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdatePositionEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateSlowmodeEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateBitrateEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateParentEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdatePermissionsEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdatePositionEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.AbstractChannelImpl;
import net.dv8tion.jda.internal.entities.CategoryImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.entities.PermissionOverrideImpl;
import net.dv8tion.jda.internal.entities.StoreChannelImpl;
import net.dv8tion.jda.internal.entities.TextChannelImpl;
import net.dv8tion.jda.internal.entities.VoiceChannelImpl;
import net.dv8tion.jda.internal.requests.WebSocketClient;

public class ChannelUpdateHandler extends SocketHandler {
   public ChannelUpdateHandler(JDAImpl api) {
      super(api);
   }

   protected Long handleInternally(DataObject content) {
      int rawType = content.getInt("type");
      boolean news = rawType == 5;
      ChannelType type = ChannelType.fromId(rawType);
      if (type == ChannelType.GROUP) {
         WebSocketClient.LOG.warn("Ignoring CHANNEL_UPDATE for a group which we don't support");
         return null;
      } else {
         long channelId = content.getLong("id");
         Long parentId = content.isNull("parent_id") ? null : content.getLong("parent_id");
         int position = content.getInt("position");
         String name = content.getString("name");
         boolean nsfw = content.getBoolean("nsfw");
         int slowmode = content.getInt("rate_limit_per_user", 0);
         DataArray permOverwrites = content.getArray("permission_overwrites");
         String oldName;
         int bitrate;
         String oldTopic;
         int oldPosition;
         int oldSlowmode;
         switch(type) {
         case STORE:
            StoreChannelImpl storeChannel = (StoreChannelImpl)this.getJDA().getStoreChannelById(channelId);
            if (storeChannel == null) {
               this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug((String)"CHANNEL_UPDATE attempted to update a StoreChannel that does not exist. JSON: {}", (Object)content);
               return null;
            }

            oldName = storeChannel.getName();
            bitrate = storeChannel.getPositionRaw();
            if (!Objects.equals(oldName, name)) {
               storeChannel.setName(name);
               this.getJDA().handleEvent(new StoreChannelUpdateNameEvent(this.getJDA(), this.responseNumber, storeChannel, oldName));
            }

            if (!Objects.equals(bitrate, position)) {
               storeChannel.setPosition(position);
               this.getJDA().handleEvent(new StoreChannelUpdatePositionEvent(this.getJDA(), this.responseNumber, storeChannel, bitrate));
            }

            this.applyPermissions(storeChannel, permOverwrites);
            break;
         case TEXT:
            String topic = content.getString("topic", (String)null);
            TextChannelImpl textChannel = (TextChannelImpl)this.getJDA().getTextChannelsView().get(channelId);
            if (textChannel == null) {
               this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug((String)"CHANNEL_UPDATE attempted to update a TextChannel that does not exist. JSON: {}", (Object)content);
               return null;
            }

            Category parent = textChannel.getParent();
            Long oldParent = parent == null ? null : parent.getIdLong();
            String oldName = textChannel.getName();
            oldTopic = textChannel.getTopic();
            oldPosition = textChannel.getPositionRaw();
            boolean oldNsfw = textChannel.isNSFW();
            oldSlowmode = textChannel.getSlowmode();
            if (!Objects.equals(oldName, name)) {
               textChannel.setName(name);
               this.getJDA().handleEvent(new TextChannelUpdateNameEvent(this.getJDA(), this.responseNumber, textChannel, oldName));
            }

            if (!Objects.equals(oldParent, parentId)) {
               textChannel.setParent(parentId == null ? 0L : parentId);
               this.getJDA().handleEvent(new TextChannelUpdateParentEvent(this.getJDA(), this.responseNumber, textChannel, parent));
            }

            if (!Objects.equals(oldTopic, topic)) {
               textChannel.setTopic(topic);
               this.getJDA().handleEvent(new TextChannelUpdateTopicEvent(this.getJDA(), this.responseNumber, textChannel, oldTopic));
            }

            if (oldPosition != position) {
               textChannel.setPosition(position);
               this.getJDA().handleEvent(new TextChannelUpdatePositionEvent(this.getJDA(), this.responseNumber, textChannel, oldPosition));
            }

            if (oldNsfw != nsfw) {
               textChannel.setNSFW(nsfw);
               this.getJDA().handleEvent(new TextChannelUpdateNSFWEvent(this.getJDA(), this.responseNumber, textChannel, oldNsfw));
            }

            if (oldSlowmode != slowmode) {
               textChannel.setSlowmode(slowmode);
               this.getJDA().handleEvent(new TextChannelUpdateSlowmodeEvent(this.getJDA(), this.responseNumber, textChannel, oldSlowmode));
            }

            if (news != textChannel.isNews()) {
               textChannel.setNews(news);
               this.getJDA().handleEvent(new TextChannelUpdateNewsEvent(this.getJDA(), this.responseNumber, textChannel));
            }

            this.applyPermissions(textChannel, permOverwrites);
            break;
         case VOICE:
            VoiceChannelImpl voiceChannel = (VoiceChannelImpl)this.getJDA().getVoiceChannelsView().get(channelId);
            int userLimit = content.getInt("user_limit");
            bitrate = content.getInt("bitrate");
            if (voiceChannel == null) {
               this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug((String)"CHANNEL_UPDATE attempted to update a VoiceChannel that does not exist. JSON: {}", (Object)content);
               return null;
            }

            Category parent = voiceChannel.getParent();
            Long oldParent = parent == null ? null : parent.getIdLong();
            oldTopic = voiceChannel.getName();
            oldPosition = voiceChannel.getPositionRaw();
            int oldLimit = voiceChannel.getUserLimit();
            oldSlowmode = voiceChannel.getBitrate();
            if (!Objects.equals(oldTopic, name)) {
               voiceChannel.setName(name);
               this.getJDA().handleEvent(new VoiceChannelUpdateNameEvent(this.getJDA(), this.responseNumber, voiceChannel, oldTopic));
            }

            if (!Objects.equals(oldParent, parentId)) {
               voiceChannel.setParent(parentId == null ? 0L : parentId);
               this.getJDA().handleEvent(new VoiceChannelUpdateParentEvent(this.getJDA(), this.responseNumber, voiceChannel, parent));
            }

            if (oldPosition != position) {
               voiceChannel.setPosition(position);
               this.getJDA().handleEvent(new VoiceChannelUpdatePositionEvent(this.getJDA(), this.responseNumber, voiceChannel, oldPosition));
            }

            if (oldLimit != userLimit) {
               voiceChannel.setUserLimit(userLimit);
               this.getJDA().handleEvent(new VoiceChannelUpdateUserLimitEvent(this.getJDA(), this.responseNumber, voiceChannel, oldLimit));
            }

            if (oldSlowmode != bitrate) {
               voiceChannel.setBitrate(bitrate);
               this.getJDA().handleEvent(new VoiceChannelUpdateBitrateEvent(this.getJDA(), this.responseNumber, voiceChannel, oldSlowmode));
            }

            this.applyPermissions(voiceChannel, permOverwrites);
            break;
         case CATEGORY:
            CategoryImpl category = (CategoryImpl)this.getJDA().getCategoryById(channelId);
            if (category == null) {
               this.getJDA().getEventCache().cache(EventCache.Type.CHANNEL, channelId, this.responseNumber, this.allContent, this::handle);
               EventCache.LOG.debug((String)"CHANNEL_UPDATE attempted to update a Category that does not exist. JSON: {}", (Object)content);
               return null;
            }

            oldName = category.getName();
            bitrate = category.getPositionRaw();
            if (!Objects.equals(oldName, name)) {
               category.setName(name);
               this.getJDA().handleEvent(new CategoryUpdateNameEvent(this.getJDA(), this.responseNumber, category, oldName));
            }

            if (!Objects.equals(bitrate, position)) {
               category.setPosition(position);
               this.getJDA().handleEvent(new CategoryUpdatePositionEvent(this.getJDA(), this.responseNumber, category, bitrate));
            }

            this.applyPermissions(category, permOverwrites);
            break;
         default:
            WebSocketClient.LOG.debug((String)"CHANNEL_UPDATE provided an unrecognized channel type JSON: {}", (Object)content);
         }

         return null;
      }
   }

   private void applyPermissions(AbstractChannelImpl<?, ?> channel, DataArray permOverwrites) {
      TLongObjectMap<PermissionOverride> currentOverrides = new TLongObjectHashMap(channel.getOverrideMap());
      List<IPermissionHolder> changed = new ArrayList(currentOverrides.size());
      Guild guild = channel.getGuild();

      for(int i = 0; i < permOverwrites.length(); ++i) {
         DataObject overrideJson = permOverwrites.getObject(i);
         long id = overrideJson.getUnsignedLong("id", 0L);
         if (this.handlePermissionOverride((PermissionOverride)currentOverrides.remove(id), overrideJson, id, channel)) {
            this.addPermissionHolder(changed, guild, id);
         }
      }

      currentOverrides.forEachValue((override) -> {
         channel.getOverrideMap().remove(override.getIdLong());
         this.addPermissionHolder(changed, guild, override.getIdLong());
         this.api.handleEvent(new PermissionOverrideDeleteEvent(this.api, this.responseNumber, channel, override));
         return true;
      });
      if (!changed.isEmpty()) {
         switch(channel.getType()) {
         case STORE:
            this.api.handleEvent(new StoreChannelUpdatePermissionsEvent(this.api, this.responseNumber, (StoreChannel)channel, changed));
            break;
         case TEXT:
            this.api.handleEvent(new TextChannelUpdatePermissionsEvent(this.api, this.responseNumber, (TextChannel)channel, changed));
            break;
         case VOICE:
            this.api.handleEvent(new VoiceChannelUpdatePermissionsEvent(this.api, this.responseNumber, (VoiceChannel)channel, changed));
            break;
         case CATEGORY:
            this.api.handleEvent(new CategoryUpdatePermissionsEvent(this.api, this.responseNumber, (Category)channel, changed));
         }

      }
   }

   private void addPermissionHolder(List<IPermissionHolder> changed, Guild guild, long id) {
      IPermissionHolder holder = guild.getRoleById(id);
      if (holder == null) {
         holder = guild.getMemberById(id);
      }

      if (holder != null) {
         changed.add(holder);
      }

   }

   private boolean handlePermissionOverride(PermissionOverride currentOverride, DataObject override, long overrideId, AbstractChannelImpl<?, ?> channel) {
      long allow = override.getLong("allow_new");
      long deny = override.getLong("deny_new");
      String type = override.getString("type");
      boolean isRole = type.equals("role");
      if (!isRole) {
         if (!type.equals("member")) {
            EntityBuilder.LOG.debug((String)"Ignoring unknown invite of type '{}'. JSON: {}", (Object)type, (Object)override);
            return false;
         }

         if (!this.api.isCacheFlagSet(CacheFlag.MEMBER_OVERRIDES) && overrideId != this.api.getSelfUser().getIdLong()) {
            return false;
         }
      }

      if (currentOverride != null) {
         long oldAllow = currentOverride.getAllowedRaw();
         long oldDeny = currentOverride.getDeniedRaw();
         PermissionOverrideImpl impl = (PermissionOverrideImpl)currentOverride;
         if (oldAllow == allow && oldDeny == deny) {
            return false;
         }

         impl.setAllow(allow);
         impl.setDeny(deny);
         this.api.handleEvent(new PermissionOverrideUpdateEvent(this.api, this.responseNumber, channel, currentOverride, oldAllow, oldDeny));
      } else {
         PermissionOverrideImpl impl;
         PermissionOverride currentOverride = impl = new PermissionOverrideImpl(channel, overrideId, isRole);
         impl.setAllow(allow);
         impl.setDeny(deny);
         channel.getOverrideMap().put(overrideId, currentOverride);
         this.api.handleEvent(new PermissionOverrideCreateEvent(this.api, this.responseNumber, channel, currentOverride));
      }

      return true;
   }
}

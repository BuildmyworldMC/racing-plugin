package net.dv8tion.jda.internal.entities;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.ApplicationTeam;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.ClientType;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TeamMember;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.WebhookType;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateFlagsEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.handle.EventCache;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.MemberCacheViewImpl;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.slf4j.Logger;

public class EntityBuilder {
   public static final Logger LOG = JDALogger.getLog(EntityBuilder.class);
   public static final String MISSING_CHANNEL = "MISSING_CHANNEL";
   public static final String MISSING_USER = "MISSING_USER";
   public static final String UNKNOWN_MESSAGE_TYPE = "UNKNOWN_MESSAGE_TYPE";
   private static final Set<String> richGameFields;
   protected final JDAImpl api;

   public EntityBuilder(JDA api) {
      this.api = (JDAImpl)api;
   }

   public JDAImpl getJDA() {
      return this.api;
   }

   public SelfUser createSelfUser(DataObject self) {
      SelfUserImpl selfUser = (SelfUserImpl)(this.getJDA().hasSelfUser() ? this.getJDA().getSelfUser() : null);
      if (selfUser == null) {
         long id = self.getLong("id");
         selfUser = new SelfUserImpl(id, this.getJDA());
         this.getJDA().setSelfUser(selfUser);
      }

      SnowflakeCacheViewImpl<User> userView = this.getJDA().getUsersView();
      UnlockHook hook = userView.writeLock();

      try {
         if (userView.getElementById(selfUser.getIdLong()) == null) {
            userView.getMap().put(selfUser.getIdLong(), selfUser);
         }
      } catch (Throwable var8) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (hook != null) {
         hook.close();
      }

      selfUser.setVerified(self.getBoolean("verified")).setMfaEnabled(self.getBoolean("mfa_enabled")).setName(self.getString("username")).setDiscriminator(self.getString("discriminator")).setAvatarId(self.getString("avatar", (String)null)).setBot(self.getBoolean("bot"));
      return selfUser;
   }

   public static Activity createActivity(String name, String url, Activity.ActivityType type) {
      return new ActivityImpl(name, url, type);
   }

   private void createGuildEmotePass(GuildImpl guildObj, DataArray array) {
      if (this.getJDA().isCacheFlagSet(CacheFlag.EMOTE)) {
         SnowflakeCacheViewImpl<Emote> emoteView = guildObj.getEmotesView();
         UnlockHook hook = emoteView.writeLock();

         try {
            TLongObjectMap<Emote> emoteMap = emoteView.getMap();

            for(int i = 0; i < array.length(); ++i) {
               DataObject object = array.getObject(i);
               if (object.isNull("id")) {
                  LOG.error((String)"Received GUILD_CREATE with an emoji with a null ID. JSON: {}", (Object)object);
               } else {
                  long emoteId = object.getLong("id");
                  emoteMap.put(emoteId, this.createEmote(guildObj, object, false));
               }
            }
         } catch (Throwable var11) {
            if (hook != null) {
               try {
                  hook.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (hook != null) {
            hook.close();
         }

      }
   }

   public TLongObjectMap<DataObject> convertToUserMap(ToLongFunction<DataObject> getId, DataArray array) {
      TLongObjectMap<DataObject> map = new TLongObjectHashMap();

      for(int i = 0; i < array.length(); ++i) {
         DataObject obj = array.getObject(i);
         long userId = getId.applyAsLong(obj);
         map.put(userId, obj);
      }

      return map;
   }

   public GuildImpl createGuild(long guildId, DataObject guildJson, TLongObjectMap<DataObject> members, int memberCount) {
      GuildImpl guildObj = new GuildImpl(this.getJDA(), guildId);
      String name = guildJson.getString("name", "");
      String iconId = guildJson.getString("icon", (String)null);
      String splashId = guildJson.getString("splash", (String)null);
      String region = guildJson.getString("region", (String)null);
      String description = guildJson.getString("description", (String)null);
      String vanityCode = guildJson.getString("vanity_url_code", (String)null);
      String bannerId = guildJson.getString("banner", (String)null);
      String locale = guildJson.getString("preferred_locale", "en");
      DataArray roleArray = guildJson.getArray("roles");
      DataArray channelArray = guildJson.getArray("channels");
      DataArray emotesArray = guildJson.getArray("emojis");
      DataArray voiceStateArray = guildJson.getArray("voice_states");
      Optional<DataArray> featuresArray = guildJson.optArray("features");
      Optional<DataArray> presencesArray = guildJson.optArray("presences");
      long ownerId = guildJson.getUnsignedLong("owner_id", 0L);
      long afkChannelId = guildJson.getUnsignedLong("afk_channel_id", 0L);
      long systemChannelId = guildJson.getUnsignedLong("system_channel_id", 0L);
      int boostCount = guildJson.getInt("premium_subscription_count", 0);
      int boostTier = guildJson.getInt("premium_tier", 0);
      int maxMembers = guildJson.getInt("max_members", 0);
      int maxPresences = guildJson.getInt("max_presences", 5000);
      int mfaLevel = guildJson.getInt("mfa_level", 0);
      int afkTimeout = guildJson.getInt("afk_timeout", 0);
      int verificationLevel = guildJson.getInt("verification_level", 0);
      int notificationLevel = guildJson.getInt("default_message_notifications", 0);
      int explicitContentLevel = guildJson.getInt("explicit_content_filter", 0);
      guildObj.setAvailable(true).setName(name).setIconId(iconId).setSplashId(splashId).setRegion(region).setDescription(description).setBannerId(bannerId).setVanityCode(vanityCode).setMaxMembers(maxMembers).setMaxPresences(maxPresences).setOwnerId(ownerId).setAfkTimeout(Guild.Timeout.fromKey(afkTimeout)).setVerificationLevel(Guild.VerificationLevel.fromKey(verificationLevel)).setDefaultNotificationLevel(Guild.NotificationLevel.fromKey(notificationLevel)).setExplicitContentLevel(Guild.ExplicitContentLevel.fromKey(explicitContentLevel)).setRequiredMFALevel(Guild.MFALevel.fromKey(mfaLevel)).setLocale(locale).setBoostCount(boostCount).setBoostTier(boostTier).setMemberCount(memberCount);
      SnowflakeCacheViewImpl<Guild> guildView = this.getJDA().getGuildsView();
      UnlockHook hook = guildView.writeLock();

      try {
         guildView.getMap().put(guildId, guildObj);
      } catch (Throwable var52) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var49) {
               var52.addSuppressed(var49);
            }
         }

         throw var52;
      }

      if (hook != null) {
         hook.close();
      }

      guildObj.setFeatures((Set)featuresArray.map((it) -> {
         return (Set)StreamSupport.stream(it.spliterator(), false).map(String::valueOf).collect(Collectors.toSet());
      }).orElse(Collections.emptySet()));
      SnowflakeCacheViewImpl<Role> roleView = guildObj.getRolesView();
      UnlockHook hook = roleView.writeLock();

      TLongObjectMap presences;
      try {
         presences = roleView.getMap();

         for(int i = 0; i < roleArray.length(); ++i) {
            DataObject obj = roleArray.getObject(i);
            Role role = this.createRole(guildObj, obj, guildId);
            presences.put(role.getIdLong(), role);
            if (role.getIdLong() == guildObj.getIdLong()) {
               guildObj.setPublicRole(role);
            }
         }
      } catch (Throwable var55) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var48) {
               var55.addSuppressed(var48);
            }
         }

         throw var55;
      }

      if (hook != null) {
         hook.close();
      }

      for(int i = 0; i < channelArray.length(); ++i) {
         DataObject channelJson = channelArray.getObject(i);
         this.createGuildChannel(guildObj, channelJson);
      }

      TLongObjectMap<DataObject> voiceStates = this.convertToUserMap((o) -> {
         return o.getUnsignedLong("user_id", 0L);
      }, voiceStateArray);
      presences = (TLongObjectMap)presencesArray.map((o1) -> {
         return this.convertToUserMap((o2) -> {
            return o2.getObject("user").getUnsignedLong("id");
         }, o1);
      }).orElseGet(TLongObjectHashMap::new);
      UnlockHook h1 = guildObj.getMembersView().writeLock();

      try {
         UnlockHook h2 = this.getJDA().getUsersView().writeLock();

         try {
            Iterator var56 = members.valueCollection().iterator();

            while(var56.hasNext()) {
               DataObject memberJson = (DataObject)var56.next();
               long userId = memberJson.getObject("user").getUnsignedLong("id");
               DataObject voiceState = (DataObject)voiceStates.get(userId);
               DataObject presence = (DataObject)presences.get(userId);
               this.updateMemberCache(this.createMember(guildObj, memberJson, voiceState, presence));
            }
         } catch (Throwable var53) {
            if (h2 != null) {
               try {
                  h2.close();
               } catch (Throwable var51) {
                  var53.addSuppressed(var51);
               }
            }

            throw var53;
         }

         if (h2 != null) {
            h2.close();
         }
      } catch (Throwable var54) {
         if (h1 != null) {
            try {
               h1.close();
            } catch (Throwable var50) {
               var54.addSuppressed(var50);
            }
         }

         throw var54;
      }

      if (h1 != null) {
         h1.close();
      }

      if (guildObj.getOwner() == null) {
         LOG.debug((String)"Finished setup for guild with a null owner. GuildId: {} OwnerId: {}", (Object)guildId, (Object)guildJson.opt("owner_id").orElse((Object)null));
      }

      this.createGuildEmotePass(guildObj, emotesArray);
      guildObj.setAfkChannel(guildObj.getVoiceChannelById(afkChannelId)).setSystemChannel(guildObj.getTextChannelById(systemChannelId));
      return guildObj;
   }

   private void createGuildChannel(GuildImpl guildObj, DataObject channelData) {
      ChannelType channelType = ChannelType.fromId(channelData.getInt("type"));
      switch(channelType) {
      case TEXT:
         this.createTextChannel(guildObj, channelData, guildObj.getIdLong());
         break;
      case VOICE:
         this.createVoiceChannel(guildObj, channelData, guildObj.getIdLong());
         break;
      case CATEGORY:
         this.createCategory(guildObj, channelData, guildObj.getIdLong());
         break;
      case STORE:
         this.createStoreChannel(guildObj, channelData, guildObj.getIdLong());
         break;
      default:
         LOG.debug("Cannot create channel for type " + channelData.getInt("type"));
      }

   }

   public UserImpl createUser(DataObject user) {
      boolean newUser = false;
      long id = user.getLong("id");
      SnowflakeCacheViewImpl<User> userView = this.getJDA().getUsersView();
      UnlockHook hook = userView.readLock();

      UserImpl userObj;
      try {
         userObj = (UserImpl)userView.getElementById(id);
         if (userObj == null) {
            userObj = new UserImpl(id, this.getJDA());
            newUser = true;
         }
      } catch (Throwable var11) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (hook != null) {
         hook.close();
      }

      if (newUser) {
         userObj.setName(user.getString("username")).setDiscriminator(user.get("discriminator").toString()).setAvatarId(user.getString("avatar", (String)null)).setBot(user.getBoolean("bot")).setFlags(user.getInt("public_flags", 0));
      } else {
         this.updateUser(userObj, user);
      }

      return userObj;
   }

   public void updateUser(UserImpl userObj, DataObject user) {
      String oldName = userObj.getName();
      String newName = user.getString("username");
      String oldDiscriminator = userObj.getDiscriminator();
      String newDiscriminator = user.get("discriminator").toString();
      String oldAvatar = userObj.getAvatarId();
      String newAvatar = user.getString("avatar", (String)null);
      int oldFlags = userObj.getFlagsRaw();
      int newFlags = user.getInt("public_flags", 0);
      JDAImpl jda = this.getJDA();
      long responseNumber = jda.getResponseTotal();
      if (!oldName.equals(newName)) {
         userObj.setName(newName);
         jda.handleEvent(new UserUpdateNameEvent(jda, responseNumber, userObj, oldName));
      }

      if (!oldDiscriminator.equals(newDiscriminator)) {
         userObj.setDiscriminator(newDiscriminator);
         jda.handleEvent(new UserUpdateDiscriminatorEvent(jda, responseNumber, userObj, oldDiscriminator));
      }

      if (!Objects.equals(oldAvatar, newAvatar)) {
         userObj.setAvatarId(newAvatar);
         jda.handleEvent(new UserUpdateAvatarEvent(jda, responseNumber, userObj, oldAvatar));
      }

      if (oldFlags != newFlags) {
         userObj.setFlags(newFlags);
         jda.handleEvent(new UserUpdateFlagsEvent(jda, responseNumber, userObj, User.UserFlag.getFlags(oldFlags)));
      }

   }

   public boolean updateMemberCache(MemberImpl member) {
      return this.updateMemberCache(member, false);
   }

   public boolean updateMemberCache(MemberImpl member, boolean forceRemove) {
      GuildImpl guild = member.getGuild();
      UserImpl user = (UserImpl)member.getUser();
      MemberCacheViewImpl membersView = guild.getMembersView();
      if (!forceRemove && this.getJDA().cacheMember(member)) {
         if (guild.getMemberById(member.getIdLong()) != null) {
            return true;
         } else {
            LOG.trace((String)"Loading member {}", (Object)member);
            if (this.getJDA().getUserById(user.getIdLong()) == null) {
               SnowflakeCacheViewImpl<User> usersView = this.getJDA().getUsersView();
               UnlockHook hook1 = usersView.writeLock();

               try {
                  usersView.getMap().put(user.getIdLong(), user);
               } catch (Throwable var12) {
                  if (hook1 != null) {
                     try {
                        hook1.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }
                  }

                  throw var12;
               }

               if (hook1 != null) {
                  hook1.close();
               }
            }

            UnlockHook hook = membersView.writeLock();

            try {
               membersView.getMap().put(member.getIdLong(), member);
               if (member.isOwner()) {
                  guild.setOwner(member);
               }
            } catch (Throwable var13) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }
               }

               throw var13;
            }

            if (hook != null) {
               hook.close();
            }

            long hashId = guild.getIdLong() ^ user.getIdLong();
            this.getJDA().getEventCache().playbackCache(EventCache.Type.USER, member.getIdLong());
            this.getJDA().getEventCache().playbackCache(EventCache.Type.MEMBER, hashId);
            return true;
         }
      } else if (membersView.remove(member.getIdLong()) == null) {
         return false;
      } else {
         LOG.trace((String)"Unloading member {}", (Object)member);
         if (user.getMutualGuilds().isEmpty()) {
            user.setFake(true);
            this.getJDA().getUsersView().remove(user.getIdLong());
         }

         GuildVoiceStateImpl voiceState = (GuildVoiceStateImpl)member.getVoiceState();
         if (voiceState != null) {
            VoiceChannelImpl connectedChannel = (VoiceChannelImpl)voiceState.getChannel();
            if (connectedChannel != null) {
               connectedChannel.getConnectedMembersMap().remove(member.getIdLong());
            }

            voiceState.setConnectedChannel((VoiceChannel)null);
         }

         return false;
      }
   }

   public MemberImpl createMember(GuildImpl guild, DataObject memberJson) {
      return this.createMember(guild, memberJson, (DataObject)null, (DataObject)null);
   }

   public MemberImpl createMember(GuildImpl guild, DataObject memberJson, DataObject voiceStateJson, DataObject presence) {
      boolean playbackCache = false;
      User user = this.createUser(memberJson.getObject("user"));
      DataArray roleArray = memberJson.getArray("roles");
      MemberImpl member = (MemberImpl)guild.getMember(user);
      long joinEpoch;
      if (member == null) {
         member = new MemberImpl(guild, user);
         member.setNickname(memberJson.getString("nick", (String)null));
         long epoch = 0L;
         if (!memberJson.isNull("premium_since")) {
            TemporalAccessor date = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(memberJson.getString("premium_since"));
            epoch = Instant.from(date).toEpochMilli();
         }

         member.setBoostDate(epoch);
         Set<Role> roles = member.getRoleSet();

         for(int i = 0; i < roleArray.length(); ++i) {
            long roleId = roleArray.getUnsignedLong(i);
            Role role = guild.getRoleById(roleId);
            if (role != null) {
               roles.add(role);
            }
         }
      } else {
         List<Role> roles = new ArrayList(roleArray.length());

         for(int i = 0; i < roleArray.length(); ++i) {
            joinEpoch = roleArray.getUnsignedLong(i);
            Role role = guild.getRoleById(joinEpoch);
            if (role != null) {
               roles.add(role);
            }
         }

         this.updateMember(guild, member, memberJson, roles);
      }

      if (!memberJson.isNull("joined_at") && !member.hasTimeJoined()) {
         String joinedAtRaw = memberJson.getString("joined_at");
         TemporalAccessor joinedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(joinedAtRaw);
         joinEpoch = Instant.from(joinedAt).toEpochMilli();
         member.setJoinDate(joinEpoch);
      }

      if (voiceStateJson != null && member.getVoiceState() != null) {
         this.createVoiceState(guild, voiceStateJson, user, member);
      }

      if (presence != null) {
         this.createPresence(member, presence);
      }

      return member;
   }

   private void createVoiceState(GuildImpl guild, DataObject voiceStateJson, User user, MemberImpl member) {
      GuildVoiceStateImpl voiceState = (GuildVoiceStateImpl)member.getVoiceState();
      long channelId = voiceStateJson.getLong("channel_id");
      VoiceChannelImpl voiceChannel = (VoiceChannelImpl)guild.getVoiceChannelsView().get(channelId);
      if (voiceChannel != null) {
         voiceChannel.getConnectedMembersMap().put(member.getIdLong(), member);
      } else {
         LOG.error("Received a GuildVoiceState with a channel ID for a non-existent channel! ChannelId: {} GuildId: {} UserId: {}", channelId, guild.getId(), user.getId());
      }

      voiceState.setSelfMuted(voiceStateJson.getBoolean("self_mute")).setSelfDeafened(voiceStateJson.getBoolean("self_deaf")).setGuildMuted(voiceStateJson.getBoolean("mute")).setGuildDeafened(voiceStateJson.getBoolean("deaf")).setSuppressed(voiceStateJson.getBoolean("suppress")).setSessionId(voiceStateJson.getString("session_id")).setStream(voiceStateJson.getBoolean("self_stream")).setConnectedChannel(voiceChannel);
   }

   public void updateMember(GuildImpl guild, MemberImpl member, DataObject content, List<Role> newRoles) {
      long responseNumber = this.getJDA().getResponseTotal();
      if (newRoles != null) {
         this.updateMemberRoles(member, newRoles, responseNumber);
      }

      String joinedAtRaw;
      if (content.hasKey("nick")) {
         joinedAtRaw = member.getNickname();
         String newNick = content.getString("nick", (String)null);
         if (!Objects.equals(joinedAtRaw, newNick)) {
            member.setNickname(newNick);
            this.getJDA().handleEvent(new GuildMemberUpdateNicknameEvent(this.getJDA(), responseNumber, member, joinedAtRaw));
         }
      }

      if (content.hasKey("premium_since")) {
         long epoch = 0L;
         if (!content.isNull("premium_since")) {
            TemporalAccessor date = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(content.getString("premium_since"));
            epoch = Instant.from(date).toEpochMilli();
         }

         if (epoch != member.getBoostDateRaw()) {
            OffsetDateTime oldTime = member.getTimeBoosted();
            member.setBoostDate(epoch);
            this.getJDA().handleEvent(new GuildMemberUpdateBoostTimeEvent(this.getJDA(), responseNumber, member, oldTime));
         }
      }

      if (!content.isNull("joined_at") && !member.hasTimeJoined()) {
         joinedAtRaw = content.getString("joined_at");
         TemporalAccessor joinedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(joinedAtRaw);
         long joinEpoch = Instant.from(joinedAt).toEpochMilli();
         member.setJoinDate(joinEpoch);
      }

      this.updateUser((UserImpl)member.getUser(), content.getObject("user"));
   }

   private void updateMemberRoles(MemberImpl member, List<Role> newRoles, long responseNumber) {
      Set<Role> currentRoles = member.getRoleSet();
      List<Role> removedRoles = new LinkedList();
      Iterator var7 = currentRoles.iterator();

      while(true) {
         label37:
         while(var7.hasNext()) {
            Role role = (Role)var7.next();
            Iterator it = newRoles.iterator();

            while(it.hasNext()) {
               Role r = (Role)it.next();
               if (role.equals(r)) {
                  it.remove();
                  continue label37;
               }
            }

            removedRoles.add(role);
         }

         if (removedRoles.size() > 0) {
            currentRoles.removeAll(removedRoles);
         }

         if (newRoles.size() > 0) {
            currentRoles.addAll(newRoles);
         }

         if (removedRoles.size() > 0) {
            this.getJDA().handleEvent(new GuildMemberRoleRemoveEvent(this.getJDA(), responseNumber, member, removedRoles));
         }

         if (newRoles.size() > 0) {
            this.getJDA().handleEvent(new GuildMemberRoleAddEvent(this.getJDA(), responseNumber, member, newRoles));
         }

         return;
      }
   }

   public void createPresence(MemberImpl member, DataObject presenceJson) {
      if (member == null) {
         throw new NullPointerException("Provided member was null!");
      } else {
         boolean cacheGame = this.getJDA().isCacheFlagSet(CacheFlag.ACTIVITY);
         boolean cacheStatus = this.getJDA().isCacheFlagSet(CacheFlag.CLIENT_STATUS);
         DataArray activityArray = cacheGame && !presenceJson.isNull("activities") ? presenceJson.getArray("activities") : null;
         DataObject clientStatusJson = cacheStatus && !presenceJson.isNull("client_status") ? presenceJson.getObject("client_status") : null;
         OnlineStatus onlineStatus = OnlineStatus.fromKey(presenceJson.getString("status"));
         List<Activity> activities = new ArrayList();
         boolean parsedActivity = false;
         if (cacheGame && activityArray != null) {
            for(int i = 0; i < activityArray.length(); ++i) {
               try {
                  activities.add(createActivity(activityArray.getObject(i)));
                  parsedActivity = true;
               } catch (Exception var14) {
                  String userId = member.getUser().getId();
                  if (LOG.isDebugEnabled()) {
                     LOG.warn("Encountered exception trying to parse a presence! UserId: {} JSON: {}", userId, activityArray, var14);
                  } else {
                     LOG.warn((String)"Encountered exception trying to parse a presence! UserId: {} Message: {} Enable debug for details", (Object)userId, (Object)var14.getMessage());
                  }
               }
            }
         }

         if (cacheGame && parsedActivity) {
            member.setActivities(activities);
         }

         member.setOnlineStatus(onlineStatus);
         if (clientStatusJson != null) {
            Iterator var15 = clientStatusJson.keys().iterator();

            while(var15.hasNext()) {
               String key = (String)var15.next();
               ClientType type = ClientType.fromKey(key);
               OnlineStatus status = OnlineStatus.fromKey(clientStatusJson.getString(key));
               member.setOnlineStatus(type, status);
            }
         }

      }
   }

   public static Activity createActivity(DataObject gameJson) {
      String name = String.valueOf(gameJson.get("name"));
      String url = gameJson.isNull("url") ? null : String.valueOf(gameJson.get("url"));

      Activity.ActivityType type;
      try {
         type = gameJson.isNull("type") ? Activity.ActivityType.DEFAULT : Activity.ActivityType.fromKey(Integer.parseInt(gameJson.get("type").toString()));
      } catch (NumberFormatException var21) {
         type = Activity.ActivityType.DEFAULT;
      }

      Activity.Timestamps timestamps = null;
      long id;
      long emojiId;
      if (!gameJson.isNull("timestamps")) {
         DataObject obj = gameJson.getObject("timestamps");
         id = obj.getLong("start", 0L);
         emojiId = obj.getLong("end", 0L);
         timestamps = new Activity.Timestamps(id, emojiId);
      }

      Activity.Emoji emoji = null;
      if (!gameJson.isNull("emoji")) {
         DataObject emojiJson = gameJson.getObject("emoji");
         String emojiName = emojiJson.getString("name");
         emojiId = emojiJson.getUnsignedLong("id", 0L);
         boolean emojiAnimated = emojiJson.getBoolean("animated");
         emoji = new Activity.Emoji(emojiName, emojiId, emojiAnimated);
      }

      if (type == Activity.ActivityType.CUSTOM_STATUS && gameJson.hasKey("state") && name.equalsIgnoreCase("Custom Status")) {
         name = gameJson.getString("state", "");
         gameJson = gameJson.remove("state");
      }

      if (!CollectionUtils.containsAny(gameJson.keys(), richGameFields)) {
         return new ActivityImpl(name, url, type, timestamps, emoji);
      } else {
         id = gameJson.getLong("application_id", 0L);
         String sessionId = gameJson.getString("session_id", (String)null);
         String syncId = gameJson.getString("sync_id", (String)null);
         int flags = gameJson.getInt("flags", 0);
         String details = gameJson.isNull("details") ? null : String.valueOf(gameJson.get("details"));
         String state = gameJson.isNull("state") ? null : String.valueOf(gameJson.get("state"));
         RichPresence.Party party = null;
         String smallImageText;
         if (!gameJson.isNull("party")) {
            DataObject obj = gameJson.getObject("party");
            smallImageText = obj.isNull("id") ? null : obj.getString("id");
            DataArray sizeArr = obj.isNull("size") ? null : obj.getArray("size");
            long size = 0L;
            long max = 0L;
            if (sizeArr != null && sizeArr.length() > 0) {
               size = sizeArr.getLong(0);
               max = sizeArr.length() < 2 ? 0L : sizeArr.getLong(1);
            }

            party = new RichPresence.Party(smallImageText, size, max);
         }

         String smallImageKey = null;
         smallImageText = null;
         String largeImageKey = null;
         String largeImageText = null;
         if (!gameJson.isNull("assets")) {
            DataObject assets = gameJson.getObject("assets");
            if (!assets.isNull("small_image")) {
               smallImageKey = String.valueOf(assets.get("small_image"));
               smallImageText = assets.isNull("small_text") ? null : String.valueOf(assets.get("small_text"));
            }

            if (!assets.isNull("large_image")) {
               largeImageKey = String.valueOf(assets.get("large_image"));
               largeImageText = assets.isNull("large_text") ? null : String.valueOf(assets.get("large_text"));
            }
         }

         return new RichPresenceImpl(type, name, url, id, emoji, party, details, state, timestamps, syncId, sessionId, flags, largeImageKey, largeImageText, smallImageKey, smallImageText);
      }
   }

   public EmoteImpl createEmote(GuildImpl guildObj, DataObject json, boolean fake) {
      DataArray emoteRoles = (DataArray)json.optArray("roles").orElseGet(DataArray::empty);
      long emoteId = json.getLong("id");
      User user = json.isNull("user") ? null : this.createUser(json.getObject("user"));
      EmoteImpl emoteObj = (EmoteImpl)guildObj.getEmoteById(emoteId);
      if (emoteObj == null) {
         emoteObj = new EmoteImpl(emoteId, guildObj, fake);
      }

      Set<Role> roleSet = emoteObj.getRoleSet();
      roleSet.clear();

      for(int j = 0; j < emoteRoles.length(); ++j) {
         Role role = guildObj.getRoleById(emoteRoles.getString(j));
         if (role != null) {
            roleSet.add(role);
         }
      }

      if (user != null) {
         emoteObj.setUser(user);
      }

      return emoteObj.setName(json.getString("name", "")).setAnimated(json.getBoolean("animated")).setManaged(json.getBoolean("managed")).setAvailable(json.getBoolean("available", true));
   }

   public Category createCategory(DataObject json, long guildId) {
      return this.createCategory((GuildImpl)null, json, guildId);
   }

   public Category createCategory(GuildImpl guild, DataObject json, long guildId) {
      boolean playbackCache = false;
      long id = json.getLong("id");
      CategoryImpl channel = (CategoryImpl)this.getJDA().getCategoriesView().get(id);
      if (channel == null) {
         if (guild == null) {
            guild = (GuildImpl)this.getJDA().getGuildsView().get(guildId);
         }

         SnowflakeCacheViewImpl<Category> guildCategoryView = guild.getCategoriesView();
         SnowflakeCacheViewImpl<Category> categoryView = this.getJDA().getCategoriesView();
         UnlockHook glock = guildCategoryView.writeLock();

         try {
            UnlockHook jlock = categoryView.writeLock();

            try {
               channel = new CategoryImpl(id, guild);
               guildCategoryView.getMap().put(id, channel);
               playbackCache = categoryView.getMap().put(id, channel) == null;
            } catch (Throwable var17) {
               if (jlock != null) {
                  try {
                     jlock.close();
                  } catch (Throwable var16) {
                     var17.addSuppressed(var16);
                  }
               }

               throw var17;
            }

            if (jlock != null) {
               jlock.close();
            }
         } catch (Throwable var18) {
            if (glock != null) {
               try {
                  glock.close();
               } catch (Throwable var15) {
                  var18.addSuppressed(var15);
               }
            }

            throw var18;
         }

         if (glock != null) {
            glock.close();
         }
      }

      ((CategoryImpl)channel.setName(json.getString("name"))).setPosition(json.getInt("position"));
      this.createOverridesPass(channel, json.getArray("permission_overwrites"));
      if (playbackCache) {
         this.getJDA().getEventCache().playbackCache(EventCache.Type.CHANNEL, id);
      }

      return channel;
   }

   public StoreChannel createStoreChannel(DataObject json, long guildId) {
      return this.createStoreChannel((GuildImpl)null, json, guildId);
   }

   public StoreChannel createStoreChannel(GuildImpl guild, DataObject json, long guildId) {
      boolean playbackCache = false;
      long id = json.getLong("id");
      StoreChannelImpl channel = (StoreChannelImpl)this.getJDA().getStoreChannelsView().get(id);
      if (channel == null) {
         if (guild == null) {
            guild = (GuildImpl)this.getJDA().getGuildById(guildId);
         }

         SnowflakeCacheViewImpl<StoreChannel> guildStoreView = guild.getStoreChannelView();
         SnowflakeCacheViewImpl<StoreChannel> storeView = this.getJDA().getStoreChannelsView();
         UnlockHook glock = guildStoreView.writeLock();

         try {
            UnlockHook jlock = storeView.writeLock();

            try {
               channel = new StoreChannelImpl(id, guild);
               guildStoreView.getMap().put(id, channel);
               playbackCache = storeView.getMap().put(id, channel) == null;
            } catch (Throwable var17) {
               if (jlock != null) {
                  try {
                     jlock.close();
                  } catch (Throwable var16) {
                     var17.addSuppressed(var16);
                  }
               }

               throw var17;
            }

            if (jlock != null) {
               jlock.close();
            }
         } catch (Throwable var18) {
            if (glock != null) {
               try {
                  glock.close();
               } catch (Throwable var15) {
                  var18.addSuppressed(var15);
               }
            }

            throw var18;
         }

         if (glock != null) {
            glock.close();
         }
      }

      ((StoreChannelImpl)((StoreChannelImpl)channel.setParent(json.getLong("parent_id", 0L))).setName(json.getString("name"))).setPosition(json.getInt("position"));
      this.createOverridesPass(channel, json.getArray("permission_overwrites"));
      if (playbackCache) {
         this.getJDA().getEventCache().playbackCache(EventCache.Type.CHANNEL, id);
      }

      return channel;
   }

   public TextChannel createTextChannel(DataObject json, long guildId) {
      return this.createTextChannel((GuildImpl)null, json, guildId);
   }

   public TextChannel createTextChannel(GuildImpl guildObj, DataObject json, long guildId) {
      boolean playbackCache = false;
      long id = json.getLong("id");
      TextChannelImpl channel = (TextChannelImpl)this.getJDA().getTextChannelsView().get(id);
      if (channel == null) {
         if (guildObj == null) {
            guildObj = (GuildImpl)this.getJDA().getGuildsView().get(guildId);
         }

         SnowflakeCacheViewImpl<TextChannel> guildTextView = guildObj.getTextChannelsView();
         SnowflakeCacheViewImpl<TextChannel> textView = this.getJDA().getTextChannelsView();
         UnlockHook glock = guildTextView.writeLock();

         try {
            UnlockHook jlock = textView.writeLock();

            try {
               channel = new TextChannelImpl(id, guildObj);
               guildTextView.getMap().put(id, channel);
               playbackCache = textView.getMap().put(id, channel) == null;
            } catch (Throwable var17) {
               if (jlock != null) {
                  try {
                     jlock.close();
                  } catch (Throwable var16) {
                     var17.addSuppressed(var16);
                  }
               }

               throw var17;
            }

            if (jlock != null) {
               jlock.close();
            }
         } catch (Throwable var18) {
            if (glock != null) {
               try {
                  glock.close();
               } catch (Throwable var15) {
                  var18.addSuppressed(var15);
               }
            }

            throw var18;
         }

         if (glock != null) {
            glock.close();
         }
      }

      ((TextChannelImpl)((TextChannelImpl)channel.setParent(json.getLong("parent_id", 0L))).setLastMessageId(json.getLong("last_message_id", 0L)).setName(json.getString("name"))).setTopic(json.getString("topic", (String)null)).setPosition(json.getInt("position")).setNSFW(json.getBoolean("nsfw")).setNews(json.getInt("type") == 5).setSlowmode(json.getInt("rate_limit_per_user", 0));
      this.createOverridesPass(channel, json.getArray("permission_overwrites"));
      if (playbackCache) {
         this.getJDA().getEventCache().playbackCache(EventCache.Type.CHANNEL, id);
      }

      return channel;
   }

   public VoiceChannel createVoiceChannel(DataObject json, long guildId) {
      return this.createVoiceChannel((GuildImpl)null, json, guildId);
   }

   public VoiceChannel createVoiceChannel(GuildImpl guild, DataObject json, long guildId) {
      boolean playbackCache = false;
      long id = json.getLong("id");
      VoiceChannelImpl channel = (VoiceChannelImpl)this.getJDA().getVoiceChannelsView().get(id);
      if (channel == null) {
         if (guild == null) {
            guild = (GuildImpl)this.getJDA().getGuildsView().get(guildId);
         }

         SnowflakeCacheViewImpl<VoiceChannel> guildVoiceView = guild.getVoiceChannelsView();
         SnowflakeCacheViewImpl<VoiceChannel> voiceView = this.getJDA().getVoiceChannelsView();
         UnlockHook vlock = guildVoiceView.writeLock();

         try {
            UnlockHook jlock = voiceView.writeLock();

            try {
               channel = new VoiceChannelImpl(id, guild);
               guildVoiceView.getMap().put(id, channel);
               playbackCache = voiceView.getMap().put(id, channel) == null;
            } catch (Throwable var17) {
               if (jlock != null) {
                  try {
                     jlock.close();
                  } catch (Throwable var16) {
                     var17.addSuppressed(var16);
                  }
               }

               throw var17;
            }

            if (jlock != null) {
               jlock.close();
            }
         } catch (Throwable var18) {
            if (vlock != null) {
               try {
                  vlock.close();
               } catch (Throwable var15) {
                  var18.addSuppressed(var15);
               }
            }

            throw var18;
         }

         if (vlock != null) {
            vlock.close();
         }
      }

      ((VoiceChannelImpl)((VoiceChannelImpl)channel.setParent(json.getLong("parent_id", 0L))).setName(json.getString("name"))).setPosition(json.getInt("position")).setUserLimit(json.getInt("user_limit")).setBitrate(json.getInt("bitrate"));
      this.createOverridesPass(channel, json.getArray("permission_overwrites"));
      if (playbackCache) {
         this.getJDA().getEventCache().playbackCache(EventCache.Type.CHANNEL, id);
      }

      return channel;
   }

   public PrivateChannel createPrivateChannel(DataObject json) {
      return this.createPrivateChannel(json, false);
   }

   public PrivateChannel createPrivateChannel(DataObject json, boolean modifyCache) {
      long channelId = json.getUnsignedLong("id");
      PrivateChannel channel = this.api.getPrivateChannelById(channelId);
      if (channel != null) {
         return channel;
      } else {
         DataObject recipient = json.hasKey("recipients") ? json.getArray("recipients").getObject(0) : json.getObject("recipient");
         long userId = recipient.getLong("id");
         UserImpl user = (UserImpl)this.getJDA().getUserById(userId);
         if (user == null) {
            user = this.createUser(recipient);
         }

         return this.createPrivateChannel(json, user, modifyCache);
      }
   }

   public PrivateChannel createPrivateChannel(DataObject json, UserImpl user) {
      return this.createPrivateChannel(json, user, false);
   }

   public PrivateChannel createPrivateChannel(DataObject json, UserImpl user, boolean modifyCache) {
      long channelId = json.getLong("id");
      PrivateChannelImpl priv = (new PrivateChannelImpl(channelId, user)).setLastMessageId(json.getLong("last_message_id", 0L));
      user.setPrivateChannel(priv);
      if (modifyCache) {
         SnowflakeCacheViewImpl<PrivateChannel> privateView = this.getJDA().getPrivateChannelsView();
         UnlockHook hook = privateView.writeLock();

         try {
            privateView.getMap().put(channelId, priv);
         } catch (Throwable var12) {
            if (hook != null) {
               try {
                  hook.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (hook != null) {
            hook.close();
         }

         this.getJDA().getEventCache().playbackCache(EventCache.Type.CHANNEL, channelId);
      }

      return priv;
   }

   public void createOverridesPass(AbstractChannelImpl<?, ?> channel, DataArray overrides) {
      for(int i = 0; i < overrides.length(); ++i) {
         try {
            this.createPermissionOverride(overrides.getObject(i), channel);
         } catch (NoSuchElementException var5) {
            LOG.debug((String)"{}. Ignoring PermissionOverride.", (Object)var5.getMessage());
         } catch (IllegalArgumentException var6) {
            LOG.warn((String)"{}. Ignoring PermissionOverride.", (Object)var6.getMessage());
         }
      }

   }

   public Role createRole(GuildImpl guild, DataObject roleJson, long guildId) {
      boolean playbackCache = false;
      long id = roleJson.getLong("id");
      if (guild == null) {
         guild = (GuildImpl)this.getJDA().getGuildsView().get(guildId);
      }

      RoleImpl role = (RoleImpl)guild.getRolesView().get(id);
      if (role == null) {
         SnowflakeCacheViewImpl<Role> roleView = guild.getRolesView();
         UnlockHook hook = roleView.writeLock();

         try {
            role = new RoleImpl(id, guild);
            playbackCache = roleView.getMap().put(id, role) == null;
         } catch (Throwable var14) {
            if (hook != null) {
               try {
                  hook.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (hook != null) {
            hook.close();
         }
      }

      int color = roleJson.getInt("color");
      role.setName(roleJson.getString("name")).setRawPosition(roleJson.getInt("position")).setRawPermissions(roleJson.getLong("permissions_new")).setManaged(roleJson.getBoolean("managed")).setHoisted(roleJson.getBoolean("hoist")).setColor(color == 0 ? 536870911 : color).setMentionable(roleJson.getBoolean("mentionable")).setTags((DataObject)roleJson.optObject("tags").orElseGet(DataObject::empty));
      if (playbackCache) {
         this.getJDA().getEventCache().playbackCache(EventCache.Type.ROLE, id);
      }

      return role;
   }

   public Message createMessage(DataObject jsonObject) {
      return this.createMessage(jsonObject, false);
   }

   public Message createMessage(DataObject jsonObject, boolean modifyCache) {
      long channelId = jsonObject.getLong("channel_id");
      MessageChannel chan = this.getJDA().getTextChannelById(channelId);
      if (chan == null) {
         chan = this.getJDA().getPrivateChannelById(channelId);
      }

      if (chan == null) {
         throw new IllegalArgumentException("MISSING_CHANNEL");
      } else {
         return this.createMessage(jsonObject, (MessageChannel)chan, modifyCache);
      }
   }

   public Message createMessage(DataObject jsonObject, MessageChannel chan, boolean modifyCache) {
      long id = jsonObject.getLong("id");
      DataObject author = jsonObject.getObject("author");
      long authorId = author.getLong("id");
      MemberImpl member = null;
      if (chan.getType().isGuild() && !jsonObject.isNull("member")) {
         DataObject memberJson = jsonObject.getObject("member");
         memberJson.put("user", author);
         GuildChannel guildChannel = (GuildChannel)chan;
         Guild guild = guildChannel.getGuild();
         member = this.createMember((GuildImpl)guild, memberJson);
         if (modifyCache) {
            this.updateMemberCache(member);
         }
      }

      String content = jsonObject.getString("content", "");
      boolean fromWebhook = jsonObject.hasKey("webhook_id");
      boolean pinned = jsonObject.getBoolean("pinned");
      boolean tts = jsonObject.getBoolean("tts");
      boolean mentionsEveryone = jsonObject.getBoolean("mention_everyone");
      OffsetDateTime editTime = jsonObject.isNull("edited_timestamp") ? null : OffsetDateTime.parse(jsonObject.getString("edited_timestamp"));
      String nonce = jsonObject.isNull("nonce") ? null : jsonObject.get("nonce").toString();
      int flags = jsonObject.getInt("flags", 0);
      List<Message.Attachment> attachments = this.map(jsonObject, "attachments", this::createMessageAttachment);
      List<MessageEmbed> embeds = this.map(jsonObject, "embeds", this::createMessageEmbed);
      List<MessageReaction> reactions = this.map(jsonObject, "reactions", (obj) -> {
         return this.createMessageReaction(chan, id, obj);
      });
      MessageActivity activity = null;
      if (!jsonObject.isNull("activity")) {
         activity = createMessageActivity(jsonObject);
      }

      Object user;
      switch(chan.getType()) {
      case TEXT:
         Guild guild = ((TextChannel)chan).getGuild();
         if (member == null) {
            member = (MemberImpl)guild.getMemberById(authorId);
         }

         user = member != null ? member.getUser() : null;
         if (user == null) {
            if (!fromWebhook && modifyCache) {
               throw new IllegalArgumentException("MISSING_USER");
            }

            user = this.createUser(author);
         }
         break;
      case PRIVATE:
         if (authorId == this.getJDA().getSelfUser().getIdLong()) {
            user = this.getJDA().getSelfUser();
         } else {
            user = ((PrivateChannel)chan).getUser();
         }
         break;
      case GROUP:
         throw new IllegalStateException("Cannot build a message for a group channel, how did this even get here?");
      default:
         throw new IllegalArgumentException("Invalid Channel for creating a Message [" + chan.getType() + ']');
      }

      if (modifyCache && !fromWebhook) {
         this.updateUser((UserImpl)user, author);
      }

      TLongSet mentionedRoles = new TLongHashSet();
      TLongSet mentionedUsers = new TLongHashSet(this.map(jsonObject, "mentions", (o) -> {
         return o.getLong("id");
      }));
      Optional<DataArray> roleMentionArr = jsonObject.optArray("mention_roles");
      roleMentionArr.ifPresent((arr) -> {
         for(int i = 0; i < arr.length(); ++i) {
            mentionedRoles.add(arr.getLong(i));
         }

      });
      MessageType type = MessageType.fromId(jsonObject.getInt("type"));
      Message referencedMessage = null;
      if (!jsonObject.isNull("referenced_message")) {
         referencedMessage = this.createMessage(jsonObject.getObject("referenced_message"), chan, false);
         if (type == MessageType.DEFAULT) {
            type = MessageType.INLINE_REPLY;
         }
      }

      Object message;
      switch(type) {
      case INLINE_REPLY:
      case DEFAULT:
         message = new ReceivedMessage(id, chan, type, referencedMessage, fromWebhook, mentionsEveryone, mentionedUsers, mentionedRoles, tts, pinned, content, nonce, (User)user, member, activity, editTime, reactions, attachments, embeds, flags);
         break;
      case UNKNOWN:
         throw new IllegalArgumentException("UNKNOWN_MESSAGE_TYPE");
      default:
         message = new SystemMessage(id, chan, type, fromWebhook, mentionsEveryone, mentionedUsers, mentionedRoles, tts, pinned, content, nonce, (User)user, member, activity, editTime, reactions, attachments, embeds, flags);
      }

      GuildImpl guild = ((ReceivedMessage)message).isFromGuild() ? (GuildImpl)((ReceivedMessage)message).getGuild() : null;
      List<User> mentionedUsersList = new ArrayList();
      List<Member> mentionedMembersList = new ArrayList();
      DataArray userMentions = jsonObject.getArray("mentions");

      for(int i = 0; i < userMentions.length(); ++i) {
         DataObject mentionJson = userMentions.getObject(i);
         if (guild != null && !mentionJson.isNull("member")) {
            DataObject memberJson = mentionJson.getObject("member");
            mentionJson.remove("member");
            memberJson.put("user", mentionJson);
            Member mentionedMember = this.createMember(guild, memberJson);
            mentionedMembersList.add(mentionedMember);
            mentionedUsersList.add(mentionedMember.getUser());
         } else {
            User mentionedUser = this.createUser(mentionJson);
            mentionedUsersList.add(mentionedUser);
            if (guild != null) {
               Member mentionedMember = guild.getMember(mentionedUser);
               if (mentionedMember != null) {
                  mentionedMembersList.add(mentionedMember);
               }
            }
         }
      }

      ((ReceivedMessage)message).setMentions(mentionedUsersList, mentionedMembersList);
      return (Message)message;
   }

   private static MessageActivity createMessageActivity(DataObject jsonObject) {
      DataObject activityData = jsonObject.getObject("activity");
      MessageActivity.ActivityType activityType = MessageActivity.ActivityType.fromId(activityData.getInt("type"));
      String partyId = activityData.getString("party_id", (String)null);
      MessageActivity.Application application = null;
      if (!jsonObject.isNull("application")) {
         DataObject applicationData = jsonObject.getObject("application");
         String name = applicationData.getString("name");
         String description = applicationData.getString("description", "");
         String iconId = applicationData.getString("icon", (String)null);
         String coverId = applicationData.getString("cover_image", (String)null);
         long applicationId = applicationData.getLong("id");
         application = new MessageActivity.Application(name, description, iconId, coverId, applicationId);
      }

      if (activityType == MessageActivity.ActivityType.UNKNOWN) {
         LOG.debug((String)"Received an unknown ActivityType, Activity: {}", (Object)activityData);
      }

      return new MessageActivity(activityType, partyId, application);
   }

   public MessageReaction createMessageReaction(MessageChannel chan, long id, DataObject obj) {
      DataObject emoji = obj.getObject("emoji");
      Long emojiID = emoji.isNull("id") ? null : emoji.getLong("id");
      String name = emoji.getString("name", "");
      boolean animated = emoji.getBoolean("animated");
      int count = obj.getInt("count", -1);
      boolean me = obj.getBoolean("me");
      MessageReaction.ReactionEmote reactionEmote;
      if (emojiID != null) {
         Emote emote = this.getJDA().getEmoteById(emojiID);
         if (emote == null) {
            emote = (new EmoteImpl(emojiID, this.getJDA())).setAnimated(animated).setName(name);
         }

         reactionEmote = MessageReaction.ReactionEmote.fromCustom((Emote)emote);
      } else {
         reactionEmote = MessageReaction.ReactionEmote.fromUnicode(name, this.getJDA());
      }

      return new MessageReaction(chan, reactionEmote, id, me, count);
   }

   public Message.Attachment createMessageAttachment(DataObject jsonObject) {
      int width = jsonObject.getInt("width", -1);
      int height = jsonObject.getInt("height", -1);
      int size = jsonObject.getInt("size");
      String url = jsonObject.getString("url");
      String proxyUrl = jsonObject.getString("proxy_url");
      String filename = jsonObject.getString("filename");
      long id = jsonObject.getLong("id");
      return new Message.Attachment(id, url, proxyUrl, filename, size, height, width, this.getJDA());
   }

   public MessageEmbed createMessageEmbed(DataObject content) {
      if (content.isNull("type")) {
         throw new IllegalStateException("Encountered embed object with missing/null type field for Json: " + content);
      } else {
         EmbedType type = EmbedType.fromKey(content.getString("type"));
         String url = content.getString("url", (String)null);
         String title = content.getString("title", (String)null);
         String description = content.getString("description", (String)null);
         OffsetDateTime timestamp = content.isNull("timestamp") ? null : OffsetDateTime.parse(content.getString("timestamp"));
         int color = content.isNull("color") ? 536870911 : content.getInt("color");
         MessageEmbed.Thumbnail thumbnail;
         if (content.isNull("thumbnail")) {
            thumbnail = null;
         } else {
            DataObject obj = content.getObject("thumbnail");
            thumbnail = new MessageEmbed.Thumbnail(obj.getString("url", (String)null), obj.getString("proxy_url", (String)null), obj.getInt("width", -1), obj.getInt("height", -1));
         }

         MessageEmbed.Provider provider;
         if (content.isNull("provider")) {
            provider = null;
         } else {
            DataObject obj = content.getObject("provider");
            provider = new MessageEmbed.Provider(obj.getString("name", (String)null), obj.getString("url", (String)null));
         }

         MessageEmbed.AuthorInfo author;
         if (content.isNull("author")) {
            author = null;
         } else {
            DataObject obj = content.getObject("author");
            author = new MessageEmbed.AuthorInfo(obj.getString("name", (String)null), obj.getString("url", (String)null), obj.getString("icon_url", (String)null), obj.getString("proxy_icon_url", (String)null));
         }

         MessageEmbed.VideoInfo video;
         if (content.isNull("video")) {
            video = null;
         } else {
            DataObject obj = content.getObject("video");
            video = new MessageEmbed.VideoInfo(obj.getString("url", (String)null), obj.getInt("width", -1), obj.getInt("height", -1));
         }

         MessageEmbed.Footer footer;
         if (content.isNull("footer")) {
            footer = null;
         } else {
            DataObject obj = content.getObject("footer");
            footer = new MessageEmbed.Footer(obj.getString("text", (String)null), obj.getString("icon_url", (String)null), obj.getString("proxy_icon_url", (String)null));
         }

         MessageEmbed.ImageInfo image;
         if (content.isNull("image")) {
            image = null;
         } else {
            DataObject obj = content.getObject("image");
            image = new MessageEmbed.ImageInfo(obj.getString("url", (String)null), obj.getString("proxy_url", (String)null), obj.getInt("width", -1), obj.getInt("height", -1));
         }

         List<MessageEmbed.Field> fields = this.map(content, "fields", (objx) -> {
            return new MessageEmbed.Field(objx.getString("name", (String)null), objx.getString("value", (String)null), objx.getBoolean("inline"), false);
         });
         return createMessageEmbed(url, title, description, type, timestamp, color, thumbnail, provider, author, video, footer, image, fields);
      }
   }

   public static MessageEmbed createMessageEmbed(String url, String title, String description, EmbedType type, OffsetDateTime timestamp, int color, MessageEmbed.Thumbnail thumbnail, MessageEmbed.Provider siteProvider, MessageEmbed.AuthorInfo author, MessageEmbed.VideoInfo videoInfo, MessageEmbed.Footer footer, MessageEmbed.ImageInfo image, List<MessageEmbed.Field> fields) {
      return new MessageEmbed(url, title, description, type, timestamp, color, thumbnail, siteProvider, author, videoInfo, footer, image, fields);
   }

   @Nullable
   public PermissionOverride createPermissionOverride(DataObject override, AbstractChannelImpl<?, ?> chan) {
      String type = override.getString("type");
      long id = override.getLong("id");
      boolean role = type.equals("role");
      if (role && chan.getGuild().getRoleById(id) == null) {
         throw new NoSuchElementException("Attempted to create a PermissionOverride for a non-existent role! JSON: " + override);
      } else if (!role && !type.equals("member")) {
         throw new IllegalArgumentException("Provided with an unknown PermissionOverride type! JSON: " + override);
      } else if (!role && id != this.api.getSelfUser().getIdLong() && !this.api.isCacheFlagSet(CacheFlag.MEMBER_OVERRIDES)) {
         return null;
      } else {
         long allow = override.getLong("allow_new");
         long deny = override.getLong("deny_new");
         PermissionOverrideImpl permOverride = (PermissionOverrideImpl)chan.getOverrideMap().get(id);
         if (permOverride == null) {
            permOverride = new PermissionOverrideImpl(chan, id, role);
            chan.getOverrideMap().put(id, permOverride);
         }

         return permOverride.setAllow(allow).setDeny(deny);
      }
   }

   public WebhookImpl createWebhook(DataObject object) {
      long id = object.getLong("id");
      long guildId = object.getUnsignedLong("guild_id");
      long channelId = object.getUnsignedLong("channel_id");
      String token = object.getString("token", (String)null);
      WebhookType type = WebhookType.fromKey(object.getInt("type", -1));
      TextChannel channel = this.getJDA().getTextChannelById(channelId);
      if (channel == null) {
         throw new NullPointerException(String.format("Tried to create Webhook for an un-cached TextChannel! WebhookId: %s ChannelId: %s GuildId: %s", id, channelId, guildId));
      } else {
         Object name = !object.isNull("name") ? object.get("name") : null;
         Object avatar = !object.isNull("avatar") ? object.get("avatar") : null;
         DataObject fakeUser = DataObject.empty().put("username", name).put("discriminator", "0000").put("id", id).put("avatar", avatar);
         User defaultUser = this.createUser(fakeUser);
         Optional<DataObject> ownerJson = object.optObject("user");
         User owner = null;
         if (ownerJson.isPresent()) {
            DataObject json = (DataObject)ownerJson.get();
            long userId = json.getLong("id");
            owner = this.getJDA().getUserById(userId);
            if (owner == null) {
               json.put("id", userId);
               owner = this.createUser(json);
            }
         }

         return (new WebhookImpl(channel, id, type)).setToken(token).setOwner(owner == null ? null : channel.getGuild().getMember((User)owner)).setUser(defaultUser);
      }
   }

   public Invite createInvite(DataObject object) {
      String code = object.getString("code");
      User inviter = object.hasKey("inviter") ? this.createUser(object.getObject("inviter")) : null;
      DataObject channelObject = object.getObject("channel");
      ChannelType channelType = ChannelType.fromId(channelObject.getInt("type"));
      Invite.InviteType type;
      InviteImpl.GuildImpl guild;
      InviteImpl.ChannelImpl channel;
      InviteImpl.GroupImpl group;
      if (channelType == ChannelType.GROUP) {
         type = Invite.InviteType.GROUP;
         guild = null;
         channel = null;
         String groupName = channelObject.getString("name", "");
         long groupId = channelObject.getLong("id");
         String groupIconId = channelObject.getString("icon", (String)null);
         List usernames;
         if (channelObject.isNull("recipients")) {
            usernames = null;
         } else {
            usernames = this.map(channelObject, "recipients", (json) -> {
               return json.getString("username");
            });
         }

         group = new InviteImpl.GroupImpl(groupIconId, groupName, groupId, usernames);
      } else if (channelType.isGuild()) {
         type = Invite.InviteType.GUILD;
         DataObject guildObject = object.getObject("guild");
         String guildIconId = guildObject.getString("icon", (String)null);
         long guildId = guildObject.getLong("id");
         String guildName = guildObject.getString("name");
         String guildSplashId = guildObject.getString("splash", (String)null);
         Guild.VerificationLevel guildVerificationLevel = Guild.VerificationLevel.fromKey(guildObject.getInt("verification_level", -1));
         int presenceCount = object.getInt("approximate_presence_count", -1);
         int memberCount = object.getInt("approximate_member_count", -1);
         Set guildFeatures;
         if (guildObject.isNull("features")) {
            guildFeatures = Collections.emptySet();
         } else {
            guildFeatures = Collections.unmodifiableSet((Set)StreamSupport.stream(guildObject.getArray("features").spliterator(), false).map(String::valueOf).collect(Collectors.toSet()));
         }

         guild = new InviteImpl.GuildImpl(guildId, guildIconId, guildName, guildSplashId, guildVerificationLevel, presenceCount, memberCount, guildFeatures);
         String channelName = channelObject.getString("name");
         long channelId = channelObject.getLong("id");
         channel = new InviteImpl.ChannelImpl(channelId, channelName, channelType);
         group = null;
      } else {
         type = Invite.InviteType.UNKNOWN;
         guild = null;
         channel = null;
         group = null;
      }

      int maxAge;
      int maxUses;
      boolean temporary;
      OffsetDateTime timeCreated;
      boolean expanded;
      int uses;
      if (object.hasKey("max_uses")) {
         expanded = true;
         maxAge = object.getInt("max_age");
         maxUses = object.getInt("max_uses");
         uses = object.getInt("uses");
         temporary = object.getBoolean("temporary");
         timeCreated = OffsetDateTime.parse(object.getString("created_at"));
      } else {
         expanded = false;
         maxAge = -1;
         maxUses = -1;
         uses = -1;
         temporary = false;
         timeCreated = null;
      }

      return new InviteImpl(this.getJDA(), code, expanded, inviter, maxAge, maxUses, temporary, timeCreated, uses, channel, guild, group, type);
   }

   public ApplicationInfo createApplicationInfo(DataObject object) {
      String description = object.getString("description");
      boolean doesBotRequireCodeGrant = object.getBoolean("bot_require_code_grant");
      String iconId = object.getString("icon", (String)null);
      long id = object.getLong("id");
      String name = object.getString("name");
      boolean isBotPublic = object.getBoolean("bot_public");
      User owner = this.createUser(object.getObject("owner"));
      ApplicationTeam team = !object.isNull("team") ? this.createApplicationTeam(object.getObject("team")) : null;
      return new ApplicationInfoImpl(this.getJDA(), description, doesBotRequireCodeGrant, iconId, id, isBotPublic, name, owner, team);
   }

   public ApplicationTeam createApplicationTeam(DataObject object) {
      String iconId = object.getString("icon", (String)null);
      long id = object.getUnsignedLong("id");
      long ownerId = object.getUnsignedLong("owner_user_id", 0L);
      List<TeamMember> members = this.map(object, "members", (o) -> {
         DataObject userJson = o.getObject("user");
         TeamMember.MembershipState state = TeamMember.MembershipState.fromKey(o.getInt("membership_state"));
         User user = this.createUser(userJson);
         return new TeamMemberImpl(user, state, id);
      });
      return new ApplicationTeamImpl(iconId, members, id, ownerId);
   }

   public AuditLogEntry createAuditLogEntry(GuildImpl guild, DataObject entryJson, DataObject userJson, DataObject webhookJson) {
      long targetId = entryJson.getLong("target_id", 0L);
      long id = entryJson.getLong("id");
      int typeKey = entryJson.getInt("action_type");
      DataArray changes = entryJson.isNull("changes") ? null : entryJson.getArray("changes");
      DataObject options = entryJson.isNull("options") ? null : entryJson.getObject("options");
      String reason = entryJson.getString("reason", (String)null);
      UserImpl user = userJson == null ? null : this.createUser(userJson);
      WebhookImpl webhook = webhookJson == null ? null : this.createWebhook(webhookJson);
      ActionType type = ActionType.from(typeKey);
      Object changesList;
      if (changes != null) {
         changesList = new HashSet(changes.length());

         for(int i = 0; i < changes.length(); ++i) {
            DataObject object = changes.getObject(i);
            AuditLogChange change = this.createAuditLogChange(object);
            ((Set)changesList).add(change);
         }
      } else {
         changesList = Collections.emptySet();
      }

      CaseInsensitiveMap<String, AuditLogChange> changeMap = new CaseInsensitiveMap(this.changeToMap((Set)changesList));
      CaseInsensitiveMap<String, Object> optionMap = options != null ? new CaseInsensitiveMap(options.toMap()) : null;
      return new AuditLogEntry(type, typeKey, id, targetId, guild, user, webhook, reason, changeMap, optionMap);
   }

   public AuditLogChange createAuditLogChange(DataObject change) {
      String key = change.getString("key");
      Object oldValue = change.isNull("old_value") ? null : change.get("old_value");
      Object newValue = change.isNull("new_value") ? null : change.get("new_value");
      return new AuditLogChange(oldValue, newValue, key);
   }

   private Map<String, AuditLogChange> changeToMap(Set<AuditLogChange> changesList) {
      return (Map)changesList.stream().collect(Collectors.toMap(AuditLogChange::getKey, UnaryOperator.identity()));
   }

   private <T> List<T> map(DataObject jsonObject, String key, Function<DataObject, T> convert) {
      if (jsonObject.isNull(key)) {
         return Collections.emptyList();
      } else {
         DataArray arr = jsonObject.getArray(key);
         List<T> mappedObjects = new ArrayList(arr.length());

         for(int i = 0; i < arr.length(); ++i) {
            DataObject obj = arr.getObject(i);
            T result = convert.apply(obj);
            if (result != null) {
               mappedObjects.add(result);
            }
         }

         return mappedObjects;
      }
   }

   static {
      Set<String> tmp = new HashSet();
      tmp.add("application_id");
      tmp.add("assets");
      tmp.add("details");
      tmp.add("flags");
      tmp.add("party");
      tmp.add("session_id");
      tmp.add("state");
      tmp.add("sync_id");
      richGameFields = Collections.unmodifiableSet(tmp);
   }
}

package net.dv8tion.jda.internal.entities;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.ListedEmote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.GuildManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MemberAction;
import net.dv8tion.jda.api.requests.restaction.RoleAction;
import net.dv8tion.jda.api.requests.restaction.order.CategoryOrderAction;
import net.dv8tion.jda.api.requests.restaction.order.ChannelOrderAction;
import net.dv8tion.jda.api.requests.restaction.order.RoleOrderAction;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.cache.MemberCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView;
import net.dv8tion.jda.api.utils.concurrent.Task;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.managers.AudioManagerImpl;
import net.dv8tion.jda.internal.managers.GuildManagerImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.DeferredRestAction;
import net.dv8tion.jda.internal.requests.MemberChunkManager;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.WebSocketClient;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.requests.restaction.ChannelActionImpl;
import net.dv8tion.jda.internal.requests.restaction.MemberActionImpl;
import net.dv8tion.jda.internal.requests.restaction.RoleActionImpl;
import net.dv8tion.jda.internal.requests.restaction.order.CategoryOrderActionImpl;
import net.dv8tion.jda.internal.requests.restaction.order.ChannelOrderActionImpl;
import net.dv8tion.jda.internal.requests.restaction.order.RoleOrderActionImpl;
import net.dv8tion.jda.internal.requests.restaction.pagination.AuditLogPaginationActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.EncodingUtil;
import net.dv8tion.jda.internal.utils.Helpers;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.AbstractCacheView;
import net.dv8tion.jda.internal.utils.cache.MemberCacheViewImpl;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;
import net.dv8tion.jda.internal.utils.cache.SortedSnowflakeCacheViewImpl;
import net.dv8tion.jda.internal.utils.concurrent.task.GatewayTask;

public class GuildImpl implements Guild {
   private final long id;
   private final JDAImpl api;
   private final SortedSnowflakeCacheViewImpl<Category> categoryCache = new SortedSnowflakeCacheViewImpl(Category.class, GuildChannel::getName, Comparator.naturalOrder());
   private final SortedSnowflakeCacheViewImpl<VoiceChannel> voiceChannelCache = new SortedSnowflakeCacheViewImpl(VoiceChannel.class, GuildChannel::getName, Comparator.naturalOrder());
   private final SortedSnowflakeCacheViewImpl<StoreChannel> storeChannelCache = new SortedSnowflakeCacheViewImpl(StoreChannel.class, GuildChannel::getName, Comparator.naturalOrder());
   private final SortedSnowflakeCacheViewImpl<TextChannel> textChannelCache = new SortedSnowflakeCacheViewImpl(TextChannel.class, GuildChannel::getName, Comparator.naturalOrder());
   private final SortedSnowflakeCacheViewImpl<Role> roleCache = new SortedSnowflakeCacheViewImpl(Role.class, Role::getName, Comparator.reverseOrder());
   private final SnowflakeCacheViewImpl<Emote> emoteCache = new SnowflakeCacheViewImpl(Emote.class, Emote::getName);
   private final MemberCacheViewImpl memberCache = new MemberCacheViewImpl();
   private final ReentrantLock mngLock = new ReentrantLock();
   private volatile GuildManager manager;
   private Member owner;
   private String name;
   private String iconId;
   private String splashId;
   private String region;
   private String vanityCode;
   private String description;
   private String banner;
   private int maxPresences;
   private int maxMembers;
   private int boostCount;
   private long ownerId;
   private Set<String> features;
   private VoiceChannel afkChannel;
   private TextChannel systemChannel;
   private Role publicRole;
   private Guild.VerificationLevel verificationLevel;
   private Guild.NotificationLevel defaultNotificationLevel;
   private Guild.MFALevel mfaLevel;
   private Guild.ExplicitContentLevel explicitContentLevel;
   private Guild.Timeout afkTimeout;
   private Guild.BoostTier boostTier;
   private Locale preferredLocale;
   private boolean available;
   private boolean canSendVerification;
   private int memberCount;

   public GuildImpl(JDAImpl api, long id) {
      this.verificationLevel = Guild.VerificationLevel.UNKNOWN;
      this.defaultNotificationLevel = Guild.NotificationLevel.UNKNOWN;
      this.mfaLevel = Guild.MFALevel.UNKNOWN;
      this.explicitContentLevel = Guild.ExplicitContentLevel.UNKNOWN;
      this.boostTier = Guild.BoostTier.NONE;
      this.preferredLocale = Locale.ENGLISH;
      this.canSendVerification = false;
      this.id = id;
      this.api = api;
   }

   @Nonnull
   public RestAction<EnumSet<Region>> retrieveRegions(boolean includeDeprecated) {
      Route.CompiledRoute route = Route.Guilds.GET_VOICE_REGIONS.compile(this.getId());
      return new RestActionImpl(this.getJDA(), route, (response, request) -> {
         EnumSet<Region> set = EnumSet.noneOf(Region.class);
         DataArray arr = response.getArray();

         for(int i = 0; i < arr.length(); ++i) {
            DataObject obj = arr.getObject(i);
            if (includeDeprecated || !obj.getBoolean("deprecated")) {
               String id = obj.getString("id", "");
               Region region = Region.fromKey(id);
               if (region != Region.UNKNOWN) {
                  set.add(region);
               }
            }
         }

         return set;
      });
   }

   @Nonnull
   public MemberAction addMember(@Nonnull String accessToken, @Nonnull String userId) {
      Checks.notBlank(accessToken, "Access-Token");
      Checks.isSnowflake(userId, "User ID");
      Checks.check(this.getMemberById(userId) == null, "User is already in this guild");
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.CREATE_INSTANT_INVITE})) {
         throw new InsufficientPermissionException(this, Permission.CREATE_INSTANT_INVITE);
      } else {
         return new MemberActionImpl(this.getJDA(), this, userId, accessToken);
      }
   }

   public boolean isLoaded() {
      return this.getJDA().isIntent(GatewayIntent.GUILD_MEMBERS) && (long)this.getMemberCount() <= this.getMemberCache().size();
   }

   public void pruneMemberCache() {
      UnlockHook h = this.memberCache.writeLock();

      try {
         EntityBuilder builder = this.getJDA().getEntityBuilder();
         Set<Member> members = this.memberCache.asSet();
         members.forEach((m) -> {
            builder.updateMemberCache((MemberImpl)m);
         });
      } catch (Throwable var5) {
         if (h != null) {
            try {
               h.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (h != null) {
         h.close();
      }

   }

   public boolean unloadMember(long userId) {
      if (userId == this.api.getSelfUser().getIdLong()) {
         return false;
      } else {
         MemberImpl member = (MemberImpl)this.getMemberById(userId);
         if (member == null) {
            return false;
         } else {
            this.api.getEntityBuilder().updateMemberCache(member, true);
            return true;
         }
      }
   }

   public int getMemberCount() {
      return this.memberCount;
   }

   @Nonnull
   public String getName() {
      return this.name;
   }

   public String getIconId() {
      return this.iconId;
   }

   @Nonnull
   public Set<String> getFeatures() {
      return this.features;
   }

   public String getSplashId() {
      return this.splashId;
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   public RestAction<String> retrieveVanityUrl() {
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_SERVER})) {
         throw new InsufficientPermissionException(this, Permission.MANAGE_SERVER);
      } else if (!this.getFeatures().contains("VANITY_URL")) {
         throw new IllegalStateException("This guild doesn't have a vanity url");
      } else {
         Route.CompiledRoute route = Route.Guilds.GET_VANITY_URL.compile(this.getId());
         return new RestActionImpl(this.getJDA(), route, (response, request) -> {
            return response.getObject().getString("code");
         });
      }
   }

   @Nullable
   public String getVanityCode() {
      return this.vanityCode;
   }

   @Nullable
   public String getDescription() {
      return this.description;
   }

   @Nonnull
   public Locale getLocale() {
      return this.preferredLocale;
   }

   @Nullable
   public String getBannerId() {
      return this.banner;
   }

   @Nonnull
   public Guild.BoostTier getBoostTier() {
      return this.boostTier;
   }

   public int getBoostCount() {
      return this.boostCount;
   }

   @Nonnull
   public List<Member> getBoosters() {
      return (List)this.memberCache.applyStream((members) -> {
         return (List)members.filter((m) -> {
            return m.getTimeBoosted() != null;
         }).sorted(Comparator.comparing(Member::getTimeBoosted)).collect(Collectors.toList());
      });
   }

   public int getMaxMembers() {
      return this.maxMembers;
   }

   public int getMaxPresences() {
      return this.maxPresences;
   }

   @Nonnull
   public RestAction<Guild.MetaData> retrieveMetaData() {
      Route.CompiledRoute route = Route.Guilds.GET_GUILD.compile(this.getId());
      route = route.withQueryParams("with_counts", "true");
      return new RestActionImpl(this.getJDA(), route, (response, request) -> {
         DataObject json = response.getObject();
         int memberLimit = json.getInt("max_members", 0);
         int presenceLimit = json.getInt("max_presences", 5000);
         this.maxMembers = memberLimit;
         this.maxPresences = presenceLimit;
         int approxMembers = json.getInt("approximate_member_count", this.memberCount);
         int approxPresence = json.getInt("approximate_presence_count", 0);
         return new Guild.MetaData(memberLimit, presenceLimit, approxPresence, approxMembers);
      });
   }

   public VoiceChannel getAfkChannel() {
      return this.afkChannel;
   }

   public TextChannel getSystemChannel() {
      return this.systemChannel;
   }

   @Nonnull
   public RestAction<List<Webhook>> retrieveWebhooks() {
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_WEBHOOKS})) {
         throw new InsufficientPermissionException(this, Permission.MANAGE_WEBHOOKS);
      } else {
         Route.CompiledRoute route = Route.Guilds.GET_WEBHOOKS.compile(this.getId());
         return new RestActionImpl(this.getJDA(), route, (response, request) -> {
            DataArray array = response.getArray();
            List<Webhook> webhooks = new ArrayList(array.length());
            EntityBuilder builder = this.api.getEntityBuilder();

            for(int i = 0; i < array.length(); ++i) {
               try {
                  webhooks.add(builder.createWebhook(array.getObject(i)));
               } catch (Exception var8) {
                  JDAImpl.LOG.error((String)"Error creating webhook from json", (Throwable)var8);
               }
            }

            return Collections.unmodifiableList(webhooks);
         });
      }
   }

   public Member getOwner() {
      return this.owner;
   }

   public long getOwnerIdLong() {
      return this.ownerId;
   }

   @Nonnull
   public Guild.Timeout getAfkTimeout() {
      return this.afkTimeout;
   }

   @Nonnull
   public String getRegionRaw() {
      return this.region;
   }

   public boolean isMember(@Nonnull User user) {
      return this.memberCache.get(user.getIdLong()) != null;
   }

   @Nonnull
   public Member getSelfMember() {
      Member member = this.getMember(this.getJDA().getSelfUser());
      if (member == null) {
         throw new IllegalStateException("Guild does not have a self member");
      } else {
         return member;
      }
   }

   public Member getMember(@Nonnull User user) {
      Checks.notNull(user, "User");
      return this.getMemberById(user.getIdLong());
   }

   @Nonnull
   public MemberCacheView getMemberCache() {
      return this.memberCache;
   }

   @Nonnull
   public SortedSnowflakeCacheView<Category> getCategoryCache() {
      return this.categoryCache;
   }

   @Nonnull
   public SortedSnowflakeCacheView<StoreChannel> getStoreChannelCache() {
      return this.storeChannelCache;
   }

   @Nonnull
   public SortedSnowflakeCacheView<TextChannel> getTextChannelCache() {
      return this.textChannelCache;
   }

   @Nonnull
   public SortedSnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
      return this.voiceChannelCache;
   }

   @Nonnull
   public SortedSnowflakeCacheView<Role> getRoleCache() {
      return this.roleCache;
   }

   @Nonnull
   public SnowflakeCacheView<Emote> getEmoteCache() {
      return this.emoteCache;
   }

   @Nonnull
   public List<GuildChannel> getChannels(boolean includeHidden) {
      Member self = this.getSelfMember();
      Predicate<GuildChannel> filterHidden = (it) -> {
         return self.hasPermission(it, new Permission[]{Permission.VIEW_CHANNEL});
      };
      SnowflakeCacheViewImpl<Category> categoryView = this.getCategoriesView();
      SnowflakeCacheViewImpl<VoiceChannel> voiceView = this.getVoiceChannelsView();
      SnowflakeCacheViewImpl<TextChannel> textView = this.getTextChannelsView();
      SnowflakeCacheViewImpl<StoreChannel> storeView = this.getStoreChannelView();
      UnlockHook categoryHook = categoryView.readLock();

      ArrayList channels;
      List textChannels;
      List storeChannels;
      List voiceChannels;
      List categories;
      try {
         UnlockHook voiceHook = voiceView.readLock();

         try {
            UnlockHook textHook = textView.readLock();

            try {
               UnlockHook storeHook = storeView.readLock();

               try {
                  if (includeHidden) {
                     storeChannels = storeView.asList();
                     textChannels = textView.asList();
                     voiceChannels = voiceView.asList();
                  } else {
                     storeChannels = (List)storeView.stream().filter(filterHidden).collect(Collectors.toList());
                     textChannels = (List)textView.stream().filter(filterHidden).collect(Collectors.toList());
                     voiceChannels = (List)voiceView.stream().filter(filterHidden).collect(Collectors.toList());
                  }

                  categories = categoryView.asList();
                  channels = new ArrayList((int)categoryView.size() + voiceChannels.size() + textChannels.size() + storeChannels.size());
               } catch (Throwable var23) {
                  if (storeHook != null) {
                     try {
                        storeHook.close();
                     } catch (Throwable var22) {
                        var23.addSuppressed(var22);
                     }
                  }

                  throw var23;
               }

               if (storeHook != null) {
                  storeHook.close();
               }
            } catch (Throwable var24) {
               if (textHook != null) {
                  try {
                     textHook.close();
                  } catch (Throwable var21) {
                     var24.addSuppressed(var21);
                  }
               }

               throw var24;
            }

            if (textHook != null) {
               textHook.close();
            }
         } catch (Throwable var25) {
            if (voiceHook != null) {
               try {
                  voiceHook.close();
               } catch (Throwable var20) {
                  var25.addSuppressed(var20);
               }
            }

            throw var25;
         }

         if (voiceHook != null) {
            voiceHook.close();
         }
      } catch (Throwable var26) {
         if (categoryHook != null) {
            try {
               categoryHook.close();
            } catch (Throwable var19) {
               var26.addSuppressed(var19);
            }
         }

         throw var26;
      }

      if (categoryHook != null) {
         categoryHook.close();
      }

      Stream var10000 = storeChannels.stream().filter((it) -> {
         return it.getParent() == null;
      });
      Objects.requireNonNull(channels);
      var10000.forEach(channels::add);
      var10000 = textChannels.stream().filter((it) -> {
         return it.getParent() == null;
      });
      Objects.requireNonNull(channels);
      var10000.forEach(channels::add);
      Collections.sort(channels);
      var10000 = voiceChannels.stream().filter((it) -> {
         return it.getParent() == null;
      });
      Objects.requireNonNull(channels);
      var10000.forEach(channels::add);
      Iterator var27 = categories.iterator();

      while(true) {
         Category category;
         List children;
         do {
            if (!var27.hasNext()) {
               return Collections.unmodifiableList(channels);
            }

            category = (Category)var27.next();
            if (includeHidden) {
               children = category.getChannels();
               break;
            }

            children = (List)category.getChannels().stream().filter(filterHidden).collect(Collectors.toList());
         } while(children.isEmpty());

         channels.add(category);
         channels.addAll(children);
      }
   }

   @Nonnull
   public RestAction<List<ListedEmote>> retrieveEmotes() {
      Route.CompiledRoute route = Route.Emotes.GET_EMOTES.compile(this.getId());
      return new RestActionImpl(this.getJDA(), route, (response, request) -> {
         EntityBuilder builder = this.getJDA().getEntityBuilder();
         DataArray emotes = response.getArray();
         List<ListedEmote> list = new ArrayList(emotes.length());

         for(int i = 0; i < emotes.length(); ++i) {
            DataObject emote = emotes.getObject(i);
            list.add(builder.createEmote(this, emote, true));
         }

         return Collections.unmodifiableList(list);
      });
   }

   @Nonnull
   public RestAction<ListedEmote> retrieveEmoteById(@Nonnull String id) {
      Checks.isSnowflake(id, "Emote ID");
      JDAImpl jda = this.getJDA();
      return new DeferredRestAction(jda, ListedEmote.class, () -> {
         Emote emote = this.getEmoteById(id);
         if (emote != null) {
            ListedEmote listedEmote = (ListedEmote)emote;
            if (listedEmote.hasUser() || !this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_EMOTES})) {
               return listedEmote;
            }
         }

         return null;
      }, () -> {
         Route.CompiledRoute route = Route.Emotes.GET_EMOTE.compile(this.getId(), id);
         return new AuditableRestActionImpl(jda, route, (response, request) -> {
            EntityBuilder builder = this.getJDA().getEntityBuilder();
            return builder.createEmote(this, response.getObject(), true);
         });
      });
   }

   @Nonnull
   public RestActionImpl<List<Guild.Ban>> retrieveBanList() {
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.BAN_MEMBERS})) {
         throw new InsufficientPermissionException(this, Permission.BAN_MEMBERS);
      } else {
         Route.CompiledRoute route = Route.Guilds.GET_BANS.compile(this.getId());
         return new RestActionImpl(this.getJDA(), route, (response, request) -> {
            EntityBuilder builder = this.api.getEntityBuilder();
            List<Guild.Ban> bans = new LinkedList();
            DataArray bannedArr = response.getArray();

            for(int i = 0; i < bannedArr.length(); ++i) {
               DataObject object = bannedArr.getObject(i);
               DataObject user = object.getObject("user");
               bans.add(new Guild.Ban(builder.createUser(user), object.getString("reason", (String)null)));
            }

            return Collections.unmodifiableList(bans);
         });
      }
   }

   @Nonnull
   public RestAction<Guild.Ban> retrieveBanById(@Nonnull String userId) {
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.BAN_MEMBERS})) {
         throw new InsufficientPermissionException(this, Permission.BAN_MEMBERS);
      } else {
         Checks.isSnowflake(userId, "User ID");
         Route.CompiledRoute route = Route.Guilds.GET_BAN.compile(this.getId(), userId);
         return new RestActionImpl(this.getJDA(), route, (response, request) -> {
            EntityBuilder builder = this.api.getEntityBuilder();
            DataObject bannedObj = response.getObject();
            DataObject user = bannedObj.getObject("user");
            return new Guild.Ban(builder.createUser(user), bannedObj.getString("reason", (String)null));
         });
      }
   }

   @Nonnull
   public RestAction<Integer> retrievePrunableMemberCount(int days) {
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.KICK_MEMBERS})) {
         throw new InsufficientPermissionException(this, Permission.KICK_MEMBERS);
      } else {
         Checks.check(days >= 1 && days <= 30, "Provided %d days must be between 1 and 30.", (Object)days);
         Route.CompiledRoute route = Route.Guilds.PRUNABLE_COUNT.compile(this.getId()).withQueryParams("days", Integer.toString(days));
         return new RestActionImpl(this.getJDA(), route, (response, request) -> {
            return response.getObject().getInt("pruned");
         });
      }
   }

   @Nonnull
   public Role getPublicRole() {
      return this.publicRole;
   }

   @Nullable
   public TextChannel getDefaultChannel() {
      Role role = this.getPublicRole();
      return (TextChannel)this.getTextChannelsView().stream().filter((c) -> {
         return role.hasPermission(c, new Permission[]{Permission.MESSAGE_READ});
      }).min(Comparator.naturalOrder()).orElse((Object)null);
   }

   @Nonnull
   public GuildManager getManager() {
      GuildManager mng = this.manager;
      if (mng == null) {
         mng = (GuildManager)MiscUtil.locked(this.mngLock, () -> {
            if (this.manager == null) {
               this.manager = new GuildManagerImpl(this);
            }

            return this.manager;
         });
      }

      return mng;
   }

   @Nonnull
   public AuditLogPaginationAction retrieveAuditLogs() {
      return new AuditLogPaginationActionImpl(this);
   }

   @Nonnull
   public RestAction<Void> leave() {
      if (this.getSelfMember().isOwner()) {
         throw new IllegalStateException("Cannot leave a guild that you are the owner of! Transfer guild ownership first!");
      } else {
         Route.CompiledRoute route = Route.Self.LEAVE_GUILD.compile(this.getId());
         return new RestActionImpl(this.getJDA(), route);
      }
   }

   @Nonnull
   public RestAction<Void> delete() {
      if (!this.getJDA().getSelfUser().isBot() && this.getJDA().getSelfUser().isMfaEnabled()) {
         throw new IllegalStateException("Cannot delete a guild without providing MFA code. Use Guild#delete(String)");
      } else {
         return this.delete((String)null);
      }
   }

   @Nonnull
   public RestAction<Void> delete(String mfaCode) {
      if (!this.getSelfMember().isOwner()) {
         throw new PermissionException("Cannot delete a guild that you do not own!");
      } else {
         DataObject mfaBody = null;
         if (!this.getJDA().getSelfUser().isBot() && this.getJDA().getSelfUser().isMfaEnabled()) {
            Checks.notEmpty((CharSequence)mfaCode, "Provided MultiFactor Auth code");
            mfaBody = DataObject.empty().put("code", mfaCode);
         }

         Route.CompiledRoute route = Route.Guilds.DELETE_GUILD.compile(this.getId());
         return new RestActionImpl(this.getJDA(), route, mfaBody);
      }
   }

   @Nonnull
   public AudioManager getAudioManager() {
      if (!this.getJDA().isIntent(GatewayIntent.GUILD_VOICE_STATES)) {
         throw new IllegalStateException("Cannot use audio features with disabled GUILD_VOICE_STATES intent!");
      } else {
         AbstractCacheView<AudioManager> managerMap = this.getJDA().getAudioManagersView();
         AudioManager mng = (AudioManager)managerMap.get(this.id);
         if (mng == null) {
            UnlockHook hook = managerMap.writeLock();

            try {
               GuildImpl cachedGuild = (GuildImpl)this.getJDA().getGuildById(this.id);
               if (cachedGuild == null) {
                  throw new IllegalStateException("Cannot get an AudioManager instance on an uncached Guild");
               }

               mng = (AudioManager)managerMap.get(this.id);
               if (mng == null) {
                  mng = new AudioManagerImpl(cachedGuild);
                  managerMap.getMap().put(this.id, mng);
               }
            } catch (Throwable var7) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (hook != null) {
               hook.close();
            }
         }

         return (AudioManager)mng;
      }
   }

   @Nonnull
   public JDAImpl getJDA() {
      return this.api;
   }

   @Nonnull
   public List<GuildVoiceState> getVoiceStates() {
      return Collections.unmodifiableList((List)this.getMembersView().stream().map(Member::getVoiceState).filter(Objects::nonNull).collect(Collectors.toList()));
   }

   @Nonnull
   public Guild.VerificationLevel getVerificationLevel() {
      return this.verificationLevel;
   }

   @Nonnull
   public Guild.NotificationLevel getDefaultNotificationLevel() {
      return this.defaultNotificationLevel;
   }

   @Nonnull
   public Guild.MFALevel getRequiredMFALevel() {
      return this.mfaLevel;
   }

   @Nonnull
   public Guild.ExplicitContentLevel getExplicitContentLevel() {
      return this.explicitContentLevel;
   }

   /** @deprecated */
   @Deprecated
   public boolean checkVerification() {
      if (this.getJDA().getAccountType() == AccountType.BOT) {
         return true;
      } else if (this.canSendVerification) {
         return true;
      } else {
         label34: {
            switch(this.verificationLevel) {
            case VERY_HIGH:
            default:
               break;
            case HIGH:
               if (ChronoUnit.MINUTES.between(this.getSelfMember().getTimeJoined(), OffsetDateTime.now()) < 10L) {
                  break;
               }
            case MEDIUM:
               if (ChronoUnit.MINUTES.between(this.getJDA().getSelfUser().getTimeCreated(), OffsetDateTime.now()) < 5L) {
                  break;
               }
            case LOW:
               if (this.getJDA().getSelfUser().isVerified()) {
                  break label34;
               }
               break;
            case NONE:
               break label34;
            case UNKNOWN:
               return true;
            }

            return false;
         }

         this.canSendVerification = true;
         return true;
      }
   }

   /** @deprecated */
   @Deprecated
   public boolean isAvailable() {
      return this.available;
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   public CompletableFuture<Void> retrieveMembers() {
      if (!this.getJDA().isIntent(GatewayIntent.GUILD_MEMBERS)) {
         CompletableFuture<Void> future = new CompletableFuture();
         future.completeExceptionally(new IllegalStateException("Unable to start member chunking on a guild with disabled GUILD_MEMBERS intent!"));
         return future;
      } else if (this.isLoaded()) {
         return CompletableFuture.completedFuture((Object)null);
      } else {
         Task<List<Member>> task = this.loadMembers();
         CompletableFuture<Void> future = new CompletableFuture();
         Objects.requireNonNull(future);
         task.onError(future::completeExceptionally);
         task.onSuccess((members) -> {
            UnlockHook hook = this.memberCache.writeLock();

            try {
               members.forEach((it) -> {
                  this.memberCache.getMap().put(it.getIdLong(), it);
               });
            } catch (Throwable var7) {
               if (hook != null) {
                  try {
                     hook.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (hook != null) {
               hook.close();
            }

            future.complete((Object)null);
         });
         return future;
      }
   }

   @Nonnull
   public Task<Void> loadMembers(@Nonnull Consumer<Member> callback) {
      Checks.notNull(callback, "Callback");
      if (!this.getJDA().isIntent(GatewayIntent.GUILD_MEMBERS)) {
         throw new IllegalStateException("Cannot use loadMembers without GatewayIntent.GUILD_MEMBERS!");
      } else if (this.isLoaded()) {
         this.memberCache.forEachUnordered(callback);
         return new GatewayTask(CompletableFuture.completedFuture((Object)null), () -> {
         });
      } else {
         MemberChunkManager chunkManager = this.getJDA().getClient().getChunkManager();
         boolean includePresences = this.getJDA().isIntent(GatewayIntent.GUILD_PRESENCES);
         CompletableFuture<Void> handler = chunkManager.chunkGuild(this, includePresences, (last, list) -> {
            list.forEach(callback);
         });
         return new GatewayTask(handler, () -> {
            handler.cancel(false);
         });
      }
   }

   private Member getMember(long id, boolean update, JDAImpl jda) {
      if (!update || jda.isIntent(GatewayIntent.GUILD_MEMBERS)) {
         Member member = this.getMemberById(id);
         if (!update || member != null && member.hasTimeJoined()) {
            return member;
         }
      }

      return null;
   }

   @Nonnull
   public RestAction<Member> retrieveMemberById(long id, boolean update) {
      JDAImpl jda = this.getJDA();
      return (RestAction)(id == jda.getSelfUser().getIdLong() ? new CompletedRestAction(jda, this.getSelfMember()) : new DeferredRestAction(jda, Member.class, () -> {
         return this.getMember(id, update, jda);
      }, () -> {
         Route.CompiledRoute route = Route.Guilds.GET_MEMBER.compile(this.getId(), Long.toUnsignedString(id));
         return new RestActionImpl(jda, route, (resp, req) -> {
            MemberImpl member = jda.getEntityBuilder().createMember(this, resp.getObject());
            jda.getEntityBuilder().updateMemberCache(member);
            return member;
         });
      }));
   }

   @Nonnull
   public Task<List<Member>> retrieveMembersByIds(boolean includePresence, @Nonnull long... ids) {
      Checks.notNull(ids, "ID Array");
      Checks.check(!includePresence || this.api.isIntent(GatewayIntent.GUILD_PRESENCES), "Cannot retrieve presences of members without GUILD_PRESENCES intent!");
      if (ids.length == 0) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         Checks.check(ids.length <= 100, "You can only request 100 members at once");
         MemberChunkManager chunkManager = this.api.getClient().getChunkManager();
         List<Member> collect = new ArrayList(ids.length);
         CompletableFuture<List<Member>> result = new CompletableFuture();
         CompletableFuture<Void> handle = chunkManager.chunkGuild(this, includePresence, ids, (last, list) -> {
            collect.addAll(list);
            if (last) {
               result.complete(collect);
            }

         });
         result.exceptionally((ex) -> {
            WebSocketClient.LOG.error("Encountered exception trying to handle member chunk response", ex);
            return null;
         });
         return new GatewayTask(result, () -> {
            handle.cancel(false);
         });
      }
   }

   @Nonnull
   @CheckReturnValue
   public Task<List<Member>> retrieveMembersByPrefix(@Nonnull String prefix, int limit) {
      Checks.notEmpty((CharSequence)prefix, "Prefix");
      Checks.positive(limit, "Limit");
      Checks.check(limit <= 100, "Limit must not be greater than 100");
      MemberChunkManager chunkManager = this.api.getClient().getChunkManager();
      List<Member> collect = new ArrayList(limit);
      CompletableFuture<List<Member>> result = new CompletableFuture();
      CompletableFuture<Void> handle = chunkManager.chunkGuild(this, prefix, limit, (last, list) -> {
         collect.addAll(list);
         if (last) {
            result.complete(collect);
         }

      });
      result.exceptionally((ex) -> {
         WebSocketClient.LOG.error("Encountered exception trying to handle member chunk response", ex);
         return null;
      });
      return new GatewayTask(result, () -> {
         handle.cancel(false);
      });
   }

   public long getIdLong() {
      return this.id;
   }

   @Nonnull
   public RestAction<List<Invite>> retrieveInvites() {
      if (!this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_SERVER})) {
         throw new InsufficientPermissionException(this, Permission.MANAGE_SERVER);
      } else {
         Route.CompiledRoute route = Route.Invites.GET_GUILD_INVITES.compile(this.getId());
         return new RestActionImpl(this.getJDA(), route, (response, request) -> {
            EntityBuilder entityBuilder = this.api.getEntityBuilder();
            DataArray array = response.getArray();
            List<Invite> invites = new ArrayList(array.length());

            for(int i = 0; i < array.length(); ++i) {
               invites.add(entityBuilder.createInvite(array.getObject(i)));
            }

            return Collections.unmodifiableList(invites);
         });
      }
   }

   @Nonnull
   public RestAction<Void> moveVoiceMember(@Nonnull Member member, @Nullable VoiceChannel voiceChannel) {
      Checks.notNull(member, "Member");
      this.checkGuild(member.getGuild(), "Member");
      if (voiceChannel != null) {
         this.checkGuild(voiceChannel.getGuild(), "VoiceChannel");
      }

      GuildVoiceState vState = member.getVoiceState();
      if (vState == null) {
         throw new IllegalStateException("Cannot move a Member with disabled CacheFlag.VOICE_STATE");
      } else {
         VoiceChannel channel = vState.getChannel();
         if (channel == null) {
            throw new IllegalStateException("You cannot move a Member who isn't in a VoiceChannel!");
         } else if (!PermissionUtil.checkPermission(channel, this.getSelfMember(), Permission.VOICE_MOVE_OTHERS)) {
            throw new InsufficientPermissionException(channel, Permission.VOICE_MOVE_OTHERS, "This account does not have Permission to MOVE_OTHERS out of the channel that the Member is currently in.");
         } else if (voiceChannel != null && !PermissionUtil.checkPermission(voiceChannel, this.getSelfMember(), Permission.VOICE_CONNECT) && !PermissionUtil.checkPermission(voiceChannel, member, Permission.VOICE_CONNECT)) {
            throw new InsufficientPermissionException(voiceChannel, Permission.VOICE_CONNECT, "Neither this account nor the Member that is attempting to be moved have the VOICE_CONNECT permission for the destination VoiceChannel, so the move cannot be done.");
         } else {
            DataObject body = DataObject.empty().put("channel_id", voiceChannel == null ? null : voiceChannel.getId());
            Route.CompiledRoute route = Route.Guilds.MODIFY_MEMBER.compile(this.getId(), member.getUser().getId());
            return new RestActionImpl(this.getJDA(), route, body);
         }
      }
   }

   @Nonnull
   public AuditableRestAction<Void> modifyNickname(@Nonnull Member member, String nickname) {
      Checks.notNull(member, "Member");
      this.checkGuild(member.getGuild(), "Member");
      if (member.equals(this.getSelfMember())) {
         if (!member.hasPermission(new Permission[]{Permission.NICKNAME_CHANGE}) && !member.hasPermission(new Permission[]{Permission.NICKNAME_MANAGE})) {
            throw new InsufficientPermissionException(this, Permission.NICKNAME_CHANGE, "You neither have NICKNAME_CHANGE nor NICKNAME_MANAGE permission!");
         }
      } else {
         this.checkPermission(Permission.NICKNAME_MANAGE);
         this.checkPosition(member);
      }

      JDAImpl jda = this.getJDA();
      return (new DeferredRestAction(jda, () -> {
         DataObject body = DataObject.empty().put("nick", nickname == null ? "" : nickname);
         Route.CompiledRoute route;
         if (member.equals(this.getSelfMember())) {
            route = Route.Guilds.MODIFY_SELF_NICK.compile(this.getId());
         } else {
            route = Route.Guilds.MODIFY_MEMBER.compile(this.getId(), member.getUser().getId());
         }

         return new AuditableRestActionImpl(jda, route, body);
      })).setCacheCheck(() -> {
         return !Objects.equals(nickname, member.getNickname());
      });
   }

   @Nonnull
   public AuditableRestAction<Integer> prune(int days, boolean wait, @Nonnull Role... roles) {
      this.checkPermission(Permission.KICK_MEMBERS);
      Checks.check(days >= 1 && days <= 30, "Provided %d days must be between 1 and 30.", (Object)days);
      Checks.notNull(roles, "Roles");
      Route.CompiledRoute route = Route.Guilds.PRUNE_MEMBERS.compile(this.getId());
      DataObject body = DataObject.empty();
      body.put("days", days);
      if (!wait) {
         body.put("compute_prune_count", false);
      }

      if (roles.length != 0) {
         Role[] var6 = roles;
         int var7 = roles.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Role role = var6[var8];
            Checks.notNull(role, "Role");
            Checks.check(role.getGuild().equals(this), "Role is not from the same guild!");
         }

         body.put("include_roles", Arrays.stream(roles).map(ISnowflake::getId).collect(Collectors.toList()));
      }

      return new AuditableRestActionImpl(this.getJDA(), route, body, (response, request) -> {
         return response.getObject().getInt("pruned", 0);
      });
   }

   @Nonnull
   public AuditableRestAction<Void> kick(@Nonnull Member member, String reason) {
      Checks.notNull(member, "member");
      this.checkGuild(member.getGuild(), "member");
      this.checkPermission(Permission.KICK_MEMBERS);
      this.checkPosition(member);
      return this.kick0(member.getUser().getId(), reason);
   }

   @Nonnull
   public AuditableRestAction<Void> kick(@Nonnull String userId, @Nullable String reason) {
      Member member = this.getMemberById(userId);
      if (member != null) {
         return this.kick(member, reason);
      } else {
         Checks.check(!userId.equals(this.getOwnerId()), "Cannot kick the owner of a guild!");
         this.checkPermission(Permission.KICK_MEMBERS);
         return this.kick0(userId, reason);
      }
   }

   @Nonnull
   private AuditableRestAction<Void> kick0(@Nonnull String userId, @Nullable String reason) {
      Route.CompiledRoute route = Route.Guilds.KICK_MEMBER.compile(this.getId(), userId);
      if (!Helpers.isBlank(reason)) {
         route = route.withQueryParams("reason", EncodingUtil.encodeUTF8(reason));
      }

      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public AuditableRestAction<Void> ban(@Nonnull User user, int delDays, String reason) {
      Checks.notNull(user, "User");
      this.checkPermission(Permission.BAN_MEMBERS);
      if (this.isMember(user)) {
         this.checkPosition(this.getMember(user));
      }

      return this.ban0(user.getId(), delDays, reason);
   }

   @Nonnull
   public AuditableRestAction<Void> ban(@Nonnull String userId, int delDays, String reason) {
      Checks.notNull(userId, "User");
      this.checkPermission(Permission.BAN_MEMBERS);
      User user = this.getJDA().getUserById(userId);
      return user != null ? this.ban(user, delDays, reason) : this.ban0(userId, delDays, reason);
   }

   @Nonnull
   private AuditableRestAction<Void> ban0(@Nonnull String userId, int delDays, String reason) {
      Checks.notNegative(delDays, "Deletion Days");
      Checks.check(delDays <= 7, "Deletion Days must not be bigger than 7.");
      Route.CompiledRoute route = Route.Guilds.BAN.compile(this.getId(), userId);
      if (!Helpers.isBlank(reason)) {
         route = route.withQueryParams("reason", EncodingUtil.encodeUTF8(reason));
      }

      if (delDays > 0) {
         route = route.withQueryParams("delete-message-days", Integer.toString(delDays));
      }

      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public AuditableRestAction<Void> unban(@Nonnull String userId) {
      Checks.isSnowflake(userId, "User ID");
      this.checkPermission(Permission.BAN_MEMBERS);
      Route.CompiledRoute route = Route.Guilds.UNBAN.compile(this.getId(), userId);
      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public AuditableRestAction<Void> deafen(@Nonnull Member member, boolean deafen) {
      Checks.notNull(member, "Member");
      this.checkGuild(member.getGuild(), "Member");
      this.checkPermission(Permission.VOICE_DEAF_OTHERS);
      GuildVoiceState voiceState = member.getVoiceState();
      if (voiceState != null) {
         if (voiceState.getChannel() == null) {
            throw new IllegalStateException("Can only deafen members who are currently in a voice channel");
         }

         if (voiceState.isGuildDeafened() == deafen) {
            return new CompletedRestAction(this.getJDA(), (Throwable)null);
         }
      }

      DataObject body = DataObject.empty().put("deaf", deafen);
      Route.CompiledRoute route = Route.Guilds.MODIFY_MEMBER.compile(this.getId(), member.getUser().getId());
      return new AuditableRestActionImpl(this.getJDA(), route, body);
   }

   @Nonnull
   public AuditableRestAction<Void> mute(@Nonnull Member member, boolean mute) {
      Checks.notNull(member, "Member");
      this.checkGuild(member.getGuild(), "Member");
      this.checkPermission(Permission.VOICE_MUTE_OTHERS);
      GuildVoiceState voiceState = member.getVoiceState();
      if (voiceState != null) {
         if (voiceState.getChannel() == null) {
            throw new IllegalStateException("Can only mute members who are currently in a voice channel");
         }

         if (voiceState.isGuildMuted() == mute) {
            return new CompletedRestAction(this.getJDA(), (Throwable)null);
         }
      }

      DataObject body = DataObject.empty().put("mute", mute);
      Route.CompiledRoute route = Route.Guilds.MODIFY_MEMBER.compile(this.getId(), member.getUser().getId());
      return new AuditableRestActionImpl(this.getJDA(), route, body);
   }

   @Nonnull
   public AuditableRestAction<Void> addRoleToMember(@Nonnull Member member, @Nonnull Role role) {
      Checks.notNull(member, "Member");
      Checks.notNull(role, "Role");
      this.checkGuild(member.getGuild(), "Member");
      this.checkGuild(role.getGuild(), "Role");
      this.checkPermission(Permission.MANAGE_ROLES);
      this.checkPosition(role);
      Route.CompiledRoute route = Route.Guilds.ADD_MEMBER_ROLE.compile(this.getId(), member.getUser().getId(), role.getId());
      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public AuditableRestAction<Void> removeRoleFromMember(@Nonnull Member member, @Nonnull Role role) {
      Checks.notNull(member, "Member");
      Checks.notNull(role, "Role");
      this.checkGuild(member.getGuild(), "Member");
      this.checkGuild(role.getGuild(), "Role");
      this.checkPermission(Permission.MANAGE_ROLES);
      this.checkPosition(role);
      Route.CompiledRoute route = Route.Guilds.REMOVE_MEMBER_ROLE.compile(this.getId(), member.getUser().getId(), role.getId());
      return new AuditableRestActionImpl(this.getJDA(), route);
   }

   @Nonnull
   public AuditableRestAction<Void> modifyMemberRoles(@Nonnull Member member, Collection<Role> rolesToAdd, Collection<Role> rolesToRemove) {
      Checks.notNull(member, "Member");
      this.checkGuild(member.getGuild(), "Member");
      this.checkPermission(Permission.MANAGE_ROLES);
      Set<Role> currentRoles = new HashSet(((MemberImpl)member).getRoleSet());
      if (rolesToAdd != null) {
         this.checkRoles(rolesToAdd, "add", "to");
         currentRoles.addAll(rolesToAdd);
      }

      if (rolesToRemove != null) {
         this.checkRoles(rolesToRemove, "remove", "from");
         currentRoles.removeAll(rolesToRemove);
      }

      return this.modifyMemberRoles(member, currentRoles);
   }

   @Nonnull
   public AuditableRestAction<Void> modifyMemberRoles(@Nonnull Member member, @Nonnull Collection<Role> roles) {
      Checks.notNull(member, "Member");
      Checks.notNull(roles, "Roles");
      this.checkGuild(member.getGuild(), "Member");
      roles.forEach((role) -> {
         Checks.notNull(role, "Role in collection");
         this.checkGuild(role.getGuild(), "Role: " + role.toString());
      });
      Checks.check(!roles.contains(this.getPublicRole()), "Cannot add the PublicRole of a Guild to a Member. All members have this role by default!");
      List<Role> memberRoles = member.getRoles();
      if (Helpers.deepEqualsUnordered(roles, memberRoles)) {
         return new CompletedRestAction(this.getJDA(), (Throwable)null);
      } else {
         Iterator var4 = memberRoles.iterator();

         Role r;
         while(var4.hasNext()) {
            r = (Role)var4.next();
            if (!roles.contains(r)) {
               this.checkPosition(r);
               Checks.check(!r.isManaged(), "Cannot remove managed role from member. Role: %s", (Object)r);
            }
         }

         var4 = roles.iterator();

         while(var4.hasNext()) {
            r = (Role)var4.next();
            if (!memberRoles.contains(r)) {
               this.checkPosition(r);
               Checks.check(!r.isManaged(), "Cannot add managed role to member. Role: %s", (Object)r);
            }
         }

         DataObject body = DataObject.empty().put("roles", roles.stream().map(ISnowflake::getId).collect(Collectors.toSet()));
         Route.CompiledRoute route = Route.Guilds.MODIFY_MEMBER.compile(this.getId(), member.getUser().getId());
         return new AuditableRestActionImpl(this.getJDA(), route, body);
      }
   }

   @Nonnull
   public AuditableRestAction<Void> transferOwnership(@Nonnull Member newOwner) {
      Checks.notNull(newOwner, "Member");
      this.checkGuild(newOwner.getGuild(), "Member");
      if (!this.getSelfMember().isOwner()) {
         throw new PermissionException("The logged in account must be the owner of this Guild to be able to transfer ownership");
      } else {
         Checks.check(!this.getSelfMember().equals(newOwner), "The member provided as the newOwner is the currently logged in account. Provide a different member to give ownership to.");
         Checks.check(!newOwner.getUser().isBot(), "Cannot transfer ownership of a Guild to a Bot!");
         DataObject body = DataObject.empty().put("owner_id", newOwner.getUser().getId());
         Route.CompiledRoute route = Route.Guilds.MODIFY_GUILD.compile(this.getId());
         return new AuditableRestActionImpl(this.getJDA(), route, body);
      }
   }

   @Nonnull
   public ChannelAction<TextChannel> createTextChannel(@Nonnull String name, Category parent) {
      if (parent != null) {
         Checks.check(parent.getGuild().equals(this), "Category is not from the same guild!");
         if (!this.getSelfMember().hasPermission(parent, new Permission[]{Permission.MANAGE_CHANNEL})) {
            throw new InsufficientPermissionException(parent, Permission.MANAGE_CHANNEL);
         }
      } else {
         this.checkPermission(Permission.MANAGE_CHANNEL);
      }

      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() > 0 && name.length() <= 100, "Provided name must be 1 - 100 characters in length");
      return (new ChannelActionImpl(TextChannel.class, name, this, ChannelType.TEXT)).setParent(parent);
   }

   @Nonnull
   public ChannelAction<VoiceChannel> createVoiceChannel(@Nonnull String name, Category parent) {
      if (parent != null) {
         Checks.check(parent.getGuild().equals(this), "Category is not from the same guild!");
         if (!this.getSelfMember().hasPermission(parent, new Permission[]{Permission.MANAGE_CHANNEL})) {
            throw new InsufficientPermissionException(parent, Permission.MANAGE_CHANNEL);
         }
      } else {
         this.checkPermission(Permission.MANAGE_CHANNEL);
      }

      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() > 0 && name.length() <= 100, "Provided name must be 1 - 100 characters in length");
      return (new ChannelActionImpl(VoiceChannel.class, name, this, ChannelType.VOICE)).setParent(parent);
   }

   @Nonnull
   public ChannelAction<Category> createCategory(@Nonnull String name) {
      this.checkPermission(Permission.MANAGE_CHANNEL);
      Checks.notBlank(name, "Name");
      name = name.trim();
      Checks.check(name.length() > 0 && name.length() <= 100, "Provided name must be 1 - 100 characters in length");
      return new ChannelActionImpl(Category.class, name, this, ChannelType.CATEGORY);
   }

   @Nonnull
   public RoleAction createRole() {
      this.checkPermission(Permission.MANAGE_ROLES);
      return new RoleActionImpl(this);
   }

   @Nonnull
   public AuditableRestAction<Emote> createEmote(@Nonnull String name, @Nonnull Icon icon, @Nonnull Role... roles) {
      this.checkPermission(Permission.MANAGE_EMOTES);
      Checks.notBlank(name, "Emote name");
      Checks.notNull(icon, "Emote icon");
      Checks.notNull(roles, "Roles");
      DataObject body = DataObject.empty();
      body.put("name", name);
      body.put("image", icon.getEncoding());
      if (roles.length > 0) {
         body.put("roles", Stream.of(roles).filter(Objects::nonNull).map(ISnowflake::getId).collect(Collectors.toSet()));
      }

      JDAImpl jda = this.getJDA();
      Route.CompiledRoute route = Route.Emotes.CREATE_EMOTE.compile(this.getId());
      return new AuditableRestActionImpl(jda, route, body, (response, request) -> {
         DataObject obj = response.getObject();
         return jda.getEntityBuilder().createEmote(this, obj, true);
      });
   }

   @Nonnull
   public ChannelOrderAction modifyCategoryPositions() {
      return new ChannelOrderActionImpl(this, ChannelType.CATEGORY.getSortBucket());
   }

   @Nonnull
   public ChannelOrderAction modifyTextChannelPositions() {
      return new ChannelOrderActionImpl(this, ChannelType.TEXT.getSortBucket());
   }

   @Nonnull
   public ChannelOrderAction modifyVoiceChannelPositions() {
      return new ChannelOrderActionImpl(this, ChannelType.VOICE.getSortBucket());
   }

   @Nonnull
   public CategoryOrderAction modifyTextChannelPositions(@Nonnull Category category) {
      Checks.notNull(category, "Category");
      this.checkGuild(category.getGuild(), "Category");
      return new CategoryOrderActionImpl(category, ChannelType.TEXT.getSortBucket());
   }

   @Nonnull
   public CategoryOrderAction modifyVoiceChannelPositions(@Nonnull Category category) {
      Checks.notNull(category, "Category");
      this.checkGuild(category.getGuild(), "Category");
      return new CategoryOrderActionImpl(category, ChannelType.VOICE.getSortBucket());
   }

   @Nonnull
   public RoleOrderAction modifyRolePositions(boolean useAscendingOrder) {
      return new RoleOrderActionImpl(this, useAscendingOrder);
   }

   protected void checkGuild(Guild providedGuild, String comment) {
      if (!this.equals(providedGuild)) {
         throw new IllegalArgumentException("Provided " + comment + " is not part of this Guild!");
      }
   }

   protected void checkPermission(Permission perm) {
      if (!this.getSelfMember().hasPermission(new Permission[]{perm})) {
         throw new InsufficientPermissionException(this, perm);
      }
   }

   protected void checkPosition(Member member) {
      if (!this.getSelfMember().canInteract(member)) {
         throw new HierarchyException("Can't modify a member with higher or equal highest role than yourself!");
      }
   }

   protected void checkPosition(Role role) {
      if (!this.getSelfMember().canInteract(role)) {
         throw new HierarchyException("Can't modify a role with higher or equal highest role than yourself! Role: " + role.toString());
      }
   }

   private void checkRoles(Collection<Role> roles, String type, String preposition) {
      roles.forEach((role) -> {
         Checks.notNull(role, "Role in roles to " + type);
         this.checkGuild(role.getGuild(), "Role: " + role.toString());
         this.checkPosition(role);
         Checks.check(!role.isManaged(), "Cannot %s a managed role %s a Member. Role: %s", type, preposition, role.toString());
      });
   }

   public GuildImpl setAvailable(boolean available) {
      this.available = available;
      return this;
   }

   public GuildImpl setOwner(Member owner) {
      if (owner != null && this.getMemberById(owner.getIdLong()) != null) {
         this.owner = owner;
      }

      return this;
   }

   public GuildImpl setName(String name) {
      this.name = name;
      return this;
   }

   public GuildImpl setIconId(String iconId) {
      this.iconId = iconId;
      return this;
   }

   public GuildImpl setFeatures(Set<String> features) {
      this.features = Collections.unmodifiableSet(features);
      return this;
   }

   public GuildImpl setSplashId(String splashId) {
      this.splashId = splashId;
      return this;
   }

   public GuildImpl setRegion(String region) {
      this.region = region;
      return this;
   }

   public GuildImpl setVanityCode(String code) {
      this.vanityCode = code;
      return this;
   }

   public GuildImpl setDescription(String description) {
      this.description = description;
      return this;
   }

   public GuildImpl setBannerId(String bannerId) {
      this.banner = bannerId;
      return this;
   }

   public GuildImpl setMaxPresences(int maxPresences) {
      this.maxPresences = maxPresences;
      return this;
   }

   public GuildImpl setMaxMembers(int maxMembers) {
      this.maxMembers = maxMembers;
      return this;
   }

   public GuildImpl setAfkChannel(VoiceChannel afkChannel) {
      this.afkChannel = afkChannel;
      return this;
   }

   public GuildImpl setSystemChannel(TextChannel systemChannel) {
      this.systemChannel = systemChannel;
      return this;
   }

   public GuildImpl setPublicRole(Role publicRole) {
      this.publicRole = publicRole;
      return this;
   }

   public GuildImpl setVerificationLevel(Guild.VerificationLevel level) {
      this.verificationLevel = level;
      this.canSendVerification = false;
      return this;
   }

   public GuildImpl setDefaultNotificationLevel(Guild.NotificationLevel level) {
      this.defaultNotificationLevel = level;
      return this;
   }

   public GuildImpl setRequiredMFALevel(Guild.MFALevel level) {
      this.mfaLevel = level;
      return this;
   }

   public GuildImpl setExplicitContentLevel(Guild.ExplicitContentLevel level) {
      this.explicitContentLevel = level;
      return this;
   }

   public GuildImpl setAfkTimeout(Guild.Timeout afkTimeout) {
      this.afkTimeout = afkTimeout;
      return this;
   }

   public GuildImpl setLocale(String locale) {
      this.preferredLocale = Locale.forLanguageTag(locale);
      return this;
   }

   public GuildImpl setBoostTier(int tier) {
      this.boostTier = Guild.BoostTier.fromKey(tier);
      return this;
   }

   public GuildImpl setBoostCount(int count) {
      this.boostCount = count;
      return this;
   }

   public GuildImpl setOwnerId(long ownerId) {
      this.ownerId = ownerId;
      return this;
   }

   public GuildImpl setMemberCount(int count) {
      this.memberCount = count;
      return this;
   }

   public SortedSnowflakeCacheViewImpl<Category> getCategoriesView() {
      return this.categoryCache;
   }

   public SortedSnowflakeCacheViewImpl<StoreChannel> getStoreChannelView() {
      return this.storeChannelCache;
   }

   public SortedSnowflakeCacheViewImpl<TextChannel> getTextChannelsView() {
      return this.textChannelCache;
   }

   public SortedSnowflakeCacheViewImpl<VoiceChannel> getVoiceChannelsView() {
      return this.voiceChannelCache;
   }

   public SortedSnowflakeCacheViewImpl<Role> getRolesView() {
      return this.roleCache;
   }

   public SnowflakeCacheViewImpl<Emote> getEmotesView() {
      return this.emoteCache;
   }

   public MemberCacheViewImpl getMembersView() {
      return this.memberCache;
   }

   public void onMemberAdd() {
      ++this.memberCount;
   }

   public void onMemberRemove() {
      --this.memberCount;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof GuildImpl)) {
         return false;
      } else {
         GuildImpl oGuild = (GuildImpl)o;
         return this.id == oGuild.id;
      }
   }

   public int hashCode() {
      return Long.hashCode(this.id);
   }

   public String toString() {
      return "G:" + this.getName() + '(' + this.id + ')';
   }
}

package net.dv8tion.jda.api.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
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
import net.dv8tion.jda.internal.requests.DeferredRestAction;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.restaction.AuditableRestActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.concurrent.task.GatewayTask;

public interface Guild extends ISnowflake {
   String ICON_URL = "https://cdn.discordapp.com/icons/%s/%s.%s";
   String SPLASH_URL = "https://cdn.discordapp.com/splashes/%s/%s.png";
   String BANNER_URL = "https://cdn.discordapp.com/banners/%s/%s.png";

   @Nonnull
   @CheckReturnValue
   default RestAction<EnumSet<Region>> retrieveRegions() {
      return this.retrieveRegions(true);
   }

   @Nonnull
   @CheckReturnValue
   RestAction<EnumSet<Region>> retrieveRegions(boolean var1);

   @Nonnull
   @CheckReturnValue
   MemberAction addMember(@Nonnull String var1, @Nonnull String var2);

   @Nonnull
   @CheckReturnValue
   default MemberAction addMember(@Nonnull String accessToken, @Nonnull User user) {
      Checks.notNull(user, "User");
      return this.addMember(accessToken, user.getId());
   }

   @Nonnull
   @CheckReturnValue
   default MemberAction addMember(@Nonnull String accessToken, long userId) {
      return this.addMember(accessToken, Long.toUnsignedString(userId));
   }

   boolean isLoaded();

   void pruneMemberCache();

   boolean unloadMember(long var1);

   int getMemberCount();

   @Nonnull
   String getName();

   @Nullable
   String getIconId();

   @Nullable
   default String getIconUrl() {
      String iconId = this.getIconId();
      return iconId == null ? null : String.format("https://cdn.discordapp.com/icons/%s/%s.%s", this.getId(), iconId, iconId.startsWith("a_") ? "gif" : "png");
   }

   @Nonnull
   Set<String> getFeatures();

   @Nullable
   String getSplashId();

   @Nullable
   default String getSplashUrl() {
      String splashId = this.getSplashId();
      return splashId == null ? null : String.format("https://cdn.discordapp.com/splashes/%s/%s.png", this.getId(), splashId);
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.0.0")
   @ReplaceWith("getVanityCode()")
   @CheckReturnValue
   RestAction<String> retrieveVanityUrl();

   @Nullable
   String getVanityCode();

   @Nullable
   default String getVanityUrl() {
      return this.getVanityCode() == null ? null : "https://discord.gg/" + this.getVanityCode();
   }

   @Nullable
   String getDescription();

   @Nonnull
   Locale getLocale();

   @Nullable
   String getBannerId();

   @Nullable
   default String getBannerUrl() {
      String bannerId = this.getBannerId();
      return bannerId == null ? null : String.format("https://cdn.discordapp.com/banners/%s/%s.png", this.getId(), bannerId);
   }

   @Nonnull
   Guild.BoostTier getBoostTier();

   int getBoostCount();

   @Nonnull
   List<Member> getBoosters();

   default int getMaxBitrate() {
      int maxBitrate = this.getFeatures().contains("VIP_REGIONS") ? 384000 : 96000;
      return Math.max(maxBitrate, this.getBoostTier().getMaxBitrate());
   }

   default long getMaxFileSize() {
      return this.getBoostTier().getMaxFileSize();
   }

   default int getMaxEmotes() {
      int maxEmotes = this.getFeatures().contains("MORE_EMOJI") ? 200 : 50;
      return Math.max(maxEmotes, this.getBoostTier().getMaxEmotes());
   }

   int getMaxMembers();

   int getMaxPresences();

   @Nonnull
   @CheckReturnValue
   RestAction<Guild.MetaData> retrieveMetaData();

   @Nullable
   VoiceChannel getAfkChannel();

   @Nullable
   TextChannel getSystemChannel();

   @Nullable
   Member getOwner();

   long getOwnerIdLong();

   @Nonnull
   default String getOwnerId() {
      return Long.toUnsignedString(this.getOwnerIdLong());
   }

   @Nonnull
   Guild.Timeout getAfkTimeout();

   @Nonnull
   default Region getRegion() {
      return Region.fromKey(this.getRegionRaw());
   }

   @Nonnull
   String getRegionRaw();

   boolean isMember(@Nonnull User var1);

   @Nonnull
   Member getSelfMember();

   @Nullable
   Member getMember(@Nonnull User var1);

   @Nullable
   default Member getMemberById(@Nonnull String userId) {
      return this.getMemberCache().getElementById(userId);
   }

   @Nullable
   default Member getMemberById(long userId) {
      return this.getMemberCache().getElementById(userId);
   }

   @Nullable
   default Member getMemberByTag(@Nonnull String tag) {
      User user = this.getJDA().getUserByTag(tag);
      return user == null ? null : this.getMember(user);
   }

   @Nullable
   default Member getMemberByTag(@Nonnull String username, @Nonnull String discriminator) {
      User user = this.getJDA().getUserByTag(username, discriminator);
      return user == null ? null : this.getMember(user);
   }

   @Nonnull
   default List<Member> getMembers() {
      return this.getMemberCache().asList();
   }

   @Nonnull
   default List<Member> getMembersByName(@Nonnull String name, boolean ignoreCase) {
      return this.getMemberCache().getElementsByUsername(name, ignoreCase);
   }

   @Nonnull
   default List<Member> getMembersByNickname(@Nullable String nickname, boolean ignoreCase) {
      return this.getMemberCache().getElementsByNickname(nickname, ignoreCase);
   }

   @Nonnull
   default List<Member> getMembersByEffectiveName(@Nonnull String name, boolean ignoreCase) {
      return this.getMemberCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   default List<Member> getMembersWithRoles(@Nonnull Role... roles) {
      return this.getMemberCache().getElementsWithRoles(roles);
   }

   @Nonnull
   default List<Member> getMembersWithRoles(@Nonnull Collection<Role> roles) {
      return this.getMemberCache().getElementsWithRoles(roles);
   }

   @Nonnull
   MemberCacheView getMemberCache();

   @Nullable
   default GuildChannel getGuildChannelById(@Nonnull String id) {
      return this.getGuildChannelById(MiscUtil.parseSnowflake(id));
   }

   @Nullable
   default GuildChannel getGuildChannelById(long id) {
      GuildChannel channel = this.getTextChannelById(id);
      if (channel == null) {
         channel = this.getVoiceChannelById(id);
      }

      if (channel == null) {
         channel = this.getStoreChannelById(id);
      }

      if (channel == null) {
         channel = this.getCategoryById(id);
      }

      return (GuildChannel)channel;
   }

   @Nullable
   default GuildChannel getGuildChannelById(@Nonnull ChannelType type, @Nonnull String id) {
      return this.getGuildChannelById(type, MiscUtil.parseSnowflake(id));
   }

   @Nullable
   default GuildChannel getGuildChannelById(@Nonnull ChannelType type, long id) {
      Checks.notNull(type, "ChannelType");
      switch(type) {
      case TEXT:
         return this.getTextChannelById(id);
      case VOICE:
         return this.getVoiceChannelById(id);
      case STORE:
         return this.getStoreChannelById(id);
      case CATEGORY:
         return this.getCategoryById(id);
      default:
         return null;
      }
   }

   @Nullable
   default Category getCategoryById(@Nonnull String id) {
      return (Category)this.getCategoryCache().getElementById(id);
   }

   @Nullable
   default Category getCategoryById(long id) {
      return (Category)this.getCategoryCache().getElementById(id);
   }

   @Nonnull
   default List<Category> getCategories() {
      return this.getCategoryCache().asList();
   }

   @Nonnull
   default List<Category> getCategoriesByName(@Nonnull String name, boolean ignoreCase) {
      return this.getCategoryCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SortedSnowflakeCacheView<Category> getCategoryCache();

   @Nullable
   default StoreChannel getStoreChannelById(@Nonnull String id) {
      return (StoreChannel)this.getStoreChannelCache().getElementById(id);
   }

   @Nullable
   default StoreChannel getStoreChannelById(long id) {
      return (StoreChannel)this.getStoreChannelCache().getElementById(id);
   }

   @Nonnull
   default List<StoreChannel> getStoreChannels() {
      return this.getStoreChannelCache().asList();
   }

   @Nonnull
   default List<StoreChannel> getStoreChannelsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getStoreChannelCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SortedSnowflakeCacheView<StoreChannel> getStoreChannelCache();

   @Nullable
   default TextChannel getTextChannelById(@Nonnull String id) {
      return (TextChannel)this.getTextChannelCache().getElementById(id);
   }

   @Nullable
   default TextChannel getTextChannelById(long id) {
      return (TextChannel)this.getTextChannelCache().getElementById(id);
   }

   @Nonnull
   default List<TextChannel> getTextChannels() {
      return this.getTextChannelCache().asList();
   }

   @Nonnull
   default List<TextChannel> getTextChannelsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getTextChannelCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SortedSnowflakeCacheView<TextChannel> getTextChannelCache();

   @Nullable
   default VoiceChannel getVoiceChannelById(@Nonnull String id) {
      return (VoiceChannel)this.getVoiceChannelCache().getElementById(id);
   }

   @Nullable
   default VoiceChannel getVoiceChannelById(long id) {
      return (VoiceChannel)this.getVoiceChannelCache().getElementById(id);
   }

   @Nonnull
   default List<VoiceChannel> getVoiceChannels() {
      return this.getVoiceChannelCache().asList();
   }

   @Nonnull
   default List<VoiceChannel> getVoiceChannelsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getVoiceChannelCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SortedSnowflakeCacheView<VoiceChannel> getVoiceChannelCache();

   @Nonnull
   default List<GuildChannel> getChannels() {
      return this.getChannels(true);
   }

   @Nonnull
   List<GuildChannel> getChannels(boolean var1);

   @Nullable
   default Role getRoleById(@Nonnull String id) {
      return (Role)this.getRoleCache().getElementById(id);
   }

   @Nullable
   default Role getRoleById(long id) {
      return (Role)this.getRoleCache().getElementById(id);
   }

   @Nonnull
   default List<Role> getRoles() {
      return this.getRoleCache().asList();
   }

   @Nonnull
   default List<Role> getRolesByName(@Nonnull String name, boolean ignoreCase) {
      return this.getRoleCache().getElementsByName(name, ignoreCase);
   }

   @Nullable
   default Role getRoleByBot(long userId) {
      return (Role)this.getRoleCache().applyStream((stream) -> {
         return (Role)stream.filter((role) -> {
            return role.getTags().getBotIdLong() == userId;
         }).findFirst().orElse((Object)null);
      });
   }

   @Nullable
   default Role getRoleByBot(@Nonnull String userId) {
      return this.getRoleByBot(MiscUtil.parseSnowflake(userId));
   }

   @Nullable
   default Role getRoleByBot(@Nonnull User user) {
      Checks.notNull(user, "User");
      return this.getRoleByBot(user.getIdLong());
   }

   @Nullable
   default Role getBotRole() {
      return this.getRoleByBot((User)this.getJDA().getSelfUser());
   }

   @Nullable
   default Role getBoostRole() {
      return (Role)this.getRoleCache().applyStream((stream) -> {
         return (Role)stream.filter((role) -> {
            return role.getTags().isBoost();
         }).findFirst().orElse((Object)null);
      });
   }

   @Nonnull
   SortedSnowflakeCacheView<Role> getRoleCache();

   @Nullable
   default Emote getEmoteById(@Nonnull String id) {
      return (Emote)this.getEmoteCache().getElementById(id);
   }

   @Nullable
   default Emote getEmoteById(long id) {
      return (Emote)this.getEmoteCache().getElementById(id);
   }

   @Nonnull
   default List<Emote> getEmotes() {
      return this.getEmoteCache().asList();
   }

   @Nonnull
   default List<Emote> getEmotesByName(@Nonnull String name, boolean ignoreCase) {
      return this.getEmoteCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SnowflakeCacheView<Emote> getEmoteCache();

   @Nonnull
   @CheckReturnValue
   RestAction<List<ListedEmote>> retrieveEmotes();

   @Nonnull
   @CheckReturnValue
   RestAction<ListedEmote> retrieveEmoteById(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   default RestAction<ListedEmote> retrieveEmoteById(long id) {
      return this.retrieveEmoteById(Long.toUnsignedString(id));
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<ListedEmote> retrieveEmote(@Nonnull Emote emote) {
      Checks.notNull(emote, "Emote");
      if (emote.getGuild() != null) {
         Checks.check(emote.getGuild().equals(this), "Emote must be from the same Guild!");
      }

      JDA jda = this.getJDA();
      return new DeferredRestAction(jda, ListedEmote.class, () -> {
         if (emote instanceof ListedEmote) {
            ListedEmote listedEmote = (ListedEmote)emote;
            if (listedEmote.hasUser() || !this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_EMOTES})) {
               return listedEmote;
            }
         }

         return null;
      }, () -> {
         return this.retrieveEmoteById(emote.getId());
      });
   }

   @Nonnull
   @CheckReturnValue
   RestAction<List<Guild.Ban>> retrieveBanList();

   @Nonnull
   @CheckReturnValue
   default RestAction<Guild.Ban> retrieveBanById(long userId) {
      return this.retrieveBanById(Long.toUnsignedString(userId));
   }

   @Nonnull
   @CheckReturnValue
   RestAction<Guild.Ban> retrieveBanById(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   default RestAction<Guild.Ban> retrieveBan(@Nonnull User bannedUser) {
      Checks.notNull(bannedUser, "bannedUser");
      return this.retrieveBanById(bannedUser.getId());
   }

   @Nonnull
   @CheckReturnValue
   RestAction<Integer> retrievePrunableMemberCount(int var1);

   @Nonnull
   Role getPublicRole();

   @Nullable
   TextChannel getDefaultChannel();

   @Nonnull
   GuildManager getManager();

   @Nonnull
   @CheckReturnValue
   AuditLogPaginationAction retrieveAuditLogs();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> leave();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> delete();

   @Nonnull
   @CheckReturnValue
   RestAction<Void> delete(@Nullable String var1);

   @Nonnull
   AudioManager getAudioManager();

   @Nonnull
   JDA getJDA();

   @Nonnull
   @CheckReturnValue
   RestAction<List<Invite>> retrieveInvites();

   @Nonnull
   @CheckReturnValue
   RestAction<List<Webhook>> retrieveWebhooks();

   @Nonnull
   List<GuildVoiceState> getVoiceStates();

   @Nonnull
   Guild.VerificationLevel getVerificationLevel();

   @Nonnull
   Guild.NotificationLevel getDefaultNotificationLevel();

   @Nonnull
   Guild.MFALevel getRequiredMFALevel();

   @Nonnull
   Guild.ExplicitContentLevel getExplicitContentLevel();

   /** @deprecated */
   @Deprecated
   @ForRemoval
   @DeprecatedSince("4.2.0")
   boolean checkVerification();

   /** @deprecated */
   @ForRemoval
   @Deprecated
   @DeprecatedSince("4.1.0")
   @ReplaceWith("getJDA().isUnavailable(guild.getIdLong())")
   boolean isAvailable();

   /** @deprecated */
   @Nonnull
   @Deprecated
   @DeprecatedSince("4.2.0")
   @ReplaceWith("loadMembers(Consumer<Member>) or loadMembers()")
   CompletableFuture<Void> retrieveMembers();

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> loadMembers() {
      return this.findMembers((m) -> {
         return true;
      });
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> findMembers(@Nonnull Predicate<? super Member> filter) {
      Checks.notNull(filter, "Filter");
      List<Member> list = new ArrayList();
      CompletableFuture<List<Member>> future = new CompletableFuture();
      Task<Void> reference = this.loadMembers((member) -> {
         if (filter.test(member)) {
            list.add(member);
         }

      });
      Objects.requireNonNull(reference);
      GatewayTask<List<Member>> task = new GatewayTask(future, reference::cancel);
      Task var10000 = reference.onSuccess((it) -> {
         future.complete(list);
      });
      Objects.requireNonNull(future);
      var10000.onError(future::completeExceptionally);
      return task;
   }

   @Nonnull
   Task<Void> loadMembers(@Nonnull Consumer<Member> var1);

   @Nonnull
   default RestAction<Member> retrieveMember(@Nonnull User user) {
      Checks.notNull(user, "User");
      return this.retrieveMemberById(user.getId());
   }

   @Nonnull
   default RestAction<Member> retrieveMemberById(@Nonnull String id) {
      return this.retrieveMemberById(MiscUtil.parseSnowflake(id));
   }

   @Nonnull
   default RestAction<Member> retrieveMemberById(long id) {
      return this.retrieveMemberById(id, true);
   }

   @Nonnull
   default RestAction<Member> retrieveOwner() {
      return this.retrieveMemberById(this.getOwnerIdLong());
   }

   @Nonnull
   default RestAction<Member> retrieveMember(@Nonnull User user, boolean update) {
      Checks.notNull(user, "User");
      return this.retrieveMemberById(user.getId(), update);
   }

   @Nonnull
   default RestAction<Member> retrieveMemberById(@Nonnull String id, boolean update) {
      return this.retrieveMemberById(MiscUtil.parseSnowflake(id), update);
   }

   @Nonnull
   RestAction<Member> retrieveMemberById(long var1, boolean var3);

   @Nonnull
   default RestAction<Member> retrieveOwner(boolean update) {
      return this.retrieveMemberById(this.getOwnerIdLong(), update);
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembers(@Nonnull Collection<User> users) {
      Checks.noneNull(users, "Users");
      if (users.isEmpty()) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         long[] ids = users.stream().mapToLong(ISnowflake::getIdLong).toArray();
         return this.retrieveMembersByIds(ids);
      }
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembersByIds(@Nonnull Collection<Long> ids) {
      Checks.noneNull(ids, "IDs");
      if (ids.isEmpty()) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         long[] arr = ids.stream().mapToLong(Long::longValue).toArray();
         return this.retrieveMembersByIds(arr);
      }
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembersByIds(@Nonnull String... ids) {
      Checks.notNull(ids, "Array");
      if (ids.length == 0) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         long[] arr = new long[ids.length];

         for(int i = 0; i < ids.length; ++i) {
            arr[i] = MiscUtil.parseSnowflake(ids[i]);
         }

         return this.retrieveMembersByIds(arr);
      }
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembersByIds(@Nonnull long... ids) {
      boolean presence = this.getJDA().getGatewayIntents().contains(GatewayIntent.GUILD_PRESENCES);
      return this.retrieveMembersByIds(presence, ids);
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembers(boolean includePresence, @Nonnull Collection<User> users) {
      Checks.noneNull(users, "Users");
      if (users.isEmpty()) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         long[] ids = users.stream().mapToLong(ISnowflake::getIdLong).toArray();
         return this.retrieveMembersByIds(includePresence, ids);
      }
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembersByIds(boolean includePresence, @Nonnull Collection<Long> ids) {
      Checks.noneNull(ids, "IDs");
      if (ids.isEmpty()) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         long[] arr = ids.stream().mapToLong(Long::longValue).toArray();
         return this.retrieveMembersByIds(includePresence, arr);
      }
   }

   @Nonnull
   @CheckReturnValue
   default Task<List<Member>> retrieveMembersByIds(boolean includePresence, @Nonnull String... ids) {
      Checks.notNull(ids, "Array");
      if (ids.length == 0) {
         return new GatewayTask(CompletableFuture.completedFuture(Collections.emptyList()), () -> {
         });
      } else {
         long[] arr = new long[ids.length];

         for(int i = 0; i < ids.length; ++i) {
            arr[i] = MiscUtil.parseSnowflake(ids[i]);
         }

         return this.retrieveMembersByIds(includePresence, arr);
      }
   }

   @Nonnull
   @CheckReturnValue
   Task<List<Member>> retrieveMembersByIds(boolean var1, @Nonnull long... var2);

   @Nonnull
   @CheckReturnValue
   Task<List<Member>> retrieveMembersByPrefix(@Nonnull String var1, int var2);

   @Nonnull
   @CheckReturnValue
   RestAction<Void> moveVoiceMember(@Nonnull Member var1, @Nullable VoiceChannel var2);

   @Nonnull
   @CheckReturnValue
   default RestAction<Void> kickVoiceMember(@Nonnull Member member) {
      return this.moveVoiceMember(member, (VoiceChannel)null);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> modifyNickname(@Nonnull Member var1, @Nullable String var2);

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Integer> prune(int days, @Nonnull Role... roles) {
      return this.prune(days, true, roles);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Integer> prune(int var1, boolean var2, @Nonnull Role... var3);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> kick(@Nonnull Member var1, @Nullable String var2);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> kick(@Nonnull String var1, @Nullable String var2);

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> kick(@Nonnull Member member) {
      return this.kick((Member)member, (String)null);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> kick(@Nonnull String userId) {
      return this.kick((String)userId, (String)null);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> ban(@Nonnull User var1, int var2, @Nullable String var3);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> ban(@Nonnull String var1, int var2, @Nullable String var3);

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> ban(@Nonnull Member member, int delDays, @Nullable String reason) {
      Checks.notNull(member, "Member");
      return this.ban(member.getUser(), delDays, reason);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> ban(@Nonnull Member member, int delDays) {
      return this.ban((Member)member, delDays, (String)null);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> ban(@Nonnull User user, int delDays) {
      return this.ban((User)user, delDays, (String)null);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> ban(@Nonnull String userId, int delDays) {
      return this.ban((String)userId, delDays, (String)null);
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> unban(@Nonnull User user) {
      Checks.notNull(user, "User");
      return this.unban(user.getId());
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> unban(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> deafen(@Nonnull Member var1, boolean var2);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> mute(@Nonnull Member var1, boolean var2);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> addRoleToMember(@Nonnull Member var1, @Nonnull Role var2);

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> addRoleToMember(long userId, @Nonnull Role role) {
      Checks.notNull(role, "Role");
      Checks.check(role.getGuild().equals(this), "Role must be from the same guild! Trying to use role from %s in %s", role.getGuild().toString(), this.toString());
      Member member = this.getMemberById(userId);
      if (member != null) {
         return this.addRoleToMember(member, role);
      } else if (!this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_ROLES})) {
         throw new InsufficientPermissionException(this, Permission.MANAGE_ROLES);
      } else if (!this.getSelfMember().canInteract(role)) {
         throw new HierarchyException("Can't modify a role with higher or equal highest role than yourself! Role: " + role.toString());
      } else {
         Route.CompiledRoute route = Route.Guilds.ADD_MEMBER_ROLE.compile(this.getId(), Long.toUnsignedString(userId), role.getId());
         return new AuditableRestActionImpl(this.getJDA(), route);
      }
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> addRoleToMember(@Nonnull String userId, @Nonnull Role role) {
      return this.addRoleToMember(MiscUtil.parseSnowflake(userId), role);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> removeRoleFromMember(@Nonnull Member var1, @Nonnull Role var2);

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> removeRoleFromMember(long userId, @Nonnull Role role) {
      Checks.notNull(role, "Role");
      Checks.check(role.getGuild().equals(this), "Role must be from the same guild! Trying to use role from %s in %s", role.getGuild().toString(), this.toString());
      Member member = this.getMemberById(userId);
      if (member != null) {
         return this.removeRoleFromMember(member, role);
      } else if (!this.getSelfMember().hasPermission(new Permission[]{Permission.MANAGE_ROLES})) {
         throw new InsufficientPermissionException(this, Permission.MANAGE_ROLES);
      } else if (!this.getSelfMember().canInteract(role)) {
         throw new HierarchyException("Can't modify a role with higher or equal highest role than yourself! Role: " + role.toString());
      } else {
         Route.CompiledRoute route = Route.Guilds.REMOVE_MEMBER_ROLE.compile(this.getId(), Long.toUnsignedString(userId), role.getId());
         return new AuditableRestActionImpl(this.getJDA(), route);
      }
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> removeRoleFromMember(@Nonnull String userId, @Nonnull Role role) {
      return this.removeRoleFromMember(MiscUtil.parseSnowflake(userId), role);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> modifyMemberRoles(@Nonnull Member var1, @Nullable Collection<Role> var2, @Nullable Collection<Role> var3);

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Void> modifyMemberRoles(@Nonnull Member member, @Nonnull Role... roles) {
      return this.modifyMemberRoles(member, (Collection)Arrays.asList(roles));
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> modifyMemberRoles(@Nonnull Member var1, @Nonnull Collection<Role> var2);

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Void> transferOwnership(@Nonnull Member var1);

   @Nonnull
   @CheckReturnValue
   default ChannelAction<TextChannel> createTextChannel(@Nonnull String name) {
      return this.createTextChannel(name, (Category)null);
   }

   @Nonnull
   @CheckReturnValue
   ChannelAction<TextChannel> createTextChannel(@Nonnull String var1, @Nullable Category var2);

   @Nonnull
   @CheckReturnValue
   default ChannelAction<VoiceChannel> createVoiceChannel(@Nonnull String name) {
      return this.createVoiceChannel(name, (Category)null);
   }

   @Nonnull
   @CheckReturnValue
   ChannelAction<VoiceChannel> createVoiceChannel(@Nonnull String var1, @Nullable Category var2);

   @Nonnull
   @CheckReturnValue
   ChannelAction<Category> createCategory(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   default <T extends GuildChannel> ChannelAction<T> createCopyOfChannel(@Nonnull T channel) {
      Checks.notNull(channel, "Channel");
      return channel.createCopy(this);
   }

   @Nonnull
   @CheckReturnValue
   RoleAction createRole();

   @Nonnull
   @CheckReturnValue
   default RoleAction createCopyOfRole(@Nonnull Role role) {
      Checks.notNull(role, "Role");
      return role.createCopy(this);
   }

   @Nonnull
   @CheckReturnValue
   AuditableRestAction<Emote> createEmote(@Nonnull String var1, @Nonnull Icon var2, @Nonnull Role... var3);

   @Nonnull
   @CheckReturnValue
   ChannelOrderAction modifyCategoryPositions();

   @Nonnull
   @CheckReturnValue
   ChannelOrderAction modifyTextChannelPositions();

   @Nonnull
   @CheckReturnValue
   ChannelOrderAction modifyVoiceChannelPositions();

   @Nonnull
   @CheckReturnValue
   CategoryOrderAction modifyTextChannelPositions(@Nonnull Category var1);

   @Nonnull
   @CheckReturnValue
   CategoryOrderAction modifyVoiceChannelPositions(@Nonnull Category var1);

   @Nonnull
   @CheckReturnValue
   default RoleOrderAction modifyRolePositions() {
      return this.modifyRolePositions(true);
   }

   @Nonnull
   @CheckReturnValue
   RoleOrderAction modifyRolePositions(boolean var1);

   public static class MetaData {
      private final int memberLimit;
      private final int presenceLimit;
      private final int approximatePresences;
      private final int approximateMembers;

      public MetaData(int memberLimit, int presenceLimit, int approximatePresences, int approximateMembers) {
         this.memberLimit = memberLimit;
         this.presenceLimit = presenceLimit;
         this.approximatePresences = approximatePresences;
         this.approximateMembers = approximateMembers;
      }

      public int getMemberLimit() {
         return this.memberLimit;
      }

      public int getPresenceLimit() {
         return this.presenceLimit;
      }

      public int getApproximatePresences() {
         return this.approximatePresences;
      }

      public int getApproximateMembers() {
         return this.approximateMembers;
      }
   }

   public static class Ban {
      protected final User user;
      protected final String reason;

      public Ban(User user, String reason) {
         this.user = user;
         this.reason = reason;
      }

      @Nonnull
      public User getUser() {
         return this.user;
      }

      @Nullable
      public String getReason() {
         return this.reason;
      }

      public String toString() {
         return "GuildBan:" + this.user + (this.reason == null ? "" : '(' + this.reason + ')');
      }
   }

   public static enum BoostTier {
      NONE(0, 96000, 50),
      TIER_1(1, 128000, 100),
      TIER_2(2, 256000, 150),
      TIER_3(3, 384000, 250),
      UNKNOWN(-1, Integer.MAX_VALUE, Integer.MAX_VALUE);

      private final int key;
      private final int maxBitrate;
      private final int maxEmotes;

      private BoostTier(int key, int maxBitrate, int maxEmotes) {
         this.key = key;
         this.maxBitrate = maxBitrate;
         this.maxEmotes = maxEmotes;
      }

      public int getKey() {
         return this.key;
      }

      public int getMaxBitrate() {
         return this.maxBitrate;
      }

      public int getMaxEmotes() {
         return this.maxEmotes;
      }

      public long getMaxFileSize() {
         if (this.key == 2) {
            return 52428800L;
         } else {
            return this.key == 3 ? 104857600L : 8388608L;
         }
      }

      @Nonnull
      public static Guild.BoostTier fromKey(int key) {
         Guild.BoostTier[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Guild.BoostTier tier = var1[var3];
            if (tier.key == key) {
               return tier;
            }
         }

         return UNKNOWN;
      }
   }

   public static enum ExplicitContentLevel {
      OFF(0, "Don't scan any messages."),
      NO_ROLE(1, "Scan messages from members without a role."),
      ALL(2, "Scan messages sent by all members."),
      UNKNOWN(-1, "Unknown filter level!");

      private final int key;
      private final String description;

      private ExplicitContentLevel(int key, String description) {
         this.key = key;
         this.description = description;
      }

      public int getKey() {
         return this.key;
      }

      @Nonnull
      public String getDescription() {
         return this.description;
      }

      @Nonnull
      public static Guild.ExplicitContentLevel fromKey(int key) {
         Guild.ExplicitContentLevel[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Guild.ExplicitContentLevel level = var1[var3];
            if (level.key == key) {
               return level;
            }
         }

         return UNKNOWN;
      }
   }

   public static enum MFALevel {
      NONE(0),
      TWO_FACTOR_AUTH(1),
      UNKNOWN(-1);

      private final int key;

      private MFALevel(int key) {
         this.key = key;
      }

      public int getKey() {
         return this.key;
      }

      @Nonnull
      public static Guild.MFALevel fromKey(int key) {
         Guild.MFALevel[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Guild.MFALevel level = var1[var3];
            if (level.getKey() == key) {
               return level;
            }
         }

         return UNKNOWN;
      }
   }

   public static enum NotificationLevel {
      ALL_MESSAGES(0),
      MENTIONS_ONLY(1),
      UNKNOWN(-1);

      private final int key;

      private NotificationLevel(int key) {
         this.key = key;
      }

      public int getKey() {
         return this.key;
      }

      @Nonnull
      public static Guild.NotificationLevel fromKey(int key) {
         Guild.NotificationLevel[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Guild.NotificationLevel level = var1[var3];
            if (level.getKey() == key) {
               return level;
            }
         }

         return UNKNOWN;
      }
   }

   public static enum VerificationLevel {
      NONE(0),
      LOW(1),
      MEDIUM(2),
      HIGH(3),
      VERY_HIGH(4),
      UNKNOWN(-1);

      private final int key;

      private VerificationLevel(int key) {
         this.key = key;
      }

      public int getKey() {
         return this.key;
      }

      @Nonnull
      public static Guild.VerificationLevel fromKey(int key) {
         Guild.VerificationLevel[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Guild.VerificationLevel level = var1[var3];
            if (level.getKey() == key) {
               return level;
            }
         }

         return UNKNOWN;
      }
   }

   public static enum Timeout {
      SECONDS_60(60),
      SECONDS_300(300),
      SECONDS_900(900),
      SECONDS_1800(1800),
      SECONDS_3600(3600);

      private final int seconds;

      private Timeout(int seconds) {
         this.seconds = seconds;
      }

      public int getSeconds() {
         return this.seconds;
      }

      @Nonnull
      public static Guild.Timeout fromKey(int seconds) {
         Guild.Timeout[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Guild.Timeout t = var1[var3];
            if (t.getSeconds() == seconds) {
               return t;
            }
         }

         throw new IllegalArgumentException("Provided key was not recognized. Seconds: " + seconds);
      }
   }
}

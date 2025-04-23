package net.dv8tion.jda.api.sharding;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.ShardCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;

public interface ShardManager {
   default void addEventListener(@Nonnull Object... listeners) {
      Checks.noneNull(listeners, "listeners");
      this.getShardCache().forEach((jda) -> {
         jda.addEventListener(listeners);
      });
   }

   default void removeEventListener(@Nonnull Object... listeners) {
      Checks.noneNull(listeners, "listeners");
      this.getShardCache().forEach((jda) -> {
         jda.removeEventListener(listeners);
      });
   }

   default void addEventListeners(@Nonnull IntFunction<Object> eventListenerProvider) {
      Checks.notNull(eventListenerProvider, "event listener provider");
      this.getShardCache().forEach((jda) -> {
         Object listener = eventListenerProvider.apply(jda.getShardInfo().getShardId());
         if (listener != null) {
            jda.addEventListener(listener);
         }

      });
   }

   default void removeEventListeners(@Nonnull IntFunction<Collection<Object>> eventListenerProvider) {
      Checks.notNull(eventListenerProvider, "event listener provider");
      this.getShardCache().forEach((jda) -> {
         jda.removeEventListener(eventListenerProvider.apply(jda.getShardInfo().getShardId()));
      });
   }

   default void removeEventListenerProvider(@Nonnull IntFunction<Object> eventListenerProvider) {
   }

   int getShardsQueued();

   default int getShardsRunning() {
      return (int)this.getShardCache().size();
   }

   default int getShardsTotal() {
      return this.getShardsQueued() + this.getShardsRunning();
   }

   @Nonnull
   default EnumSet<GatewayIntent> getGatewayIntents() {
      return (EnumSet)this.getShardCache().applyStream((stream) -> {
         return (EnumSet)stream.map(JDA::getGatewayIntents).findAny().orElse(EnumSet.noneOf(GatewayIntent.class));
      });
   }

   @Nonnull
   default RestAction<ApplicationInfo> retrieveApplicationInfo() {
      return ((JDA)this.getShardCache().stream().findAny().orElseThrow(() -> {
         return new IllegalStateException("no active shards");
      })).retrieveApplicationInfo();
   }

   default double getAverageGatewayPing() {
      return this.getShardCache().stream().mapToLong(JDA::getGatewayPing).filter((ping) -> {
         return ping != -1L;
      }).average().orElse(-1.0D);
   }

   @Nonnull
   default List<Category> getCategories() {
      return this.getCategoryCache().asList();
   }

   @Nonnull
   default List<Category> getCategoriesByName(@Nonnull String name, boolean ignoreCase) {
      return this.getCategoryCache().getElementsByName(name, ignoreCase);
   }

   @Nullable
   default Category getCategoryById(long id) {
      return (Category)this.getCategoryCache().getElementById(id);
   }

   @Nullable
   default Category getCategoryById(@Nonnull String id) {
      return (Category)this.getCategoryCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<Category> getCategoryCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getCategoryCache);
      });
   }

   @Nullable
   default Emote getEmoteById(long id) {
      return (Emote)this.getEmoteCache().getElementById(id);
   }

   @Nullable
   default Emote getEmoteById(@Nonnull String id) {
      return (Emote)this.getEmoteCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<Emote> getEmoteCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getEmoteCache);
      });
   }

   @Nonnull
   default List<Emote> getEmotes() {
      return this.getEmoteCache().asList();
   }

   @Nonnull
   default List<Emote> getEmotesByName(@Nonnull String name, boolean ignoreCase) {
      return this.getEmoteCache().getElementsByName(name, ignoreCase);
   }

   @Nullable
   default Guild getGuildById(long id) {
      return (Guild)this.getGuildCache().getElementById(id);
   }

   @Nullable
   default Guild getGuildById(@Nonnull String id) {
      return this.getGuildById(MiscUtil.parseSnowflake(id));
   }

   @Nonnull
   default List<Guild> getGuildsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getGuildCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   default SnowflakeCacheView<Guild> getGuildCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getGuildCache);
      });
   }

   @Nonnull
   default List<Guild> getGuilds() {
      return this.getGuildCache().asList();
   }

   @Nonnull
   default List<Guild> getMutualGuilds(@Nonnull Collection<User> users) {
      Checks.noneNull(users, "users");
      return Collections.unmodifiableList((List)this.getGuildCache().stream().filter((guild) -> {
         Stream var10000 = users.stream();
         Objects.requireNonNull(guild);
         return var10000.allMatch(guild::isMember);
      }).collect(Collectors.toList()));
   }

   @Nonnull
   default List<Guild> getMutualGuilds(@Nonnull User... users) {
      Checks.notNull(users, "users");
      return this.getMutualGuilds((Collection)Arrays.asList(users));
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<User> retrieveUserById(@Nonnull String id) {
      return this.retrieveUserById(MiscUtil.parseSnowflake(id));
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<User> retrieveUserById(long id) {
      JDA api = null;
      Iterator var4 = this.getShardCache().iterator();

      JDA shard;
      User user;
      boolean isUpdated;
      do {
         if (!var4.hasNext()) {
            if (api == null) {
               throw new IllegalStateException("no shards active");
            }

            JDAImpl jda = (JDAImpl)api;
            Route.CompiledRoute route = Route.Users.GET_USER.compile(Long.toUnsignedString(id));
            return new RestActionImpl(jda, route, (response, request) -> {
               return jda.getEntityBuilder().createUser(response.getObject());
            });
         }

         shard = (JDA)var4.next();
         api = shard;
         EnumSet<GatewayIntent> intents = shard.getGatewayIntents();
         user = shard.getUserById(id);
         isUpdated = intents.contains(GatewayIntent.GUILD_PRESENCES) || intents.contains(GatewayIntent.GUILD_MEMBERS);
      } while(user == null || !isUpdated);

      return new CompletedRestAction(shard, user);
   }

   @Nullable
   default User getUserByTag(@Nonnull String tag) {
      return (User)this.getShardCache().applyStream((stream) -> {
         return (User)stream.map((jda) -> {
            return jda.getUserByTag(tag);
         }).filter(Objects::nonNull).findFirst().orElse((Object)null);
      });
   }

   @Nullable
   default User getUserByTag(@Nonnull String username, @Nonnull String discriminator) {
      return (User)this.getShardCache().applyStream((stream) -> {
         return (User)stream.map((jda) -> {
            return jda.getUserByTag(username, discriminator);
         }).filter(Objects::nonNull).findFirst().orElse((Object)null);
      });
   }

   @Nullable
   default PrivateChannel getPrivateChannelById(long id) {
      return (PrivateChannel)this.getPrivateChannelCache().getElementById(id);
   }

   @Nullable
   default PrivateChannel getPrivateChannelById(@Nonnull String id) {
      return (PrivateChannel)this.getPrivateChannelCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getPrivateChannelCache);
      });
   }

   @Nonnull
   default List<PrivateChannel> getPrivateChannels() {
      return this.getPrivateChannelCache().asList();
   }

   @Nullable
   default Role getRoleById(long id) {
      return (Role)this.getRoleCache().getElementById(id);
   }

   @Nullable
   default Role getRoleById(@Nonnull String id) {
      return (Role)this.getRoleCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<Role> getRoleCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getRoleCache);
      });
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
   default GuildChannel getGuildChannelById(@Nonnull String id) {
      return this.getGuildChannelById(MiscUtil.parseSnowflake(id));
   }

   @Nullable
   default GuildChannel getGuildChannelById(long id) {
      Iterator var4 = this.getShards().iterator();

      GuildChannel channel;
      do {
         if (!var4.hasNext()) {
            return null;
         }

         JDA shard = (JDA)var4.next();
         channel = shard.getGuildChannelById(id);
      } while(channel == null);

      return channel;
   }

   @Nullable
   default GuildChannel getGuildChannelById(@Nonnull ChannelType type, @Nonnull String id) {
      return this.getGuildChannelById(type, MiscUtil.parseSnowflake(id));
   }

   @Nullable
   default GuildChannel getGuildChannelById(@Nonnull ChannelType type, long id) {
      Checks.notNull(type, "ChannelType");
      Iterator var5 = this.getShards().iterator();

      GuildChannel channel;
      do {
         if (!var5.hasNext()) {
            return null;
         }

         JDA shard = (JDA)var5.next();
         channel = shard.getGuildChannelById(type, id);
      } while(channel == null);

      return channel;
   }

   @Nullable
   default JDA getShardById(int id) {
      return this.getShardCache().getElementById(id);
   }

   @Nullable
   default JDA getShardById(@Nonnull String id) {
      return this.getShardCache().getElementById(id);
   }

   @Nonnull
   ShardCacheView getShardCache();

   @Nonnull
   default List<JDA> getShards() {
      return this.getShardCache().asList();
   }

   @Nullable
   default JDA.Status getStatus(int shardId) {
      JDA jda = this.getShardCache().getElementById(shardId);
      return jda == null ? null : jda.getStatus();
   }

   @Nonnull
   default Map<JDA, JDA.Status> getStatuses() {
      return Collections.unmodifiableMap((Map)this.getShardCache().stream().collect(Collectors.toMap(Function.identity(), JDA::getStatus)));
   }

   @Nullable
   default TextChannel getTextChannelById(long id) {
      return (TextChannel)this.getTextChannelCache().getElementById(id);
   }

   @Nullable
   default TextChannel getTextChannelById(@Nonnull String id) {
      return (TextChannel)this.getTextChannelCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<TextChannel> getTextChannelCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getTextChannelCache);
      });
   }

   @Nonnull
   default List<TextChannel> getTextChannels() {
      return this.getTextChannelCache().asList();
   }

   @Nullable
   default StoreChannel getStoreChannelById(long id) {
      return (StoreChannel)this.getStoreChannelCache().getElementById(id);
   }

   @Nullable
   default StoreChannel getStoreChannelById(@Nonnull String id) {
      return (StoreChannel)this.getStoreChannelCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<StoreChannel> getStoreChannelCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getStoreChannelCache);
      });
   }

   @Nonnull
   default List<StoreChannel> getStoreChannels() {
      return this.getStoreChannelCache().asList();
   }

   @Nullable
   default User getUserById(long id) {
      return (User)this.getUserCache().getElementById(id);
   }

   @Nullable
   default User getUserById(@Nonnull String id) {
      return (User)this.getUserCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<User> getUserCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getUserCache);
      });
   }

   @Nonnull
   default List<User> getUsers() {
      return this.getUserCache().asList();
   }

   @Nullable
   default VoiceChannel getVoiceChannelById(long id) {
      return (VoiceChannel)this.getVoiceChannelCache().getElementById(id);
   }

   @Nullable
   default VoiceChannel getVoiceChannelById(@Nonnull String id) {
      return (VoiceChannel)this.getVoiceChannelCache().getElementById(id);
   }

   @Nonnull
   default SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
      return CacheView.allSnowflakes(() -> {
         return this.getShardCache().stream().map(JDA::getVoiceChannelCache);
      });
   }

   @Nonnull
   default List<VoiceChannel> getVoiceChannels() {
      return this.getVoiceChannelCache().asList();
   }

   void restart();

   void restart(int var1);

   /** @deprecated */
   @Deprecated
   @DeprecatedSince("4.0.0")
   @ReplaceWith("setActivity()")
   default void setGame(@Nullable Activity game) {
      this.setActivityProvider((id) -> {
         return game;
      });
   }

   default void setActivity(@Nullable Activity activity) {
      this.setActivityProvider((id) -> {
         return activity;
      });
   }

   default void setActivityProvider(@Nullable IntFunction<? extends Activity> activityProvider) {
      this.getShardCache().forEach((jda) -> {
         jda.getPresence().setActivity(activityProvider == null ? null : (Activity)activityProvider.apply(jda.getShardInfo().getShardId()));
      });
   }

   default void setIdle(boolean idle) {
      this.setIdleProvider((id) -> {
         return idle;
      });
   }

   default void setIdleProvider(@Nonnull IntFunction<Boolean> idleProvider) {
      this.getShardCache().forEach((jda) -> {
         jda.getPresence().setIdle((Boolean)idleProvider.apply(jda.getShardInfo().getShardId()));
      });
   }

   default void setPresence(@Nullable OnlineStatus status, @Nullable Activity activity) {
      this.setPresenceProvider((id) -> {
         return status;
      }, (id) -> {
         return activity;
      });
   }

   default void setPresenceProvider(@Nullable IntFunction<OnlineStatus> statusProvider, @Nullable IntFunction<? extends Activity> activityProvider) {
      this.getShardCache().forEach((jda) -> {
         jda.getPresence().setPresence(statusProvider == null ? null : (OnlineStatus)statusProvider.apply(jda.getShardInfo().getShardId()), activityProvider == null ? null : (Activity)activityProvider.apply(jda.getShardInfo().getShardId()));
      });
   }

   default void setStatus(@Nullable OnlineStatus status) {
      this.setStatusProvider((id) -> {
         return status;
      });
   }

   default void setStatusProvider(@Nullable IntFunction<OnlineStatus> statusProvider) {
      this.getShardCache().forEach((jda) -> {
         jda.getPresence().setStatus(statusProvider == null ? null : (OnlineStatus)statusProvider.apply(jda.getShardInfo().getShardId()));
      });
   }

   void shutdown();

   void shutdown(int var1);

   void start(int var1);
}

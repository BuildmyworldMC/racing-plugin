package net.dv8tion.jda.api;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.DirectAudioController;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;
import okhttp3.OkHttpClient;

public interface JDA {
   @Nonnull
   JDA.Status getStatus();

   @Nonnull
   EnumSet<GatewayIntent> getGatewayIntents();

   boolean unloadUser(long var1);

   long getGatewayPing();

   @Nonnull
   default RestAction<Long> getRestPing() {
      AtomicLong time = new AtomicLong();
      Route.CompiledRoute route = Route.Self.GET_SELF.compile();
      RestActionImpl<Long> action = new RestActionImpl(this, route, (response, request) -> {
         return System.currentTimeMillis() - time.get();
      });
      action.setCheck(() -> {
         time.set(System.currentTimeMillis());
         return true;
      });
      return action;
   }

   @Nonnull
   default JDA awaitStatus(@Nonnull JDA.Status status) throws InterruptedException {
      return this.awaitStatus(status);
   }

   @Nonnull
   JDA awaitStatus(@Nonnull JDA.Status var1, @Nonnull JDA.Status... var2) throws InterruptedException;

   @Nonnull
   default JDA awaitReady() throws InterruptedException {
      return this.awaitStatus(JDA.Status.CONNECTED);
   }

   int cancelRequests();

   @Nonnull
   ScheduledExecutorService getRateLimitPool();

   @Nonnull
   ScheduledExecutorService getGatewayPool();

   @Nonnull
   ExecutorService getCallbackPool();

   @Nonnull
   OkHttpClient getHttpClient();

   @Nonnull
   DirectAudioController getDirectAudioController();

   void setEventManager(@Nullable IEventManager var1);

   void addEventListener(@Nonnull Object... var1);

   void removeEventListener(@Nonnull Object... var1);

   @Nonnull
   List<Object> getRegisteredListeners();

   @Nonnull
   @CheckReturnValue
   GuildAction createGuild(@Nonnull String var1);

   @Nonnull
   CacheView<AudioManager> getAudioManagerCache();

   @Nonnull
   default List<AudioManager> getAudioManagers() {
      return this.getAudioManagerCache().asList();
   }

   @Nonnull
   SnowflakeCacheView<User> getUserCache();

   @Nonnull
   default List<User> getUsers() {
      return this.getUserCache().asList();
   }

   @Nullable
   default User getUserById(@Nonnull String id) {
      return (User)this.getUserCache().getElementById(id);
   }

   @Nullable
   default User getUserById(long id) {
      return (User)this.getUserCache().getElementById(id);
   }

   @Nullable
   default User getUserByTag(@Nonnull String tag) {
      Checks.notNull(tag, "Tag");
      Matcher matcher = User.USER_TAG.matcher(tag);
      Checks.check(matcher.matches(), "Invalid tag format!");
      String username = matcher.group(1);
      String discriminator = matcher.group(2);
      return this.getUserByTag(username, discriminator);
   }

   @Nullable
   default User getUserByTag(@Nonnull String username, @Nonnull String discriminator) {
      Checks.notNull(username, "Username");
      Checks.notNull(discriminator, "Discriminator");
      Checks.check(discriminator.length() == 4 && Helpers.isNumeric(discriminator), "Invalid format for discriminator!");
      Checks.check(username.length() >= 2 && username.length() <= 32, "Username must be between 2 and 32 characters in length!");
      return (User)this.getUserCache().applyStream((stream) -> {
         return (User)stream.filter((it) -> {
            return it.getDiscriminator().equals(discriminator);
         }).filter((it) -> {
            return it.getName().equals(username);
         }).findFirst().orElse((Object)null);
      });
   }

   @Nonnull
   default List<User> getUsersByName(@Nonnull String name, boolean ignoreCase) {
      return this.getUserCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   List<Guild> getMutualGuilds(@Nonnull User... var1);

   @Nonnull
   List<Guild> getMutualGuilds(@Nonnull Collection<User> var1);

   @Nonnull
   @CheckReturnValue
   default RestAction<User> retrieveUserById(@Nonnull String id) {
      return this.retrieveUserById(id, true);
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<User> retrieveUserById(long id) {
      return this.retrieveUserById(id, true);
   }

   @Nonnull
   @CheckReturnValue
   default RestAction<User> retrieveUserById(@Nonnull String id, boolean update) {
      return this.retrieveUserById(MiscUtil.parseSnowflake(id), update);
   }

   @Nonnull
   @CheckReturnValue
   RestAction<User> retrieveUserById(long var1, boolean var3);

   @Nonnull
   SnowflakeCacheView<Guild> getGuildCache();

   @Nonnull
   default List<Guild> getGuilds() {
      return this.getGuildCache().asList();
   }

   @Nullable
   default Guild getGuildById(@Nonnull String id) {
      return (Guild)this.getGuildCache().getElementById(id);
   }

   @Nullable
   default Guild getGuildById(long id) {
      return (Guild)this.getGuildCache().getElementById(id);
   }

   @Nonnull
   default List<Guild> getGuildsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getGuildCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   Set<String> getUnavailableGuilds();

   boolean isUnavailable(long var1);

   @Nonnull
   SnowflakeCacheView<Role> getRoleCache();

   @Nonnull
   default List<Role> getRoles() {
      return this.getRoleCache().asList();
   }

   @Nullable
   default Role getRoleById(@Nonnull String id) {
      return (Role)this.getRoleCache().getElementById(id);
   }

   @Nullable
   default Role getRoleById(long id) {
      return (Role)this.getRoleCache().getElementById(id);
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

   @Nonnull
   SnowflakeCacheView<Category> getCategoryCache();

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
   SnowflakeCacheView<StoreChannel> getStoreChannelCache();

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
   SnowflakeCacheView<TextChannel> getTextChannelCache();

   @Nonnull
   default List<TextChannel> getTextChannels() {
      return this.getTextChannelCache().asList();
   }

   @Nullable
   default TextChannel getTextChannelById(@Nonnull String id) {
      return (TextChannel)this.getTextChannelCache().getElementById(id);
   }

   @Nullable
   default TextChannel getTextChannelById(long id) {
      return (TextChannel)this.getTextChannelCache().getElementById(id);
   }

   @Nonnull
   default List<TextChannel> getTextChannelsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getTextChannelCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SnowflakeCacheView<VoiceChannel> getVoiceChannelCache();

   @Nonnull
   default List<VoiceChannel> getVoiceChannels() {
      return this.getVoiceChannelCache().asList();
   }

   @Nullable
   default VoiceChannel getVoiceChannelById(@Nonnull String id) {
      return (VoiceChannel)this.getVoiceChannelCache().getElementById(id);
   }

   @Nullable
   default VoiceChannel getVoiceChannelById(long id) {
      return (VoiceChannel)this.getVoiceChannelCache().getElementById(id);
   }

   /** @deprecated */
   @Nonnull
   @Deprecated
   @DeprecatedSince("4.0.0")
   @ForRemoval(
      deadline = "4.3.0"
   )
   @ReplaceWith("jda.getVoiceChannelsByName(name, ignoreCase)")
   default List<VoiceChannel> getVoiceChannelByName(@Nonnull String name, boolean ignoreCase) {
      return this.getVoiceChannelsByName(name, ignoreCase);
   }

   @Nonnull
   default List<VoiceChannel> getVoiceChannelsByName(@Nonnull String name, boolean ignoreCase) {
      return this.getVoiceChannelCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   SnowflakeCacheView<PrivateChannel> getPrivateChannelCache();

   @Nonnull
   default List<PrivateChannel> getPrivateChannels() {
      return this.getPrivateChannelCache().asList();
   }

   @Nullable
   default PrivateChannel getPrivateChannelById(@Nonnull String id) {
      return (PrivateChannel)this.getPrivateChannelCache().getElementById(id);
   }

   @Nullable
   default PrivateChannel getPrivateChannelById(long id) {
      return (PrivateChannel)this.getPrivateChannelCache().getElementById(id);
   }

   @Nonnull
   @CheckReturnValue
   RestAction<PrivateChannel> openPrivateChannelById(long var1);

   @Nonnull
   @CheckReturnValue
   default RestAction<PrivateChannel> openPrivateChannelById(@Nonnull String userId) {
      return this.openPrivateChannelById(MiscUtil.parseSnowflake(userId));
   }

   @Nonnull
   SnowflakeCacheView<Emote> getEmoteCache();

   @Nonnull
   default List<Emote> getEmotes() {
      return this.getEmoteCache().asList();
   }

   @Nullable
   default Emote getEmoteById(@Nonnull String id) {
      return (Emote)this.getEmoteCache().getElementById(id);
   }

   @Nullable
   default Emote getEmoteById(long id) {
      return (Emote)this.getEmoteCache().getElementById(id);
   }

   @Nonnull
   default List<Emote> getEmotesByName(@Nonnull String name, boolean ignoreCase) {
      return this.getEmoteCache().getElementsByName(name, ignoreCase);
   }

   @Nonnull
   IEventManager getEventManager();

   @Nonnull
   SelfUser getSelfUser();

   @Nonnull
   Presence getPresence();

   @Nonnull
   JDA.ShardInfo getShardInfo();

   @Nonnull
   String getToken();

   long getResponseTotal();

   int getMaxReconnectDelay();

   void setAutoReconnect(boolean var1);

   void setRequestTimeoutRetry(boolean var1);

   boolean isAutoReconnect();

   boolean isBulkDeleteSplittingEnabled();

   void shutdown();

   void shutdownNow();

   @Nonnull
   AccountType getAccountType();

   @Nonnull
   @CheckReturnValue
   RestAction<ApplicationInfo> retrieveApplicationInfo();

   @Nonnull
   String getInviteUrl(@Nullable Permission... var1);

   @Nonnull
   String getInviteUrl(@Nullable Collection<Permission> var1);

   @Nullable
   ShardManager getShardManager();

   @Nonnull
   @CheckReturnValue
   RestAction<Webhook> retrieveWebhookById(@Nonnull String var1);

   @Nonnull
   @CheckReturnValue
   default RestAction<Webhook> retrieveWebhookById(long webhookId) {
      return this.retrieveWebhookById(Long.toUnsignedString(webhookId));
   }

   @Nonnull
   @CheckReturnValue
   default AuditableRestAction<Integer> installAuxiliaryPort() {
      int port = ThreadLocalRandom.current().nextInt();
      if (Desktop.isDesktopSupported()) {
         try {
            Desktop.getDesktop().browse(new URI("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
         } catch (URISyntaxException | IOException var3) {
            throw new IllegalStateException("No port available");
         }

         return new CompletedRestAction(this, port);
      } else {
         throw new IllegalStateException("No port available");
      }
   }

   public static class ShardInfo {
      public static final JDA.ShardInfo SINGLE = new JDA.ShardInfo(0, 1);
      int shardId;
      int shardTotal;

      public ShardInfo(int shardId, int shardTotal) {
         this.shardId = shardId;
         this.shardTotal = shardTotal;
      }

      public int getShardId() {
         return this.shardId;
      }

      public int getShardTotal() {
         return this.shardTotal;
      }

      public String getShardString() {
         return "[" + this.shardId + " / " + this.shardTotal + "]";
      }

      public String toString() {
         return "Shard " + this.getShardString();
      }

      public boolean equals(Object o) {
         if (!(o instanceof JDA.ShardInfo)) {
            return false;
         } else {
            JDA.ShardInfo oInfo = (JDA.ShardInfo)o;
            return this.shardId == oInfo.getShardId() && this.shardTotal == oInfo.getShardTotal();
         }
      }
   }

   public static enum Status {
      INITIALIZING(true),
      INITIALIZED(true),
      LOGGING_IN(true),
      CONNECTING_TO_WEBSOCKET(true),
      IDENTIFYING_SESSION(true),
      AWAITING_LOGIN_CONFIRMATION(true),
      LOADING_SUBSYSTEMS(true),
      CONNECTED(true),
      DISCONNECTED,
      RECONNECT_QUEUED,
      WAITING_TO_RECONNECT,
      ATTEMPTING_TO_RECONNECT,
      SHUTTING_DOWN,
      SHUTDOWN,
      FAILED_TO_LOGIN;

      private final boolean isInit;

      private Status(boolean isInit) {
         this.isInit = isInit;
      }

      private Status() {
         this.isInit = false;
      }

      public boolean isInit() {
         return this.isInit;
      }
   }
}

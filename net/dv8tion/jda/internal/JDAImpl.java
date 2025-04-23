package net.dv8tion.jda.internal;

import com.neovisionaries.ws.client.WebSocketFactory;
import gnu.trove.set.TLongSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.factory.DefaultSendFactory;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.StoreChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.events.GatewayPingEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.exceptions.AccountTypeException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.Presence;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.cache.CacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.entities.UserImpl;
import net.dv8tion.jda.internal.handle.EventCache;
import net.dv8tion.jda.internal.handle.GuildSetupController;
import net.dv8tion.jda.internal.hooks.EventManagerProxy;
import net.dv8tion.jda.internal.managers.AudioManagerImpl;
import net.dv8tion.jda.internal.managers.DirectAudioControllerImpl;
import net.dv8tion.jda.internal.managers.PresenceImpl;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.dv8tion.jda.internal.requests.DeferredRestAction;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.requests.WebSocketClient;
import net.dv8tion.jda.internal.requests.restaction.GuildActionImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.AbstractCacheView;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig;
import net.dv8tion.jda.internal.utils.config.MetaConfig;
import net.dv8tion.jda.internal.utils.config.SessionConfig;
import net.dv8tion.jda.internal.utils.config.ThreadingConfig;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class JDAImpl implements JDA {
   public static final Logger LOG = JDALogger.getLog(JDA.class);
   protected final SnowflakeCacheViewImpl<User> userCache;
   protected final SnowflakeCacheViewImpl<Guild> guildCache;
   protected final SnowflakeCacheViewImpl<Category> categories;
   protected final SnowflakeCacheViewImpl<StoreChannel> storeChannelCache;
   protected final SnowflakeCacheViewImpl<TextChannel> textChannelCache;
   protected final SnowflakeCacheViewImpl<VoiceChannel> voiceChannelCache;
   protected final SnowflakeCacheViewImpl<PrivateChannel> privateChannelCache;
   protected final AbstractCacheView<AudioManager> audioManagers;
   protected final PresenceImpl presence;
   protected final Thread shutdownHook;
   protected final EntityBuilder entityBuilder;
   protected final EventCache eventCache;
   protected final EventManagerProxy eventManager;
   protected final GuildSetupController guildSetupController;
   protected final DirectAudioControllerImpl audioController;
   protected final AuthorizationConfig authConfig;
   protected final ThreadingConfig threadConfig;
   protected final SessionConfig sessionConfig;
   protected final MetaConfig metaConfig;
   protected WebSocketClient client;
   protected Requester requester;
   protected IAudioSendFactory audioSendFactory;
   protected JDA.Status status;
   protected SelfUser selfUser;
   protected JDA.ShardInfo shardInfo;
   protected long responseTotal;
   protected long gatewayPing;
   protected String gatewayUrl;
   protected ChunkingFilter chunkingFilter;
   protected String clientId;
   protected ShardManager shardManager;
   protected MemberCachePolicy memberCachePolicy;

   public JDAImpl(AuthorizationConfig authConfig) {
      this(authConfig, (SessionConfig)null, (ThreadingConfig)null, (MetaConfig)null);
   }

   public JDAImpl(AuthorizationConfig authConfig, SessionConfig sessionConfig, ThreadingConfig threadConfig, MetaConfig metaConfig) {
      this.userCache = new SnowflakeCacheViewImpl(User.class, User::getName);
      this.guildCache = new SnowflakeCacheViewImpl(Guild.class, Guild::getName);
      this.categories = new SnowflakeCacheViewImpl(Category.class, GuildChannel::getName);
      this.storeChannelCache = new SnowflakeCacheViewImpl(StoreChannel.class, GuildChannel::getName);
      this.textChannelCache = new SnowflakeCacheViewImpl(TextChannel.class, GuildChannel::getName);
      this.voiceChannelCache = new SnowflakeCacheViewImpl(VoiceChannel.class, GuildChannel::getName);
      this.privateChannelCache = new SnowflakeCacheViewImpl(PrivateChannel.class, MessageChannel::getName);
      this.audioManagers = new CacheView.SimpleCacheView(AudioManager.class, (m) -> {
         return m.getGuild().getName();
      });
      this.entityBuilder = new EntityBuilder(this);
      this.audioSendFactory = new DefaultSendFactory();
      this.status = JDA.Status.INITIALIZING;
      this.gatewayPing = -1L;
      this.clientId = null;
      this.shardManager = null;
      this.memberCachePolicy = MemberCachePolicy.ALL;
      this.authConfig = authConfig;
      this.threadConfig = threadConfig == null ? ThreadingConfig.getDefault() : threadConfig;
      this.sessionConfig = sessionConfig == null ? SessionConfig.getDefault() : sessionConfig;
      this.metaConfig = metaConfig == null ? MetaConfig.getDefault() : metaConfig;
      this.shutdownHook = this.metaConfig.isUseShutdownHook() ? new Thread(this::shutdown, "JDA Shutdown Hook") : null;
      this.presence = new PresenceImpl(this);
      this.requester = new Requester(this);
      this.requester.setRetryOnTimeout(this.sessionConfig.isRetryOnTimeout());
      this.guildSetupController = new GuildSetupController(this);
      this.audioController = new DirectAudioControllerImpl(this);
      this.eventCache = new EventCache();
      this.eventManager = new EventManagerProxy(new InterfacedEventManager(), this.threadConfig.getEventPool());
   }

   public void handleEvent(@Nonnull GenericEvent event) {
      this.eventManager.handle(event);
   }

   public boolean isRawEvents() {
      return this.sessionConfig.isRawEvents();
   }

   public boolean isRelativeRateLimit() {
      return this.sessionConfig.isRelativeRateLimit();
   }

   public boolean isCacheFlagSet(CacheFlag flag) {
      return this.metaConfig.getCacheFlags().contains(flag);
   }

   public boolean isIntent(GatewayIntent intent) {
      int raw = intent.getRawValue();
      return (this.client.getGatewayIntents() & raw) == raw;
   }

   public boolean useIntents() {
      return this.client.getGatewayIntents() != -1;
   }

   public int getLargeThreshold() {
      return this.sessionConfig.getLargeThreshold();
   }

   public int getMaxBufferSize() {
      return this.metaConfig.getMaxBufferSize();
   }

   public boolean chunkGuild(long id) {
      try {
         return this.isIntent(GatewayIntent.GUILD_MEMBERS) && this.chunkingFilter.filter(id);
      } catch (Exception var4) {
         LOG.error((String)"Uncaught exception from chunking filter", (Throwable)var4);
         return true;
      }
   }

   public void setChunkingFilter(ChunkingFilter filter) {
      this.chunkingFilter = filter;
   }

   public boolean cacheMember(Member member) {
      try {
         return member.getUser().equals(this.getSelfUser()) || this.chunkGuild(member.getGuild().getIdLong()) || this.memberCachePolicy.cacheMember(member);
      } catch (Exception var3) {
         LOG.error((String)"Uncaught exception from member cache policy", (Throwable)var3);
         return true;
      }
   }

   public void setMemberCachePolicy(MemberCachePolicy policy) {
      this.memberCachePolicy = policy;
   }

   public SessionController getSessionController() {
      return this.sessionConfig.getSessionController();
   }

   public GuildSetupController getGuildSetupController() {
      return this.guildSetupController;
   }

   public VoiceDispatchInterceptor getVoiceInterceptor() {
      return this.sessionConfig.getVoiceDispatchInterceptor();
   }

   public int login() throws LoginException {
      return this.login((String)null, (JDA.ShardInfo)null, Compression.ZLIB, true, GatewayIntent.ALL_INTENTS, GatewayEncoding.JSON);
   }

   public int login(JDA.ShardInfo shardInfo, Compression compression, boolean validateToken, int intents, GatewayEncoding encoding) throws LoginException {
      return this.login((String)null, shardInfo, compression, validateToken, intents, encoding);
   }

   public int login(String gatewayUrl, JDA.ShardInfo shardInfo, Compression compression, boolean validateToken, int intents, GatewayEncoding encoding) throws LoginException {
      this.shardInfo = shardInfo;
      this.threadConfig.init(this::getIdentifierString);
      this.requester.getRateLimiter().init();
      this.gatewayUrl = gatewayUrl == null ? this.getGateway() : gatewayUrl;
      Checks.notNull(this.gatewayUrl, "Gateway URL");
      String token = this.authConfig.getToken();
      this.setStatus(JDA.Status.LOGGING_IN);
      if (token != null && !token.isEmpty()) {
         Map<String, String> previousContext = null;
         ConcurrentMap<String, String> contextMap = this.metaConfig.getMdcContextMap();
         if (contextMap != null) {
            if (shardInfo != null) {
               contextMap.put("jda.shard", shardInfo.getShardString());
               contextMap.put("jda.shard.id", String.valueOf(shardInfo.getShardId()));
               contextMap.put("jda.shard.total", String.valueOf(shardInfo.getShardTotal()));
            }

            previousContext = MDC.getCopyOfContextMap();
            contextMap.forEach(MDC::put);
            this.requester.setContextReady(true);
         }

         if (validateToken) {
            this.verifyToken();
            LOG.info("Login Successful!");
         }

         this.client = new WebSocketClient(this, compression, intents, encoding);
         if (previousContext != null) {
            previousContext.forEach(MDC::put);
         }

         if (this.shutdownHook != null) {
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
         }

         return shardInfo == null ? -1 : shardInfo.getShardTotal();
      } else {
         throw new LoginException("Provided token was null or empty!");
      }
   }

   public String getGateway() {
      return this.getSessionController().getGateway(this);
   }

   public SessionController.ShardedGateway getShardedGateway() {
      return this.getSessionController().getShardedGateway(this);
   }

   public ConcurrentMap<String, String> getContextMap() {
      return this.metaConfig.getMdcContextMap() == null ? null : new ConcurrentHashMap(this.metaConfig.getMdcContextMap());
   }

   public void setContext() {
      if (this.metaConfig.getMdcContextMap() != null) {
         this.metaConfig.getMdcContextMap().forEach(MDC::put);
      }

   }

   public void setToken(String token) {
      this.authConfig.setToken(token);
   }

   public void setStatus(JDA.Status status) {
      synchronized(this.status) {
         JDA.Status oldStatus = this.status;
         this.status = status;
         this.handleEvent(new StatusChangeEvent(this, status, oldStatus));
      }
   }

   public void verifyToken() throws LoginException {
      RestActionImpl<DataObject> login = (new RestActionImpl<DataObject>(this, Route.Self.GET_SELF.compile()) {
         public void handleResponse(Response response, Request<DataObject> request) {
            if (response.isOk()) {
               request.onSuccess(response.getObject());
            } else if (response.isRateLimit()) {
               request.onFailure((Throwable)(new RateLimitedException(request.getRoute(), response.retryAfter)));
            } else if (response.code == 401) {
               request.onSuccess((Object)null);
            } else {
               request.onFailure(response);
            }

         }
      }).priority();
      DataObject userResponse = (DataObject)login.complete();
      if (userResponse != null) {
         this.getEntityBuilder().createSelfUser(userResponse);
      } else {
         this.shutdownNow();
         throw new LoginException("The provided token is invalid!");
      }
   }

   public AuthorizationConfig getAuthorizationConfig() {
      return this.authConfig;
   }

   @Nonnull
   public String getToken() {
      return this.authConfig.getToken();
   }

   public boolean isBulkDeleteSplittingEnabled() {
      return this.sessionConfig.isBulkDeleteSplittingEnabled();
   }

   public void setAutoReconnect(boolean autoReconnect) {
      this.sessionConfig.setAutoReconnect(autoReconnect);
      WebSocketClient client = this.getClient();
      if (client != null) {
         client.setAutoReconnect(autoReconnect);
      }

   }

   public void setRequestTimeoutRetry(boolean retryOnTimeout) {
      this.requester.setRetryOnTimeout(retryOnTimeout);
   }

   public boolean isAutoReconnect() {
      return this.sessionConfig.isAutoReconnect();
   }

   @Nonnull
   public JDA.Status getStatus() {
      return this.status;
   }

   @Nonnull
   public EnumSet<GatewayIntent> getGatewayIntents() {
      return GatewayIntent.getIntents(this.client.getGatewayIntents());
   }

   public boolean unloadUser(long userId) {
      if (userId == this.selfUser.getIdLong()) {
         return false;
      } else {
         User user = this.getUserById(userId);
         if (user == null) {
            return false;
         } else {
            return this.getGuildCache().stream().filter((guild) -> {
               return guild.unloadMember(userId);
            }).count() > 0L;
         }
      }
   }

   public long getGatewayPing() {
      return this.gatewayPing;
   }

   @Nonnull
   public JDA awaitStatus(@Nonnull JDA.Status status, @Nonnull JDA.Status... failOn) throws InterruptedException {
      Checks.notNull(status, "Status");
      Checks.check(status.isInit(), "Cannot await the status %s as it is not part of the login cycle!", (Object)status);
      if (this.getStatus() == JDA.Status.CONNECTED) {
         return this;
      } else {
         List failStatus = Arrays.asList(failOn);

         while(!this.getStatus().isInit() || this.getStatus().ordinal() < status.ordinal()) {
            if (this.getStatus() == JDA.Status.SHUTDOWN) {
               throw new IllegalStateException("Was shutdown trying to await status");
            }

            if (failStatus.contains(this.getStatus())) {
               return this;
            }

            Thread.sleep(50L);
         }

         return this;
      }
   }

   public int cancelRequests() {
      return this.requester.getRateLimiter().cancelRequests();
   }

   @Nonnull
   public ScheduledExecutorService getRateLimitPool() {
      return this.threadConfig.getRateLimitPool();
   }

   @Nonnull
   public ScheduledExecutorService getGatewayPool() {
      return this.threadConfig.getGatewayPool();
   }

   @Nonnull
   public ExecutorService getCallbackPool() {
      return this.threadConfig.getCallbackPool();
   }

   @Nonnull
   public OkHttpClient getHttpClient() {
      return this.sessionConfig.getHttpClient();
   }

   @Nonnull
   public DirectAudioControllerImpl getDirectAudioController() {
      if (!this.isIntent(GatewayIntent.GUILD_VOICE_STATES)) {
         throw new IllegalStateException("Cannot use audio features with disabled GUILD_VOICE_STATES intent!");
      } else {
         return this.audioController;
      }
   }

   @Nonnull
   public List<Guild> getMutualGuilds(@Nonnull User... users) {
      Checks.notNull(users, "users");
      return this.getMutualGuilds((Collection)Arrays.asList(users));
   }

   @Nonnull
   public List<Guild> getMutualGuilds(@Nonnull Collection<User> users) {
      Checks.notNull(users, "users");
      Iterator var2 = users.iterator();

      while(var2.hasNext()) {
         User u = (User)var2.next();
         Checks.notNull(u, "All users");
      }

      return Collections.unmodifiableList((List)this.getGuilds().stream().filter((guild) -> {
         Stream var10000 = users.stream();
         Objects.requireNonNull(guild);
         return var10000.allMatch(guild::isMember);
      }).collect(Collectors.toList()));
   }

   @Nonnull
   public RestAction<User> retrieveUserById(@Nonnull String id) {
      return this.retrieveUserById(MiscUtil.parseSnowflake(id));
   }

   @Nonnull
   public RestAction<User> retrieveUserById(long id, boolean update) {
      if (id == this.getSelfUser().getIdLong()) {
         return new CompletedRestAction(this, this.getSelfUser());
      } else {
         AccountTypeException.check(this.getAccountType(), AccountType.BOT);
         return new DeferredRestAction(this, User.class, () -> {
            return update && !this.isIntent(GatewayIntent.GUILD_MEMBERS) && !this.isIntent(GatewayIntent.GUILD_PRESENCES) ? null : this.getUserById(id);
         }, () -> {
            Route.CompiledRoute route = Route.Users.GET_USER.compile(Long.toUnsignedString(id));
            return new RestActionImpl(this, route, (response, request) -> {
               return this.getEntityBuilder().createUser(response.getObject());
            });
         });
      }
   }

   @Nonnull
   public CacheView<AudioManager> getAudioManagerCache() {
      return this.audioManagers;
   }

   @Nonnull
   public SnowflakeCacheView<Guild> getGuildCache() {
      return this.guildCache;
   }

   @Nonnull
   public Set<String> getUnavailableGuilds() {
      TLongSet unavailableGuilds = this.guildSetupController.getUnavailableGuilds();
      Set<String> copy = new HashSet();
      unavailableGuilds.forEach((id) -> {
         return copy.add(Long.toUnsignedString(id));
      });
      return copy;
   }

   public boolean isUnavailable(long guildId) {
      return this.guildSetupController.isUnavailable(guildId);
   }

   @Nonnull
   public SnowflakeCacheView<Role> getRoleCache() {
      return CacheView.allSnowflakes(() -> {
         return this.guildCache.stream().map(Guild::getRoleCache);
      });
   }

   @Nonnull
   public SnowflakeCacheView<Emote> getEmoteCache() {
      return CacheView.allSnowflakes(() -> {
         return this.guildCache.stream().map(Guild::getEmoteCache);
      });
   }

   @Nonnull
   public SnowflakeCacheView<Category> getCategoryCache() {
      return this.categories;
   }

   @Nonnull
   public SnowflakeCacheView<StoreChannel> getStoreChannelCache() {
      return this.storeChannelCache;
   }

   @Nonnull
   public SnowflakeCacheView<TextChannel> getTextChannelCache() {
      return this.textChannelCache;
   }

   @Nonnull
   public SnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
      return this.voiceChannelCache;
   }

   @Nonnull
   public SnowflakeCacheView<PrivateChannel> getPrivateChannelCache() {
      return this.privateChannelCache;
   }

   @Nonnull
   public RestAction<PrivateChannel> openPrivateChannelById(long userId) {
      if (this.selfUser != null && userId == this.selfUser.getIdLong()) {
         throw new UnsupportedOperationException("Cannot open private channel with yourself!");
      } else {
         return new DeferredRestAction(this, PrivateChannel.class, () -> {
            User user = this.getUserById(userId);
            return user instanceof UserImpl ? ((UserImpl)user).getPrivateChannel() : null;
         }, () -> {
            Route.CompiledRoute route = Route.Self.CREATE_PRIVATE_CHANNEL.compile();
            DataObject body = DataObject.empty().put("recipient_id", userId);
            return new RestActionImpl(this, route, body, (response, request) -> {
               return this.getEntityBuilder().createPrivateChannel(response.getObject());
            });
         });
      }
   }

   @Nonnull
   public SnowflakeCacheView<User> getUserCache() {
      return this.userCache;
   }

   public boolean hasSelfUser() {
      return this.selfUser != null;
   }

   @Nonnull
   public SelfUser getSelfUser() {
      if (this.selfUser == null) {
         throw new IllegalStateException("Session is not yet ready!");
      } else {
         return this.selfUser;
      }
   }

   public synchronized void shutdownNow() {
      this.requester.shutdown();
      this.shutdown();
      this.threadConfig.shutdownNow();
   }

   public synchronized void shutdown() {
      if (this.status != JDA.Status.SHUTDOWN && this.status != JDA.Status.SHUTTING_DOWN) {
         this.setStatus(JDA.Status.SHUTTING_DOWN);
         this.shutdownInternals();
         WebSocketClient client = this.getClient();
         if (client != null) {
            client.getChunkManager().shutdown();
            client.shutdown();
         }

      }
   }

   public synchronized void shutdownInternals() {
      if (this.status != JDA.Status.SHUTDOWN) {
         this.closeAudioConnections();
         this.guildSetupController.close();
         if (this.requester.stop()) {
            this.shutdownRequester();
         }

         this.threadConfig.shutdown();
         if (this.shutdownHook != null) {
            try {
               Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            } catch (Exception var2) {
            }
         }

         this.setStatus(JDA.Status.SHUTDOWN);
      }
   }

   public synchronized void shutdownRequester() {
      this.requester.shutdown();
      this.threadConfig.shutdownRequester();
   }

   private void closeAudioConnections() {
      Stream var10000 = this.getAudioManagerCache().stream();
      Objects.requireNonNull(AudioManagerImpl.class);
      var10000.map(AudioManagerImpl.class::cast).forEach((m) -> {
         m.closeAudioConnection(ConnectionStatus.SHUTTING_DOWN);
      });
   }

   public long getResponseTotal() {
      return this.responseTotal;
   }

   public int getMaxReconnectDelay() {
      return this.sessionConfig.getMaxReconnectDelay();
   }

   @Nonnull
   public JDA.ShardInfo getShardInfo() {
      return this.shardInfo == null ? JDA.ShardInfo.SINGLE : this.shardInfo;
   }

   @Nonnull
   public Presence getPresence() {
      return this.presence;
   }

   @Nonnull
   public IEventManager getEventManager() {
      return this.eventManager.getSubject();
   }

   @Nonnull
   public AccountType getAccountType() {
      return this.authConfig.getAccountType();
   }

   public void setEventManager(IEventManager eventManager) {
      this.eventManager.setSubject(eventManager);
   }

   public void addEventListener(@Nonnull Object... listeners) {
      Checks.noneNull(listeners, "listeners");
      Object[] var2 = listeners;
      int var3 = listeners.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Object listener = var2[var4];
         this.eventManager.register(listener);
      }

   }

   public void removeEventListener(@Nonnull Object... listeners) {
      Checks.noneNull(listeners, "listeners");
      Object[] var2 = listeners;
      int var3 = listeners.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Object listener = var2[var4];
         this.eventManager.unregister(listener);
      }

   }

   @Nonnull
   public List<Object> getRegisteredListeners() {
      return this.eventManager.getRegisteredListeners();
   }

   @Nonnull
   public GuildActionImpl createGuild(@Nonnull String name) {
      if (this.guildCache.size() >= 10L) {
         throw new IllegalStateException("Cannot create a Guild with a Bot in 10 or more guilds!");
      } else {
         return new GuildActionImpl(this, name);
      }
   }

   @Nonnull
   public RestAction<Webhook> retrieveWebhookById(@Nonnull String webhookId) {
      Checks.isSnowflake(webhookId, "Webhook ID");
      Route.CompiledRoute route = Route.Webhooks.GET_WEBHOOK.compile(webhookId);
      return new RestActionImpl(this, route, (response, request) -> {
         DataObject object = response.getObject();
         EntityBuilder builder = this.getEntityBuilder();
         return builder.createWebhook(object);
      });
   }

   @Nonnull
   public RestAction<ApplicationInfo> retrieveApplicationInfo() {
      AccountTypeException.check(this.getAccountType(), AccountType.BOT);
      Route.CompiledRoute route = Route.Applications.GET_BOT_APPLICATION.compile();
      return new RestActionImpl(this, route, (response, request) -> {
         ApplicationInfo info = this.getEntityBuilder().createApplicationInfo(response.getObject());
         this.clientId = info.getId();
         return info;
      });
   }

   @Nonnull
   public String getInviteUrl(Permission... permissions) {
      StringBuilder builder = this.buildBaseInviteUrl();
      if (permissions != null && permissions.length > 0) {
         builder.append("&permissions=").append(Permission.getRaw(permissions));
      }

      return builder.toString();
   }

   @Nonnull
   public String getInviteUrl(Collection<Permission> permissions) {
      StringBuilder builder = this.buildBaseInviteUrl();
      if (permissions != null && !permissions.isEmpty()) {
         builder.append("&permissions=").append(Permission.getRaw(permissions));
      }

      return builder.toString();
   }

   private StringBuilder buildBaseInviteUrl() {
      if (this.clientId == null) {
         this.retrieveApplicationInfo().complete();
      }

      StringBuilder builder = new StringBuilder("https://discord.com/oauth2/authorize?scope=bot&client_id=");
      builder.append(this.clientId);
      return builder;
   }

   public void setShardManager(ShardManager shardManager) {
      this.shardManager = shardManager;
   }

   public ShardManager getShardManager() {
      return this.shardManager;
   }

   public EntityBuilder getEntityBuilder() {
      return this.entityBuilder;
   }

   public IAudioSendFactory getAudioSendFactory() {
      return this.audioSendFactory;
   }

   public void setAudioSendFactory(IAudioSendFactory factory) {
      Checks.notNull(factory, "Provided IAudioSendFactory");
      this.audioSendFactory = factory;
   }

   public void setGatewayPing(long ping) {
      long oldPing = this.gatewayPing;
      this.gatewayPing = ping;
      this.handleEvent(new GatewayPingEvent(this, oldPing));
   }

   public Requester getRequester() {
      return this.requester;
   }

   public WebSocketFactory getWebSocketFactory() {
      return this.sessionConfig.getWebSocketFactory();
   }

   public WebSocketClient getClient() {
      return this.client;
   }

   public SnowflakeCacheViewImpl<User> getUsersView() {
      return this.userCache;
   }

   public SnowflakeCacheViewImpl<Guild> getGuildsView() {
      return this.guildCache;
   }

   public SnowflakeCacheViewImpl<Category> getCategoriesView() {
      return this.categories;
   }

   public SnowflakeCacheViewImpl<StoreChannel> getStoreChannelsView() {
      return this.storeChannelCache;
   }

   public SnowflakeCacheViewImpl<TextChannel> getTextChannelsView() {
      return this.textChannelCache;
   }

   public SnowflakeCacheViewImpl<VoiceChannel> getVoiceChannelsView() {
      return this.voiceChannelCache;
   }

   public SnowflakeCacheViewImpl<PrivateChannel> getPrivateChannelsView() {
      return this.privateChannelCache;
   }

   public AbstractCacheView<AudioManager> getAudioManagersView() {
      return this.audioManagers;
   }

   public void setSelfUser(SelfUser selfUser) {
      UnlockHook hook = this.userCache.writeLock();

      try {
         this.userCache.getMap().put(selfUser.getIdLong(), selfUser);
      } catch (Throwable var6) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (hook != null) {
         hook.close();
      }

      this.selfUser = selfUser;
   }

   public void setResponseTotal(int responseTotal) {
      this.responseTotal = (long)responseTotal;
   }

   public String getIdentifierString() {
      return this.shardInfo != null ? "JDA " + this.shardInfo.getShardString() : "JDA";
   }

   public EventCache getEventCache() {
      return this.eventCache;
   }

   public String getGatewayUrl() {
      return this.gatewayUrl == null ? (this.gatewayUrl = this.getGateway()) : this.gatewayUrl;
   }

   public void resetGatewayUrl() {
      this.gatewayUrl = null;
   }

   public ScheduledExecutorService getAudioLifeCyclePool() {
      return this.threadConfig.getAudioPool(this::getIdentifierString);
   }
}

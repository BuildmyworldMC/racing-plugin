package net.dv8tion.jda.internal.requests;

import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.hooks.ConnectionListener;
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.CloseCode;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.DataType;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.audio.ConnectionRequest;
import net.dv8tion.jda.internal.audio.ConnectionStage;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.handle.ChannelCreateHandler;
import net.dv8tion.jda.internal.handle.ChannelDeleteHandler;
import net.dv8tion.jda.internal.handle.ChannelUpdateHandler;
import net.dv8tion.jda.internal.handle.GuildBanHandler;
import net.dv8tion.jda.internal.handle.GuildCreateHandler;
import net.dv8tion.jda.internal.handle.GuildDeleteHandler;
import net.dv8tion.jda.internal.handle.GuildEmojisUpdateHandler;
import net.dv8tion.jda.internal.handle.GuildMemberAddHandler;
import net.dv8tion.jda.internal.handle.GuildMemberRemoveHandler;
import net.dv8tion.jda.internal.handle.GuildMemberUpdateHandler;
import net.dv8tion.jda.internal.handle.GuildMembersChunkHandler;
import net.dv8tion.jda.internal.handle.GuildRoleCreateHandler;
import net.dv8tion.jda.internal.handle.GuildRoleDeleteHandler;
import net.dv8tion.jda.internal.handle.GuildRoleUpdateHandler;
import net.dv8tion.jda.internal.handle.GuildSetupController;
import net.dv8tion.jda.internal.handle.GuildSyncHandler;
import net.dv8tion.jda.internal.handle.GuildUpdateHandler;
import net.dv8tion.jda.internal.handle.InviteCreateHandler;
import net.dv8tion.jda.internal.handle.InviteDeleteHandler;
import net.dv8tion.jda.internal.handle.MessageBulkDeleteHandler;
import net.dv8tion.jda.internal.handle.MessageCreateHandler;
import net.dv8tion.jda.internal.handle.MessageDeleteHandler;
import net.dv8tion.jda.internal.handle.MessageReactionBulkRemoveHandler;
import net.dv8tion.jda.internal.handle.MessageReactionClearEmoteHandler;
import net.dv8tion.jda.internal.handle.MessageReactionHandler;
import net.dv8tion.jda.internal.handle.MessageUpdateHandler;
import net.dv8tion.jda.internal.handle.PresenceUpdateHandler;
import net.dv8tion.jda.internal.handle.ReadyHandler;
import net.dv8tion.jda.internal.handle.SocketHandler;
import net.dv8tion.jda.internal.handle.TypingStartHandler;
import net.dv8tion.jda.internal.handle.UserUpdateHandler;
import net.dv8tion.jda.internal.handle.VoiceServerUpdateHandler;
import net.dv8tion.jda.internal.handle.VoiceStateUpdateHandler;
import net.dv8tion.jda.internal.managers.AudioManagerImpl;
import net.dv8tion.jda.internal.managers.PresenceImpl;
import net.dv8tion.jda.internal.utils.IOUtil;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.dv8tion.jda.internal.utils.UnlockHook;
import net.dv8tion.jda.internal.utils.cache.AbstractCacheView;
import net.dv8tion.jda.internal.utils.compress.Decompressor;
import net.dv8tion.jda.internal.utils.compress.ZlibDecompressor;
import org.slf4j.Logger;
import org.slf4j.MDC;

public class WebSocketClient extends WebSocketAdapter implements WebSocketListener {
   public static final ThreadLocal<Boolean> WS_THREAD = ThreadLocal.withInitial(() -> {
      return false;
   });
   public static final Logger LOG = JDALogger.getLog(WebSocketClient.class);
   public static final int IDENTIFY_DELAY = 5;
   public static final int ZLIB_SUFFIX = 65535;
   protected static final String INVALIDATE_REASON = "INVALIDATE_SESSION";
   protected static final long IDENTIFY_BACKOFF;
   protected final JDAImpl api;
   protected final JDA.ShardInfo shardInfo;
   protected final Map<String, SocketHandler> handlers = new HashMap();
   protected final Compression compression;
   protected final int gatewayIntents;
   protected final MemberChunkManager chunkManager;
   protected final GatewayEncoding encoding;
   public WebSocket socket;
   protected volatile String sessionId = null;
   protected final Object readLock = new Object();
   protected Decompressor decompressor;
   protected final ReentrantLock queueLock = new ReentrantLock();
   protected final ScheduledExecutorService executor;
   protected WebSocketSendingThread ratelimitThread;
   protected volatile Future<?> keepAliveThread;
   protected boolean initiating;
   protected int missedHeartbeats = 0;
   protected int reconnectTimeoutS = 2;
   protected long heartbeatStartTime;
   protected long identifyTime = 0L;
   protected final TLongObjectMap<ConnectionRequest> queuedAudioConnections = MiscUtil.newLongMap();
   protected final Queue<DataObject> chunkSyncQueue = new ConcurrentLinkedQueue();
   protected final Queue<DataObject> ratelimitQueue = new ConcurrentLinkedQueue();
   protected volatile long ratelimitResetTime;
   protected final AtomicInteger messagesSent = new AtomicInteger(0);
   protected volatile boolean shutdown = false;
   protected boolean shouldReconnect;
   protected boolean handleIdentifyRateLimit = false;
   protected boolean connected = false;
   protected volatile boolean printedRateLimitMessage = false;
   protected volatile boolean sentAuthInfo = false;
   protected boolean firstInit = true;
   protected boolean processingReady = true;
   protected volatile WebSocketClient.ConnectNode connectNode;

   public WebSocketClient(JDAImpl api, Compression compression, int gatewayIntents, GatewayEncoding encoding) {
      this.api = api;
      this.executor = api.getGatewayPool();
      this.shardInfo = api.getShardInfo();
      this.compression = compression;
      this.gatewayIntents = gatewayIntents;
      this.chunkManager = new MemberChunkManager(this);
      this.encoding = encoding;
      this.shouldReconnect = api.isAutoReconnect();
      this.connectNode = new WebSocketClient.StartingNode();
      this.setupHandlers();

      try {
         api.getSessionController().appendSession(this.connectNode);
      } catch (Error | RuntimeException var6) {
         LOG.error((String)"Failed to append new session to session controller queue. Shutting down!", (Throwable)var6);
         this.api.setStatus(JDA.Status.SHUTDOWN);
         this.api.handleEvent(new ShutdownEvent(api, OffsetDateTime.now(), 1006));
         if (var6 instanceof RuntimeException) {
            throw (RuntimeException)var6;
         } else {
            throw (Error)var6;
         }
      }
   }

   public JDA getJDA() {
      return this.api;
   }

   public void setAutoReconnect(boolean reconnect) {
      this.shouldReconnect = reconnect;
   }

   public boolean isConnected() {
      return this.connected;
   }

   public int getGatewayIntents() {
      return this.gatewayIntents;
   }

   public MemberChunkManager getChunkManager() {
      return this.chunkManager;
   }

   public void ready() {
      if (this.initiating) {
         this.initiating = false;
         this.processingReady = false;
         if (this.firstInit) {
            this.firstInit = false;
            if (this.api.getGuilds().size() >= 2000) {
               JDAImpl.LOG.warn(" __      __ _    ___  _  _  ___  _  _   ___  _ ");
               JDAImpl.LOG.warn(" \\ \\    / //_\\  | _ \\| \\| ||_ _|| \\| | / __|| |");
               JDAImpl.LOG.warn("  \\ \\/\\/ // _ \\ |   /| .` | | | | .` || (_ ||_|");
               JDAImpl.LOG.warn("   \\_/\\_//_/ \\_\\|_|_\\|_|\\_||___||_|\\_| \\___|(_)");
               JDAImpl.LOG.warn("You're running a session with over 2000 connected");
               JDAImpl.LOG.warn("guilds. You should shard the connection in order");
               JDAImpl.LOG.warn("to split the load or things like resuming");
               JDAImpl.LOG.warn("connection might not work as expected.");
               JDAImpl.LOG.warn("For more info see https://git.io/vrFWP");
            }

            JDAImpl.LOG.info("Finished Loading!");
            this.api.handleEvent(new ReadyEvent(this.api, this.api.getResponseTotal()));
         } else {
            this.updateAudioManagerReferences();
            JDAImpl.LOG.info("Finished (Re)Loading!");
            this.api.handleEvent(new ReconnectedEvent(this.api, this.api.getResponseTotal()));
         }
      } else {
         JDAImpl.LOG.debug("Successfully resumed Session!");
         this.api.handleEvent(new ResumedEvent(this.api, this.api.getResponseTotal()));
      }

      this.api.setStatus(JDA.Status.CONNECTED);
   }

   public boolean isReady() {
      return !this.initiating;
   }

   public boolean isSession() {
      return this.sessionId != null;
   }

   public void handle(List<DataObject> events) {
      events.forEach(this::onDispatch);
   }

   public void send(DataObject message) {
      this.locked("Interrupted while trying to add request to queue", () -> {
         return this.ratelimitQueue.add(message);
      });
   }

   public void cancelChunkRequest(String nonce) {
      this.locked("Interrupted while trying to cancel chunk request", () -> {
         return this.chunkSyncQueue.removeIf((it) -> {
            return it.getString("nonce", "").equals(nonce);
         });
      });
   }

   public void sendChunkRequest(DataObject request) {
      this.locked("Interrupted while trying to add chunk request", () -> {
         return this.chunkSyncQueue.add(request);
      });
   }

   protected boolean send(DataObject message, boolean skipQueue) {
      if (!this.connected) {
         return false;
      } else {
         long now = System.currentTimeMillis();
         if (this.ratelimitResetTime <= now) {
            this.messagesSent.set(0);
            this.ratelimitResetTime = now + 60000L;
            this.printedRateLimitMessage = false;
         }

         if (this.messagesSent.get() <= 115 || skipQueue && this.messagesSent.get() <= 119) {
            LOG.trace((String)"<- {}", (Object)message);
            if (this.encoding == GatewayEncoding.ETF) {
               this.socket.sendBinary(message.toETF());
            } else {
               this.socket.sendText(message.toString());
            }

            this.messagesSent.getAndIncrement();
            return true;
         } else {
            if (!this.printedRateLimitMessage) {
               LOG.warn("Hit the WebSocket RateLimit! This can be caused by too many presence or voice status updates (connect/disconnect/mute/deaf). Regular: {} Voice: {} Chunking: {}", this.ratelimitQueue.size(), this.queuedAudioConnections.size(), this.chunkSyncQueue.size());
               this.printedRateLimitMessage = true;
            }

            return false;
         }
      }
   }

   protected void setupSendingThread() {
      this.ratelimitThread = new WebSocketSendingThread(this);
      this.ratelimitThread.start();
   }

   private void prepareClose() {
      try {
         if (this.socket != null) {
            Socket rawSocket = this.socket.getSocket();
            if (rawSocket != null) {
               rawSocket.setSoTimeout(10000);
            }
         }
      } catch (SocketException var2) {
      }

   }

   public void close() {
      this.prepareClose();
      if (this.socket != null) {
         this.socket.sendClose(1000);
      }

   }

   public void close(int code) {
      this.prepareClose();
      if (this.socket != null) {
         this.socket.sendClose(code);
      }

   }

   public void close(int code, String reason) {
      this.prepareClose();
      if (this.socket != null) {
         this.socket.sendClose(code, reason);
      }

   }

   public synchronized void shutdown() {
      this.shutdown = true;
      this.shouldReconnect = false;
      if (this.connectNode != null) {
         this.api.getSessionController().removeSession(this.connectNode);
      }

      this.close(1000, "Shutting down");
   }

   protected synchronized void connect() {
      if (this.api.getStatus() != JDA.Status.ATTEMPTING_TO_RECONNECT) {
         this.api.setStatus(JDA.Status.CONNECTING_TO_WEBSOCKET);
      }

      if (this.shutdown) {
         throw new RejectedExecutionException("JDA is shutdown!");
      } else {
         this.initiating = true;
         String url = this.api.getGatewayUrl() + "?encoding=" + this.encoding.name().toLowerCase() + "&v=" + 6;
         if (this.compression != Compression.NONE) {
            url = url + "&compress=" + this.compression.getKey();
            switch(this.compression) {
            case ZLIB:
               if (this.decompressor == null || this.decompressor.getType() != Compression.ZLIB) {
                  this.decompressor = new ZlibDecompressor(this.api.getMaxBufferSize());
               }
               break;
            default:
               throw new IllegalStateException("Unknown compression");
            }
         }

         try {
            WebSocketFactory socketFactory = new WebSocketFactory(this.api.getWebSocketFactory());
            IOUtil.setServerName(socketFactory, url);
            this.socket = socketFactory.createSocket(url);
            this.socket.setDirectTextMessage(true);
            this.socket.addHeader("Accept-Encoding", "gzip").addListener(this).connect();
         } catch (WebSocketException | IOException var3) {
            this.api.resetGatewayUrl();
            throw new IllegalStateException(var3);
         }
      }
   }

   public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
      this.api.setContext();
   }

   public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
      this.prepareClose();
      this.api.setStatus(JDA.Status.IDENTIFYING_SESSION);
      if (this.sessionId == null) {
         LOG.info("Connected to WebSocket");
         if (this.api.useIntents()) {
            LOG.debug((String)"Connected with gateway intents: {}", (Object)Integer.toBinaryString(this.gatewayIntents));
         }
      } else {
         LOG.debug("Connected to WebSocket");
      }

      this.connected = true;
      this.messagesSent.set(0);
      this.ratelimitResetTime = System.currentTimeMillis() + 60000L;
      if (this.sessionId == null) {
         this.sendIdentify();
      } else {
         this.sendResume();
      }

   }

   public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
      this.sentAuthInfo = false;
      this.connected = false;
      if (Thread.currentThread().isInterrupted()) {
         Thread thread = new Thread(() -> {
            this.handleDisconnect(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
         });
         thread.setName(this.api.getIdentifierString() + " MainWS-ReconnectThread");
         thread.start();
      } else {
         this.handleDisconnect(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
      }

   }

   private void handleDisconnect(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
      this.api.setStatus(JDA.Status.DISCONNECTED);
      CloseCode closeCode = null;
      int rawCloseCode = 1005;
      boolean isInvalidate = false;
      if (this.keepAliveThread != null) {
         this.keepAliveThread.cancel(false);
         this.keepAliveThread = null;
      }

      if (closedByServer && serverCloseFrame != null) {
         rawCloseCode = serverCloseFrame.getCloseCode();
         String rawCloseReason = serverCloseFrame.getCloseReason();
         closeCode = CloseCode.from(rawCloseCode);
         if (closeCode == CloseCode.RATE_LIMITED) {
            LOG.error("WebSocket connection closed due to ratelimit! Sent more than 120 websocket messages in under 60 seconds!");
         } else if (closeCode == CloseCode.UNKNOWN_ERROR) {
            LOG.error((String)"WebSocket connection closed due to server error! {}: {}", (Object)rawCloseCode, (Object)rawCloseReason);
         } else if (closeCode != null) {
            LOG.debug((String)"WebSocket connection closed with code {}", (Object)closeCode);
         } else if (rawCloseReason != null) {
            LOG.warn((String)"WebSocket connection closed with code {}: {}", (Object)rawCloseCode, (Object)rawCloseReason);
         } else {
            LOG.warn((String)"WebSocket connection closed with unknown meaning for close-code {}", (Object)rawCloseCode);
         }
      } else if (clientCloseFrame != null) {
         rawCloseCode = clientCloseFrame.getCloseCode();
         if (rawCloseCode == 1000 && "INVALIDATE_SESSION".equals(clientCloseFrame.getCloseReason())) {
            isInvalidate = true;
         }
      }

      boolean closeCodeIsReconnect = closeCode == null || closeCode.isReconnect();
      if (this.shouldReconnect && closeCodeIsReconnect && !this.executor.isShutdown()) {
         synchronized(this.readLock) {
            if (this.decompressor != null) {
               this.decompressor.reset();
            }
         }

         if (isInvalidate) {
            this.invalidate();
         }

         this.api.handleEvent(new DisconnectEvent(this.api, serverCloseFrame, clientCloseFrame, closedByServer, OffsetDateTime.now()));

         try {
            this.handleReconnect(rawCloseCode);
         } catch (InterruptedException var11) {
            LOG.error((String)"Failed to resume due to interrupted thread", (Throwable)var11);
            this.invalidate();
            this.queueReconnect();
         }
      } else {
         if (this.ratelimitThread != null) {
            this.ratelimitThread.shutdown();
            this.ratelimitThread = null;
         }

         if (!closeCodeIsReconnect) {
            LOG.error((String)"WebSocket connection was closed and cannot be recovered due to identification issues\n{}", (Object)closeCode);
         }

         if (this.decompressor != null) {
            this.decompressor.shutdown();
         }

         this.api.shutdownInternals();
         this.api.handleEvent(new ShutdownEvent(this.api, OffsetDateTime.now(), rawCloseCode));
      }

   }

   private void handleReconnect(int code) throws InterruptedException {
      if (this.sessionId == null) {
         if (this.handleIdentifyRateLimit) {
            long backoff = this.calculateIdentifyBackoff();
            if (backoff > 0L) {
               LOG.error((String)"Encountered IDENTIFY Rate Limit! Waiting {} milliseconds before trying again!", (Object)backoff);
               Thread.sleep(backoff);
            } else {
               LOG.error("Encountered IDENTIFY Rate Limit!");
            }
         }

         LOG.warn((String)"Got disconnected from WebSocket (Code {}). Appending to reconnect queue", (Object)code);
         this.queueReconnect();
      } else {
         LOG.debug((String)"Got disconnected from WebSocket (Code: {}). Attempting to resume session", (Object)code);
         this.reconnect();
      }

   }

   protected long calculateIdentifyBackoff() {
      long currentTime = System.currentTimeMillis();
      return currentTime - (this.identifyTime + IDENTIFY_BACKOFF);
   }

   protected void queueReconnect() {
      try {
         this.api.setStatus(JDA.Status.RECONNECT_QUEUED);
         this.connectNode = new WebSocketClient.ReconnectNode();
         this.api.getSessionController().appendSession(this.connectNode);
      } catch (IllegalStateException var2) {
         LOG.error("Reconnect queue rejected session. Shutting down...");
         this.api.setStatus(JDA.Status.SHUTDOWN);
         this.api.handleEvent(new ShutdownEvent(this.api, OffsetDateTime.now(), 1006));
      }

   }

   protected void reconnect() throws InterruptedException {
      this.reconnect(false);
   }

   public void reconnect(boolean callFromQueue) throws InterruptedException {
      Set<MDC.MDCCloseable> contextEntries = null;
      Map<String, String> previousContext = null;
      ConcurrentMap<String, String> contextMap = this.api.getContextMap();
      if (callFromQueue && contextMap != null) {
         previousContext = MDC.getCopyOfContextMap();
         contextEntries = (Set)contextMap.entrySet().stream().map((entry) -> {
            return MDC.putCloseable((String)entry.getKey(), (String)entry.getValue());
         }).collect(Collectors.toSet());
      }

      if (this.shutdown) {
         this.api.setStatus(JDA.Status.SHUTDOWN);
         this.api.handleEvent(new ShutdownEvent(this.api, OffsetDateTime.now(), 1000));
      } else {
         String message = "";
         if (callFromQueue) {
            message = String.format("Queue is attempting to reconnect a shard...%s ", this.shardInfo != null ? " Shard: " + this.shardInfo.getShardString() : "");
         }

         if (this.sessionId != null) {
            this.reconnectTimeoutS = 0;
         }

         LOG.debug((String)"{}Attempting to reconnect in {}s", (Object)message, (Object)this.reconnectTimeoutS);

         while(this.shouldReconnect) {
            this.api.setStatus(JDA.Status.WAITING_TO_RECONNECT);
            int delay = this.reconnectTimeoutS;
            this.reconnectTimeoutS = this.reconnectTimeoutS == 0 ? 2 : Math.min(this.reconnectTimeoutS << 1, this.api.getMaxReconnectDelay());
            Thread.sleep((long)(delay * 1000));
            this.handleIdentifyRateLimit = false;
            this.api.setStatus(JDA.Status.ATTEMPTING_TO_RECONNECT);
            LOG.debug("Attempting to reconnect!");

            try {
               this.connect();
               break;
            } catch (RejectedExecutionException var7) {
               this.api.setStatus(JDA.Status.SHUTDOWN);
               this.api.handleEvent(new ShutdownEvent(this.api, OffsetDateTime.now(), 1000));
               return;
            } catch (RuntimeException var8) {
               LOG.warn((String)"Reconnect failed! Next attempt in {}s", (Object)this.reconnectTimeoutS);
            }
         }

         if (contextEntries != null) {
            contextEntries.forEach(MDC.MDCCloseable::close);
         }

         if (previousContext != null) {
            previousContext.forEach(MDC::put);
         }

      }
   }

   protected void setupKeepAlive(int timeout) {
      try {
         Socket rawSocket = this.socket.getSocket();
         if (rawSocket != null) {
            rawSocket.setSoTimeout(timeout + 10000);
         }
      } catch (SocketException var3) {
         LOG.warn((String)"Failed to setup timeout for socket", (Throwable)var3);
      }

      this.keepAliveThread = this.executor.scheduleAtFixedRate(() -> {
         this.api.setContext();
         if (this.connected) {
            this.sendKeepAlive();
         }

      }, 0L, (long)timeout, TimeUnit.MILLISECONDS);
   }

   protected void sendKeepAlive() {
      DataObject keepAlivePacket = DataObject.empty().put("op", 1).put("d", this.api.getResponseTotal());
      if (this.missedHeartbeats >= 2) {
         this.missedHeartbeats = 0;
         LOG.warn("Missed 2 heartbeats! Trying to reconnect...");
         this.prepareClose();
         this.socket.disconnect(4900, "ZOMBIE CONNECTION");
      } else {
         ++this.missedHeartbeats;
         this.send(keepAlivePacket, true);
         this.heartbeatStartTime = System.currentTimeMillis();
      }

   }

   protected void sendIdentify() {
      LOG.debug("Sending Identify-packet...");
      PresenceImpl presenceObj = (PresenceImpl)this.api.getPresence();
      DataObject connectionProperties = DataObject.empty().put("$os", System.getProperty("os.name")).put("$browser", "JDA").put("$device", "JDA").put("$referring_domain", "").put("$referrer", "");
      DataObject payload = DataObject.empty().put("presence", presenceObj.getFullPresence()).put("token", this.getToken()).put("properties", connectionProperties).put("v", 6).put("large_threshold", this.api.getLargeThreshold());
      if (this.api.useIntents()) {
         payload.put("intents", this.gatewayIntents);
      }

      DataObject identify = DataObject.empty().put("op", 2).put("d", payload);
      if (this.shardInfo != null) {
         payload.put("shard", DataArray.empty().add(this.shardInfo.getShardId()).add(this.shardInfo.getShardTotal()));
      }

      this.send(identify, true);
      this.handleIdentifyRateLimit = true;
      this.identifyTime = System.currentTimeMillis();
      this.sentAuthInfo = true;
      this.api.setStatus(JDA.Status.AWAITING_LOGIN_CONFIRMATION);
   }

   protected void sendResume() {
      LOG.debug("Sending Resume-packet...");
      DataObject resume = DataObject.empty().put("op", 6).put("d", DataObject.empty().put("session_id", this.sessionId).put("token", this.getToken()).put("seq", this.api.getResponseTotal()));
      this.send(resume, true);
      this.api.setStatus(JDA.Status.AWAITING_LOGIN_CONFIRMATION);
   }

   protected void invalidate() {
      this.sessionId = null;
      this.sentAuthInfo = false;
      Queue var10002 = this.chunkSyncQueue;
      Objects.requireNonNull(var10002);
      this.locked("Interrupted while trying to invalidate chunk/sync queue", var10002::clear);
      this.api.getTextChannelsView().clear();
      this.api.getVoiceChannelsView().clear();
      this.api.getStoreChannelsView().clear();
      this.api.getCategoriesView().clear();
      this.api.getGuildsView().clear();
      this.api.getUsersView().clear();
      this.api.getPrivateChannelsView().clear();
      this.api.getEventCache().clear();
      this.api.getGuildSetupController().clearCache();
      this.chunkManager.clear();
   }

   protected void updateAudioManagerReferences() {
      AbstractCacheView<AudioManager> managerView = this.api.getAudioManagersView();
      UnlockHook hook = managerView.writeLock();

      try {
         TLongObjectMap<AudioManager> managerMap = managerView.getMap();
         if (managerMap.size() > 0) {
            LOG.trace("Updating AudioManager references");
         }

         TLongObjectIterator it = managerMap.iterator();

         while(it.hasNext()) {
            it.advance();
            long guildId = it.key();
            AudioManagerImpl mng = (AudioManagerImpl)it.value();
            GuildImpl guild = (GuildImpl)this.api.getGuildById(guildId);
            if (guild == null) {
               this.queuedAudioConnections.remove(guildId);
               mng.closeAudioConnection(ConnectionStatus.DISCONNECTED_REMOVED_DURING_RECONNECT);
               it.remove();
            }
         }
      } catch (Throwable var10) {
         if (hook != null) {
            try {
               hook.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (hook != null) {
         hook.close();
      }

   }

   protected String getToken() {
      return this.api.getAccountType() == AccountType.BOT ? this.api.getToken().substring("Bot ".length()) : this.api.getToken();
   }

   protected List<DataObject> convertPresencesReplace(long responseTotal, DataArray array) {
      List<DataObject> output = new LinkedList();

      for(int i = 0; i < array.length(); ++i) {
         DataObject presence = array.getObject(i);
         DataObject obj = DataObject.empty();
         obj.put("comment", "This was constructed from a PRESENCES_REPLACE payload").put("op", 0).put("s", responseTotal).put("d", presence).put("t", "PRESENCE_UPDATE");
         output.add(obj);
      }

      return output;
   }

   protected void handleEvent(DataObject content) {
      try {
         this.onEvent(content);
      } catch (Exception var3) {
         LOG.error((String)"Encountered exception on lifecycle level\nJSON: {}", (Object)content, (Object)var3);
         this.api.handleEvent(new ExceptionEvent(this.api, var3, true));
      }

   }

   protected void onEvent(DataObject content) {
      WS_THREAD.set(true);
      int opCode = content.getInt("op");
      if (!content.isNull("s")) {
         this.api.setResponseTotal(content.getInt("s"));
      }

      switch(opCode) {
      case 0:
         this.onDispatch(content);
         break;
      case 1:
         LOG.debug("Got Keep-Alive request (OP 1). Sending response...");
         this.sendKeepAlive();
         break;
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 8:
      default:
         LOG.debug((String)"Got unknown op-code: {} with content: {}", (Object)opCode, (Object)content);
         break;
      case 7:
         LOG.debug("Got Reconnect request (OP 7). Closing connection now...");
         this.close(4900, "OP 7: RECONNECT");
         break;
      case 9:
         LOG.debug("Got Invalidate request (OP 9). Invalidating...");
         this.handleIdentifyRateLimit = this.handleIdentifyRateLimit && System.currentTimeMillis() - this.identifyTime < IDENTIFY_BACKOFF;
         this.sentAuthInfo = false;
         boolean isResume = content.getBoolean("d");
         int closeCode = isResume ? 4900 : 1000;
         if (isResume) {
            LOG.debug("Session can be recovered... Closing and sending new RESUME request");
         } else {
            this.invalidate();
         }

         this.close(closeCode, "INVALIDATE_SESSION");
         break;
      case 10:
         LOG.debug("Got HELLO packet (OP 10). Initializing keep-alive.");
         DataObject data = content.getObject("d");
         this.setupKeepAlive(data.getInt("heartbeat_interval"));
         break;
      case 11:
         LOG.trace("Got Heartbeat Ack (OP 11).");
         this.missedHeartbeats = 0;
         this.api.setGatewayPing(System.currentTimeMillis() - this.heartbeatStartTime);
      }

   }

   protected void onDispatch(DataObject raw) {
      String type = raw.getString("t");
      long responseTotal = this.api.getResponseTotal();
      if (!raw.isType("d", DataType.OBJECT)) {
         if (type.equals("PRESENCES_REPLACE")) {
            DataArray payload = raw.getArray("d");
            List<DataObject> converted = this.convertPresencesReplace(responseTotal, payload);
            SocketHandler handler = this.getHandler("PRESENCE_UPDATE");
            LOG.trace((String)"{} -> {}", (Object)type, (Object)payload);
            Iterator var16 = converted.iterator();

            while(var16.hasNext()) {
               DataObject o = (DataObject)var16.next();
               handler.handle(responseTotal, o);
               if (this.api.isRawEvents()) {
                  this.api.handleEvent(new RawGatewayEvent(this.api, responseTotal, o));
               }
            }
         } else {
            LOG.debug((String)"Received event with unhandled body type JSON: {}", (Object)raw);
         }

      } else {
         DataObject content = raw.getObject("d");
         LOG.trace((String)"{} -> {}", (Object)type, (Object)content);
         JDAImpl jda = (JDAImpl)this.getJDA();

         try {
            byte var8 = -1;
            switch(type.hashCode()) {
            case 77848963:
               if (type.equals("READY")) {
                  var8 = 0;
               }
               break;
            case 1815529911:
               if (type.equals("RESUMED")) {
                  var8 = 1;
               }
            }

            switch(var8) {
            case 0:
               this.reconnectTimeoutS = 2;
               this.api.setStatus(JDA.Status.LOADING_SUBSYSTEMS);
               this.processingReady = true;
               this.handleIdentifyRateLimit = false;
               ((SocketHandler)this.handlers.get("READY")).handle(responseTotal, raw);
               this.sessionId = content.getString("session_id");
               break;
            case 1:
               this.reconnectTimeoutS = 2;
               this.sentAuthInfo = true;
               if (!this.processingReady) {
                  this.initiating = false;
                  this.ready();
               } else {
                  LOG.debug("Resumed while still processing initial ready");
                  jda.setStatus(JDA.Status.LOADING_SUBSYSTEMS);
               }
               break;
            default:
               long guildId = content.getLong("guild_id", 0L);
               if (this.api.isUnavailable(guildId) && !type.equals("GUILD_CREATE") && !type.equals("GUILD_DELETE")) {
                  LOG.debug("Ignoring {} for unavailable guild with id {}. JSON: {}", type, guildId, content);
               } else {
                  SocketHandler handler = (SocketHandler)this.handlers.get(type);
                  if (handler != null) {
                     handler.handle(responseTotal, raw);
                  } else {
                     LOG.debug((String)"Unrecognized event:\n{}", (Object)raw);
                  }
               }
            }

            if (this.api.isRawEvents()) {
               this.api.handleEvent(new RawGatewayEvent(this.api, responseTotal, raw));
            }
         } catch (ParsingException var12) {
            LOG.warn("Got an unexpected Json-parse error. Please redirect following message to the devs:\n\t{}\n\t{} -> {}", var12.getMessage(), type, content, var12);
         } catch (Exception var13) {
            LOG.error("Got an unexpected error. Please redirect following message to the devs:\n\t{} -> {}", type, content, var13);
         }

         if (responseTotal % 100L == 0L) {
            jda.getEventCache().timeout(responseTotal);
         }

      }
   }

   public void onTextMessage(WebSocket websocket, byte[] data) {
      this.handleEvent(DataObject.fromJson(data));
   }

   public void onBinaryMessage(WebSocket websocket, byte[] binary) throws DataFormatException {
      DataObject message;
      synchronized(this.readLock) {
         message = this.handleBinary(binary);
      }

      if (message != null) {
         this.handleEvent(message);
      }

   }

   protected DataObject handleBinary(byte[] binary) throws DataFormatException {
      if (this.decompressor == null) {
         if (this.encoding == GatewayEncoding.ETF) {
            return DataObject.fromETF(binary);
         } else {
            throw new IllegalStateException("Cannot decompress binary message due to unknown compression algorithm: " + this.compression);
         }
      } else {
         byte[] data;
         try {
            data = this.decompressor.decompress(binary);
            if (data == null) {
               return null;
            }
         } catch (DataFormatException var8) {
            this.close(4900, "MALFORMED_PACKAGE");
            throw var8;
         }

         try {
            return this.encoding == GatewayEncoding.ETF ? DataObject.fromETF(data) : DataObject.fromJson(data);
         } catch (ParsingException var7) {
            String jsonString = "malformed";

            try {
               jsonString = new String(data, StandardCharsets.UTF_8);
            } catch (Exception var6) {
            }

            LOG.error((String)"Failed to parse json: {}", (Object)jsonString);
            throw var7;
         }
      }
   }

   public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
      if (cause.getCause() instanceof SocketTimeoutException) {
         LOG.debug("Socket timed out");
      } else if (cause.getCause() instanceof IOException) {
         LOG.debug((String)"Encountered I/O error", (Throwable)cause);
      } else {
         LOG.error((String)"There was an error in the WebSocket connection", (Throwable)cause);
         this.api.handleEvent(new ExceptionEvent(this.api, cause, true));
      }

   }

   public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
      String identifier = this.api.getIdentifierString();
      switch(threadType) {
      case CONNECT_THREAD:
         thread.setName(identifier + " MainWS-ConnectThread");
         break;
      case FINISH_THREAD:
         thread.setName(identifier + " MainWS-FinishThread");
         break;
      case READING_THREAD:
         thread.setName(identifier + " MainWS-ReadThread");
         break;
      case WRITING_THREAD:
         thread.setName(identifier + " MainWS-WriteThread");
         break;
      default:
         thread.setName(identifier + " MainWS-" + threadType);
      }

   }

   protected void maybeUnlock() {
      if (this.queueLock.isHeldByCurrentThread()) {
         this.queueLock.unlock();
      }

   }

   protected void locked(String comment, Runnable task) {
      try {
         if (!this.queueLock.tryLock() && !this.queueLock.tryLock(10L, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Could not acquire lock in reasonable timeframe! (10 seconds)");
         }

         task.run();
      } catch (InterruptedException var7) {
         LOG.error((String)comment, (Throwable)var7);
      } finally {
         this.maybeUnlock();
      }

   }

   protected <T> T locked(String comment, Supplier<T> task) {
      Object var4;
      try {
         if (!this.queueLock.tryLock() && !this.queueLock.tryLock(10L, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Could not acquire lock in reasonable timeframe! (10 seconds)");
         }

         Object var3 = task.get();
         return var3;
      } catch (InterruptedException var8) {
         LOG.error((String)comment, (Throwable)var8);
         var4 = null;
      } finally {
         this.maybeUnlock();
      }

      return var4;
   }

   public void queueAudioReconnect(VoiceChannel channel) {
      this.locked("There was an error queueing the audio reconnect", () -> {
         long guildId = channel.getGuild().getIdLong();
         ConnectionRequest request = (ConnectionRequest)this.queuedAudioConnections.get(guildId);
         if (request == null) {
            request = new ConnectionRequest(channel, ConnectionStage.RECONNECT);
            this.queuedAudioConnections.put(guildId, request);
         } else {
            request.setStage(ConnectionStage.RECONNECT);
         }

         request.setChannel(channel);
      });
   }

   public void queueAudioConnect(VoiceChannel channel) {
      this.locked("There was an error queueing the audio connect", () -> {
         long guildId = channel.getGuild().getIdLong();
         ConnectionRequest request = (ConnectionRequest)this.queuedAudioConnections.get(guildId);
         if (request == null) {
            request = new ConnectionRequest(channel, ConnectionStage.CONNECT);
            this.queuedAudioConnections.put(guildId, request);
         } else if (request.getStage() == ConnectionStage.DISCONNECT) {
            request.setStage(ConnectionStage.RECONNECT);
         }

         request.setChannel(channel);
      });
   }

   public void queueAudioDisconnect(Guild guild) {
      this.locked("There was an error queueing the audio disconnect", () -> {
         long guildId = guild.getIdLong();
         ConnectionRequest request = (ConnectionRequest)this.queuedAudioConnections.get(guildId);
         if (request == null) {
            this.queuedAudioConnections.put(guildId, new ConnectionRequest(guild));
         } else {
            request.setStage(ConnectionStage.DISCONNECT);
         }

      });
   }

   public ConnectionRequest removeAudioConnection(long guildId) {
      return (ConnectionRequest)this.locked("There was an error cleaning up audio connections for deleted guild", () -> {
         return (ConnectionRequest)this.queuedAudioConnections.remove(guildId);
      });
   }

   public ConnectionRequest updateAudioConnection(long guildId, VoiceChannel connectedChannel) {
      return (ConnectionRequest)this.locked("There was an error updating the audio connection", () -> {
         return this.updateAudioConnection0(guildId, connectedChannel);
      });
   }

   public ConnectionRequest updateAudioConnection0(long guildId, VoiceChannel connectedChannel) {
      ConnectionRequest request = (ConnectionRequest)this.queuedAudioConnections.get(guildId);
      if (request == null) {
         return null;
      } else {
         ConnectionStage requestStage = request.getStage();
         if (connectedChannel == null) {
            switch(requestStage) {
            case DISCONNECT:
               return (ConnectionRequest)this.queuedAudioConnections.remove(guildId);
            case RECONNECT:
               request.setStage(ConnectionStage.CONNECT);
               request.setNextAttemptEpoch(System.currentTimeMillis());
            default:
               return null;
            }
         } else {
            return requestStage == ConnectionStage.CONNECT && request.getChannelId() == connectedChannel.getIdLong() ? (ConnectionRequest)this.queuedAudioConnections.remove(guildId) : null;
         }
      }
   }

   private SoftReference<ByteArrayOutputStream> newDecompressBuffer() {
      return new SoftReference(new ByteArrayOutputStream(1024));
   }

   protected ConnectionRequest getNextAudioConnectRequest() {
      if (this.sessionId == null) {
         return null;
      } else {
         long now = System.currentTimeMillis();
         AtomicReference<ConnectionRequest> request = new AtomicReference();
         this.queuedAudioConnections.retainEntries((guildId, audioRequest) -> {
            if (audioRequest.getNextAttemptEpoch() < now) {
               Guild guild = this.api.getGuildById(guildId);
               if (guild == null) {
                  GuildSetupController controller = this.api.getGuildSetupController();
                  if (!controller.isKnown(guildId)) {
                     LOG.debug((String)"Removing audio connection request because the guild has been removed. {}", (Object)audioRequest);
                     return false;
                  }

                  return true;
               }

               ConnectionListener listener = guild.getAudioManager().getConnectionListener();
               if (audioRequest.getStage() != ConnectionStage.DISCONNECT) {
                  VoiceChannel channel = guild.getVoiceChannelById(audioRequest.getChannelId());
                  if (channel == null) {
                     if (listener != null) {
                        listener.onStatusChange(ConnectionStatus.DISCONNECTED_CHANNEL_DELETED);
                     }

                     return false;
                  }

                  if (!guild.getSelfMember().hasPermission(channel, new Permission[]{Permission.VOICE_CONNECT})) {
                     if (listener != null) {
                        listener.onStatusChange(ConnectionStatus.DISCONNECTED_LOST_PERMISSION);
                     }

                     return false;
                  }
               }

               request.compareAndSet((Object)null, audioRequest);
            }

            return true;
         });
         return (ConnectionRequest)request.get();
      }
   }

   public Map<String, SocketHandler> getHandlers() {
      return this.handlers;
   }

   public <T extends SocketHandler> T getHandler(String type) {
      try {
         return (SocketHandler)this.handlers.get(type);
      } catch (ClassCastException var3) {
         throw new IllegalStateException(var3);
      }
   }

   protected void setupHandlers() {
      SocketHandler.NOPHandler nopHandler = new SocketHandler.NOPHandler(this.api);
      this.handlers.put("CHANNEL_CREATE", new ChannelCreateHandler(this.api));
      this.handlers.put("CHANNEL_DELETE", new ChannelDeleteHandler(this.api));
      this.handlers.put("CHANNEL_UPDATE", new ChannelUpdateHandler(this.api));
      this.handlers.put("GUILD_BAN_ADD", new GuildBanHandler(this.api, true));
      this.handlers.put("GUILD_BAN_REMOVE", new GuildBanHandler(this.api, false));
      this.handlers.put("GUILD_CREATE", new GuildCreateHandler(this.api));
      this.handlers.put("GUILD_DELETE", new GuildDeleteHandler(this.api));
      this.handlers.put("GUILD_EMOJIS_UPDATE", new GuildEmojisUpdateHandler(this.api));
      this.handlers.put("GUILD_MEMBER_ADD", new GuildMemberAddHandler(this.api));
      this.handlers.put("GUILD_MEMBER_REMOVE", new GuildMemberRemoveHandler(this.api));
      this.handlers.put("GUILD_MEMBER_UPDATE", new GuildMemberUpdateHandler(this.api));
      this.handlers.put("GUILD_MEMBERS_CHUNK", new GuildMembersChunkHandler(this.api));
      this.handlers.put("GUILD_ROLE_CREATE", new GuildRoleCreateHandler(this.api));
      this.handlers.put("GUILD_ROLE_DELETE", new GuildRoleDeleteHandler(this.api));
      this.handlers.put("GUILD_ROLE_UPDATE", new GuildRoleUpdateHandler(this.api));
      this.handlers.put("GUILD_SYNC", new GuildSyncHandler(this.api));
      this.handlers.put("GUILD_UPDATE", new GuildUpdateHandler(this.api));
      this.handlers.put("INVITE_CREATE", new InviteCreateHandler(this.api));
      this.handlers.put("INVITE_DELETE", new InviteDeleteHandler(this.api));
      this.handlers.put("MESSAGE_CREATE", new MessageCreateHandler(this.api));
      this.handlers.put("MESSAGE_DELETE", new MessageDeleteHandler(this.api));
      this.handlers.put("MESSAGE_DELETE_BULK", new MessageBulkDeleteHandler(this.api));
      this.handlers.put("MESSAGE_REACTION_ADD", new MessageReactionHandler(this.api, true));
      this.handlers.put("MESSAGE_REACTION_REMOVE", new MessageReactionHandler(this.api, false));
      this.handlers.put("MESSAGE_REACTION_REMOVE_ALL", new MessageReactionBulkRemoveHandler(this.api));
      this.handlers.put("MESSAGE_REACTION_REMOVE_EMOTE", new MessageReactionClearEmoteHandler(this.api));
      this.handlers.put("MESSAGE_UPDATE", new MessageUpdateHandler(this.api));
      this.handlers.put("READY", new ReadyHandler(this.api));
      this.handlers.put("USER_UPDATE", new UserUpdateHandler(this.api));
      this.handlers.put("VOICE_SERVER_UPDATE", new VoiceServerUpdateHandler(this.api));
      this.handlers.put("VOICE_STATE_UPDATE", new VoiceStateUpdateHandler(this.api));
      this.handlers.put("PRESENCE_UPDATE", new PresenceUpdateHandler(this.api));
      this.handlers.put("TYPING_START", new TypingStartHandler(this.api));
      this.handlers.put("CHANNEL_PINS_ACK", nopHandler);
      this.handlers.put("CHANNEL_PINS_UPDATE", nopHandler);
      this.handlers.put("GUILD_INTEGRATIONS_UPDATE", nopHandler);
      this.handlers.put("PRESENCES_REPLACE", nopHandler);
      this.handlers.put("WEBHOOKS_UPDATE", nopHandler);
   }

   static {
      IDENTIFY_BACKOFF = TimeUnit.SECONDS.toMillis(5L);
   }

   protected class ReconnectNode extends WebSocketClient.ConnectNode {
      protected ReconnectNode() {
         super();
      }

      public boolean isReconnect() {
         return true;
      }

      public void run(boolean isLast) throws InterruptedException {
         if (!WebSocketClient.this.shutdown) {
            WebSocketClient.this.reconnect(true);
            if (!isLast) {
               try {
                  WebSocketClient.this.api.awaitStatus(JDA.Status.LOADING_SUBSYSTEMS, JDA.Status.RECONNECT_QUEUED);
               } catch (IllegalStateException var3) {
                  WebSocketClient.this.close();
                  WebSocketClient.LOG.debug("Shutdown while trying to reconnect");
               }

            }
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{"R", this.getJDA()});
      }

      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (!(obj instanceof WebSocketClient.ReconnectNode)) {
            return false;
         } else {
            WebSocketClient.ReconnectNode node = (WebSocketClient.ReconnectNode)obj;
            return node.getJDA().equals(this.getJDA());
         }
      }
   }

   protected class StartingNode extends WebSocketClient.ConnectNode {
      protected StartingNode() {
         super();
      }

      public boolean isReconnect() {
         return false;
      }

      public void run(boolean isLast) throws InterruptedException {
         if (!WebSocketClient.this.shutdown) {
            WebSocketClient.this.setupSendingThread();
            WebSocketClient.this.connect();
            if (!isLast) {
               try {
                  WebSocketClient.this.api.awaitStatus(JDA.Status.LOADING_SUBSYSTEMS, JDA.Status.RECONNECT_QUEUED);
               } catch (IllegalStateException var3) {
                  WebSocketClient.this.close();
                  WebSocketClient.LOG.debug("Shutdown while trying to connect");
               }

            }
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{"C", this.getJDA()});
      }

      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (!(obj instanceof WebSocketClient.StartingNode)) {
            return false;
         } else {
            WebSocketClient.StartingNode node = (WebSocketClient.StartingNode)obj;
            return node.getJDA().equals(this.getJDA());
         }
      }
   }

   protected abstract class ConnectNode implements SessionController.SessionConnectNode {
      @Nonnull
      public JDA getJDA() {
         return WebSocketClient.this.api;
      }

      @Nonnull
      public JDA.ShardInfo getShardInfo() {
         return WebSocketClient.this.api.getShardInfo();
      }
   }
}

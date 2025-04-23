package net.dv8tion.jda.internal.requests;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.MemberImpl;

public class MemberChunkManager {
   private static final long MAX_CHUNK_AGE = 10000L;
   private final WebSocketClient client;
   private final ReentrantLock lock = new ReentrantLock();
   private final TLongObjectMap<MemberChunkManager.ChunkRequest> requests = new TLongObjectHashMap();
   private Future<?> timeoutHandle;

   public MemberChunkManager(WebSocketClient client) {
      this.client = client;
   }

   public static boolean isLastChunk(DataObject chunk) {
      return chunk.getInt("chunk_index") + 1 == chunk.getInt("chunk_count");
   }

   public void clear() {
      ReentrantLock var10000 = this.lock;
      TLongObjectMap var10001 = this.requests;
      Objects.requireNonNull(var10001);
      MiscUtil.locked(var10000, var10001::clear);
   }

   private void init() {
      MiscUtil.locked(this.lock, () -> {
         if (this.timeoutHandle == null) {
            this.timeoutHandle = this.client.getJDA().getGatewayPool().scheduleAtFixedRate(new MemberChunkManager.TimeoutHandler(), 5L, 5L, TimeUnit.SECONDS);
         }

      });
   }

   public void shutdown() {
      if (this.timeoutHandle != null) {
         this.timeoutHandle.cancel(false);
      }

   }

   public CompletableFuture<Void> chunkGuild(GuildImpl guild, boolean presence, BiConsumer<Boolean, List<Member>> handler) {
      this.init();
      DataObject request = DataObject.empty().put("guild_id", guild.getId()).put("presences", presence).put("limit", 0).put("query", "");
      MemberChunkManager.ChunkRequest chunkRequest = new MemberChunkManager.ChunkRequest(handler, guild, request);
      this.makeRequest(chunkRequest);
      return chunkRequest;
   }

   public CompletableFuture<Void> chunkGuild(GuildImpl guild, String query, int limit, BiConsumer<Boolean, List<Member>> handler) {
      this.init();
      DataObject request = DataObject.empty().put("guild_id", guild.getId()).put("limit", Math.min(100, Math.max(1, limit))).put("query", query);
      MemberChunkManager.ChunkRequest chunkRequest = new MemberChunkManager.ChunkRequest(handler, guild, request);
      this.makeRequest(chunkRequest);
      return chunkRequest;
   }

   public CompletableFuture<Void> chunkGuild(GuildImpl guild, boolean presence, long[] userIds, BiConsumer<Boolean, List<Member>> handler) {
      this.init();
      DataObject request = DataObject.empty().put("guild_id", guild.getId()).put("presences", presence).put("user_ids", userIds);
      MemberChunkManager.ChunkRequest chunkRequest = new MemberChunkManager.ChunkRequest(handler, guild, request);
      this.makeRequest(chunkRequest);
      return chunkRequest;
   }

   public boolean handleChunk(long guildId, DataObject response) {
      return (Boolean)MiscUtil.locked(this.lock, () -> {
         String nonce = response.getString("nonce", (String)null);
         if (nonce != null && !nonce.isEmpty()) {
            long key = Long.parseLong(nonce);
            MemberChunkManager.ChunkRequest request = (MemberChunkManager.ChunkRequest)this.requests.get(key);
            if (request == null) {
               return false;
            } else {
               boolean lastChunk = isLastChunk(response);
               request.handleChunk(lastChunk, response);
               if (lastChunk || request.isCancelled()) {
                  this.requests.remove(key);
                  request.complete((Object)null);
               }

               return true;
            }
         } else {
            return false;
         }
      });
   }

   public void cancelRequest(MemberChunkManager.ChunkRequest request) {
      MiscUtil.locked(this.lock, () -> {
         this.requests.remove(request.nonce);
      });
   }

   private void makeRequest(MemberChunkManager.ChunkRequest request) {
      MiscUtil.locked(this.lock, () -> {
         this.requests.put(request.nonce, request);
         this.sendChunkRequest(request.getRequest());
      });
   }

   private void sendChunkRequest(DataObject request) {
      this.client.sendChunkRequest(request);
   }

   private class TimeoutHandler implements Runnable {
      private TimeoutHandler() {
      }

      public void run() {
         MiscUtil.locked(MemberChunkManager.this.lock, () -> {
            MemberChunkManager.this.requests.forEachValue((request) -> {
               if (request.getAge() > 10000L) {
                  request.completeExceptionally(new TimeoutException());
               }

               return true;
            });
            MemberChunkManager.this.requests.valueCollection().removeIf(CompletableFuture::isDone);
         });
      }

      // $FF: synthetic method
      TimeoutHandler(Object x1) {
         this();
      }
   }

   private class ChunkRequest extends CompletableFuture<Void> {
      private final BiConsumer<Boolean, List<Member>> handler;
      private final GuildImpl guild;
      private final DataObject request;
      private final long nonce;
      private long startTime;

      public ChunkRequest(BiConsumer<Boolean, List<Member>> handler, GuildImpl guild, DataObject request) {
         this.handler = handler;
         this.guild = guild;
         this.nonce = ThreadLocalRandom.current().nextLong() & -2L;
         this.request = request.put("nonce", this.getNonce());
      }

      public boolean isNonce(String nonce) {
         return this.nonce == Long.parseLong(nonce);
      }

      public String getNonce() {
         return String.valueOf(this.nonce);
      }

      public long getAge() {
         return this.startTime <= 0L ? 0L : System.currentTimeMillis() - this.startTime;
      }

      public DataObject getRequest() {
         this.startTime = System.currentTimeMillis();
         return this.request;
      }

      private List<Member> toMembers(DataObject chunk) {
         EntityBuilder builder = this.guild.getJDA().getEntityBuilder();
         DataArray memberArray = chunk.getArray("members");
         TLongObjectMap<DataObject> presences = (TLongObjectMap)chunk.optArray("presences").map((it) -> {
            return builder.convertToUserMap((o) -> {
               return o.getObject("user").getUnsignedLong("id");
            }, it);
         }).orElseGet(TLongObjectHashMap::new);
         List<Member> collect = new ArrayList(memberArray.length());

         for(int i = 0; i < memberArray.length(); ++i) {
            DataObject json = memberArray.getObject(i);
            long userId = json.getObject("user").getUnsignedLong("id");
            DataObject presence = (DataObject)presences.get(userId);
            MemberImpl member = builder.createMember(this.guild, json, (DataObject)null, presence);
            builder.updateMemberCache(member);
            collect.add(member);
         }

         return collect;
      }

      public void handleChunk(boolean last, DataObject chunk) {
         if (!this.isCancelled()) {
            this.handler.accept(last, this.toMembers(chunk));
         }

      }

      public boolean cancel(boolean mayInterruptIfRunning) {
         MemberChunkManager.this.client.cancelChunkRequest(this.getNonce());
         MemberChunkManager.this.cancelRequest(this);
         return super.cancel(mayInterruptIfRunning);
      }
   }
}

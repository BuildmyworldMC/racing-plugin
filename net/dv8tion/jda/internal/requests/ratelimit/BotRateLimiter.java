package net.dv8tion.jda.internal.requests.ratelimit;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.requests.RateLimiter;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.requests.Route;
import okhttp3.Headers;
import okhttp3.Response;
import org.jetbrains.annotations.Contract;

public class BotRateLimiter extends RateLimiter {
   private static final String RESET_AFTER_HEADER = "X-RateLimit-Reset-After";
   private static final String RESET_HEADER = "X-RateLimit-Reset";
   private static final String LIMIT_HEADER = "X-RateLimit-Limit";
   private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
   private static final String GLOBAL_HEADER = "X-RateLimit-Global";
   private static final String HASH_HEADER = "X-RateLimit-Bucket";
   private static final String RETRY_AFTER_HEADER = "Retry-After";
   private static final String UNLIMITED_BUCKET = "unlimited";
   private final ReentrantLock bucketLock = new ReentrantLock();
   private final Set<Route> hitRatelimit = ConcurrentHashMap.newKeySet(5);
   private final Map<Route, String> hashes = new ConcurrentHashMap();
   private final Map<String, BotRateLimiter.Bucket> buckets = new ConcurrentHashMap();
   private final Map<BotRateLimiter.Bucket, Future<?>> rateLimitQueue = new ConcurrentHashMap();
   private Future<?> cleanupWorker;

   public BotRateLimiter(Requester requester) {
      super(requester);
   }

   public void init() {
      this.cleanupWorker = this.getScheduler().scheduleAtFixedRate(this::cleanup, 30L, 30L, TimeUnit.SECONDS);
   }

   private ScheduledExecutorService getScheduler() {
      return this.requester.getJDA().getRateLimitPool();
   }

   public int cancelRequests() {
      return (Integer)MiscUtil.locked(this.bucketLock, () -> {
         AtomicInteger count = new AtomicInteger(0);
         this.buckets.values().stream().map(BotRateLimiter.Bucket::getRequests).flatMap(Collection::stream).filter((request) -> {
            return !request.isPriority() && !request.isCancelled();
         }).forEach((request) -> {
            request.cancel();
            count.incrementAndGet();
         });
         int cancelled = count.get();
         if (cancelled == 1) {
            RateLimiter.log.warn("Cancelled 1 request!");
         } else if (cancelled > 1) {
            RateLimiter.log.warn((String)"Cancelled {} requests!", (Object)cancelled);
         }

         return cancelled;
      });
   }

   private void cleanup() {
      MiscUtil.locked(this.bucketLock, () -> {
         int size = this.buckets.size();
         Iterator entries = this.buckets.entrySet().iterator();

         while(true) {
            while(entries.hasNext()) {
               Entry<String, BotRateLimiter.Bucket> entry = (Entry)entries.next();
               String key = (String)entry.getKey();
               BotRateLimiter.Bucket bucket = (BotRateLimiter.Bucket)entry.getValue();
               bucket.requests.removeIf(Request::isSkipped);
               if (bucket.isUnlimited() && bucket.requests.isEmpty()) {
                  entries.remove();
               } else if (bucket.requests.isEmpty() && bucket.reset <= this.getNow()) {
                  entries.remove();
               }
            }

            size -= this.buckets.size();
            if (size > 0) {
               log.debug((String)"Removed {} expired buckets", (Object)size);
            }

            return;
         }
      });
   }

   private String getRouteHash(Route route) {
      return (String)this.hashes.getOrDefault(route, "unlimited+" + route);
   }

   protected boolean stop() {
      return (Boolean)MiscUtil.locked(this.bucketLock, () -> {
         if (this.isStopped) {
            return false;
         } else {
            super.stop();
            if (this.cleanupWorker != null) {
               this.cleanupWorker.cancel(false);
            }

            this.cleanup();
            int size = this.buckets.size();
            if (!this.isShutdown && size > 0) {
               int average = (int)Math.ceil(this.buckets.values().stream().map(BotRateLimiter.Bucket::getRequests).mapToInt(Collection::size).average().orElse(0.0D));
               log.info((String)"Waiting for {} bucket(s) to finish. Average queue size of {} requests", (Object)size, (Object)average);
            }

            return size < 1;
         }
      });
   }

   public Long getRateLimit(Route.CompiledRoute route) {
      BotRateLimiter.Bucket bucket = this.getBucket(route, false);
      return bucket == null ? 0L : bucket.getRateLimit();
   }

   protected void queueRequest(Request request) {
      MiscUtil.locked(this.bucketLock, () -> {
         BotRateLimiter.Bucket bucket = this.getBucket(request.getRoute(), true);
         bucket.enqueue(request);
         this.runBucket(bucket);
      });
   }

   protected Long handleResponse(Route.CompiledRoute route, Response response) {
      return (Long)MiscUtil.locked(this.bucketLock, () -> {
         long rateLimit = this.updateBucket(route, response).getRateLimit();
         return response.code() == 429 ? rateLimit : null;
      });
   }

   private BotRateLimiter.Bucket updateBucket(Route.CompiledRoute route, Response response) {
      return (BotRateLimiter.Bucket)MiscUtil.locked(this.bucketLock, () -> {
         try {
            BotRateLimiter.Bucket bucket = this.getBucket(route, true);
            Headers headers = response.headers();
            boolean global = headers.get("X-RateLimit-Global") != null;
            boolean cloudflare = headers.get("via") == null;
            String hash = headers.get("X-RateLimit-Bucket");
            long now = this.getNow();
            Route baseRoute = route.getBaseRoute();
            if (hash != null) {
               if (!this.hashes.containsKey(baseRoute)) {
                  this.hashes.put(baseRoute, hash);
                  log.debug((String)"Caching bucket hash {} -> {}", (Object)baseRoute, (Object)hash);
               }

               bucket = this.getBucket(route, true);
            }

            String limitHeader;
            if (response.code() == 429) {
               limitHeader = headers.get("Retry-After");
               long retryAfter = this.parseLong(limitHeader);
               if (global) {
                  this.requester.getJDA().getSessionController().setGlobalRatelimit(now + retryAfter);
                  log.error((String)"Encountered global rate limit! Retry-After: {} ms", (Object)retryAfter);
               } else if (cloudflare) {
                  this.requester.getJDA().getSessionController().setGlobalRatelimit(now + retryAfter * 1000L);
                  log.error((String)"Encountered cloudflare rate limit! Retry-After: {} s", (Object)retryAfter);
               } else {
                  boolean firstHit = this.hitRatelimit.add(baseRoute);
                  bucket.remaining = 0;
                  bucket.reset = this.getNow() + retryAfter;
                  if (firstHit) {
                     log.debug("Encountered 429 on route {} with bucket {} Retry-After: {} ms", baseRoute, bucket.bucketId, retryAfter);
                  } else {
                     log.warn("Encountered 429 on route {} with bucket {} Retry-After: {} ms", baseRoute, bucket.bucketId, retryAfter);
                  }
               }

               return bucket;
            } else if (hash == null) {
               return bucket;
            } else {
               limitHeader = headers.get("X-RateLimit-Limit");
               String remainingHeader = headers.get("X-RateLimit-Remaining");
               String resetAfterHeader = headers.get("X-RateLimit-Reset-After");
               String resetHeader = headers.get("X-RateLimit-Reset");
               bucket.limit = (int)Math.max(1L, this.parseLong(limitHeader));
               bucket.remaining = (int)this.parseLong(remainingHeader);
               if (this.requester.getJDA().isRelativeRateLimit()) {
                  bucket.reset = now + this.parseDouble(resetAfterHeader);
               } else {
                  bucket.reset = this.parseDouble(resetHeader);
               }

               log.trace("Updated bucket {} to ({}/{}, {})", bucket.bucketId, bucket.remaining, bucket.limit, bucket.reset - now);
               return bucket;
            }
         } catch (Exception var15) {
            BotRateLimiter.Bucket bucketx = this.getBucket(route, true);
            log.error("Encountered Exception while updating a bucket. Route: {} Bucket: {} Code: {} Headers:\n{}", route.getBaseRoute(), bucketx, response.code(), response.headers(), var15);
            return bucketx;
         }
      });
   }

   @Contract("_,true->!null")
   private BotRateLimiter.Bucket getBucket(Route.CompiledRoute route, boolean create) {
      return (BotRateLimiter.Bucket)MiscUtil.locked(this.bucketLock, () -> {
         String hash = this.getRouteHash(route.getBaseRoute());
         String bucketId = hash + ":" + route.getMajorParameters();
         BotRateLimiter.Bucket bucket = (BotRateLimiter.Bucket)this.buckets.get(bucketId);
         if (bucket == null && create) {
            this.buckets.put(bucketId, bucket = new BotRateLimiter.Bucket(bucketId));
         }

         return bucket;
      });
   }

   private void runBucket(BotRateLimiter.Bucket bucket) {
      if (!this.isShutdown) {
         MiscUtil.locked(this.bucketLock, () -> {
            return (Future)this.rateLimitQueue.computeIfAbsent(bucket, (k) -> {
               return this.getScheduler().schedule(bucket, bucket.getRateLimit(), TimeUnit.MILLISECONDS);
            });
         });
      }
   }

   private long parseLong(String input) {
      return input == null ? 0L : Long.parseLong(input);
   }

   private long parseDouble(String input) {
      return input == null ? 0L : (long)(Double.parseDouble(input) * 1000.0D);
   }

   public long getNow() {
      return System.currentTimeMillis();
   }

   private class Bucket implements IBucket, Runnable {
      private final String bucketId;
      private final Deque<Request> requests = new ConcurrentLinkedDeque();
      private long reset = 0L;
      private int remaining = 1;
      private int limit = 1;

      public Bucket(String bucketId) {
         this.bucketId = bucketId;
      }

      public void enqueue(Request request) {
         this.requests.addLast(request);
      }

      public void retry(Request request) {
         this.requests.addFirst(request);
      }

      public long getRateLimit() {
         long now = BotRateLimiter.this.getNow();
         long global = BotRateLimiter.this.requester.getJDA().getSessionController().getGlobalRatelimit();
         if (global > now) {
            return global - now;
         } else if (this.reset <= now) {
            this.remaining = this.limit;
            return 0L;
         } else {
            return this.remaining < 1 ? this.reset - now : 0L;
         }
      }

      public long getReset() {
         return this.reset;
      }

      public int getRemaining() {
         return this.remaining;
      }

      public int getLimit() {
         return this.limit;
      }

      private boolean isUnlimited() {
         return this.bucketId.startsWith("unlimited");
      }

      private void backoff() {
         MiscUtil.locked(BotRateLimiter.this.bucketLock, () -> {
            BotRateLimiter.this.rateLimitQueue.remove(this);
            if (!this.requests.isEmpty()) {
               BotRateLimiter.this.runBucket(this);
            } else if (BotRateLimiter.this.isStopped) {
               BotRateLimiter.this.buckets.remove(this.bucketId);
            }

            if (BotRateLimiter.this.isStopped && BotRateLimiter.this.buckets.isEmpty()) {
               BotRateLimiter.this.requester.getJDA().shutdownRequester();
            }

         });
      }

      public void run() {
         BotRateLimiter.log.trace((String)"Bucket {} is running {} requests", (Object)this.bucketId, (Object)this.requests.size());

         while(!this.requests.isEmpty()) {
            Long rateLimit = this.getRateLimit();
            if (rateLimit > 0L) {
               BotRateLimiter.log.debug((String)"Backing off {} ms for bucket {}", (Object)rateLimit, (Object)this.bucketId);
               break;
            }

            Request request = (Request)this.requests.removeFirst();
            if (!request.isSkipped()) {
               if (this.isUnlimited()) {
                  boolean shouldSkip = (Boolean)MiscUtil.locked(BotRateLimiter.this.bucketLock, () -> {
                     BotRateLimiter.Bucket bucket = BotRateLimiter.this.getBucket(request.getRoute(), true);
                     if (bucket != this) {
                        bucket.enqueue(request);
                        BotRateLimiter.this.runBucket(bucket);
                        return true;
                     } else {
                        return false;
                     }
                  });
                  if (shouldSkip) {
                     continue;
                  }
               }

               try {
                  rateLimit = BotRateLimiter.this.requester.execute(request);
                  if (rateLimit != null) {
                     this.retry(request);
                  }
               } catch (Throwable var4) {
                  BotRateLimiter.log.error("Encountered exception trying to execute request", var4);
                  if (!(var4 instanceof Error)) {
                     break;
                  }

                  throw (Error)var4;
               }
            }
         }

         this.backoff();
      }

      public Queue<Request> getRequests() {
         return this.requests;
      }

      public String toString() {
         return this.bucketId;
      }
   }
}

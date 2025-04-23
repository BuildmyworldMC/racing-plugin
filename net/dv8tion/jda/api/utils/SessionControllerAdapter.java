package net.dv8tion.jda.api.utils;

import com.neovisionaries.ws.client.OpeningHandshakeException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.AccountTypeException;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.slf4j.Logger;

public class SessionControllerAdapter implements SessionController {
   protected static final Logger log = JDALogger.getLog(SessionControllerAdapter.class);
   protected final Object lock = new Object();
   protected Queue<SessionController.SessionConnectNode> connectQueue = new ConcurrentLinkedQueue();
   protected AtomicLong globalRatelimit = new AtomicLong(Long.MIN_VALUE);
   protected Thread workerHandle;
   protected long lastConnect = 0L;

   public void appendSession(@Nonnull SessionController.SessionConnectNode node) {
      this.removeSession(node);
      this.connectQueue.add(node);
      this.runWorker();
   }

   public void removeSession(@Nonnull SessionController.SessionConnectNode node) {
      this.connectQueue.remove(node);
   }

   public long getGlobalRatelimit() {
      return this.globalRatelimit.get();
   }

   public void setGlobalRatelimit(long ratelimit) {
      this.globalRatelimit.set(ratelimit);
   }

   @Nonnull
   public String getGateway(@Nonnull JDA api) {
      Route.CompiledRoute route = Route.Misc.GATEWAY.compile();
      return (String)(new RestActionImpl(api, route, (response, request) -> {
         return response.getObject().getString("url");
      })).priority().complete();
   }

   @Nonnull
   public SessionController.ShardedGateway getShardedGateway(@Nonnull JDA api) {
      AccountTypeException.check(api.getAccountType(), AccountType.BOT);
      return (SessionController.ShardedGateway)(new RestActionImpl<SessionController.ShardedGateway>(api, Route.Misc.GATEWAY_BOT.compile()) {
         public void handleResponse(Response response, Request<SessionController.ShardedGateway> request) {
            if (response.isOk()) {
               DataObject object = response.getObject();
               String url = object.getString("url");
               int shards = object.getInt("shards");
               int concurrency = object.getObject("session_start_limit").getInt("max_concurrency", 1);
               request.onSuccess(new SessionController.ShardedGateway(url, shards, concurrency));
            } else if (response.code == 401) {
               this.api.shutdownNow();
               request.onFailure((Throwable)(new LoginException("The provided token is invalid!")));
            } else {
               request.onFailure(response);
            }

         }
      }).priority().complete();
   }

   @Nonnull
   public Pair<String, Integer> getGatewayBot(@Nonnull JDA api) {
      SessionController.ShardedGateway bot = this.getShardedGateway(api);
      return Pair.of(bot.getUrl(), bot.getShardTotal());
   }

   protected void runWorker() {
      synchronized(this.lock) {
         if (this.workerHandle == null) {
            this.workerHandle = new SessionControllerAdapter.QueueWorker();
            this.workerHandle.start();
         }

      }
   }

   protected class QueueWorker extends Thread {
      protected final long delay;

      public QueueWorker() {
         this(5);
      }

      public QueueWorker(int delay) {
         this(TimeUnit.SECONDS.toMillis((long)delay));
      }

      public QueueWorker(long delay) {
         super("SessionControllerAdapter-Worker");
         this.delay = delay;
         super.setUncaughtExceptionHandler(this::handleFailure);
      }

      protected void handleFailure(Thread thread, Throwable exception) {
         SessionControllerAdapter.log.error("Worker has failed with throwable!", exception);
      }

      public void run() {
         try {
            if (this.delay > 0L) {
               long interval = System.currentTimeMillis() - SessionControllerAdapter.this.lastConnect;
               if (interval < this.delay) {
                  Thread.sleep(this.delay - interval);
               }
            }
         } catch (InterruptedException var5) {
            SessionControllerAdapter.log.error((String)"Unable to backoff", (Throwable)var5);
         }

         this.processQueue();
         synchronized(SessionControllerAdapter.this.lock) {
            SessionControllerAdapter.this.workerHandle = null;
            if (!SessionControllerAdapter.this.connectQueue.isEmpty()) {
               SessionControllerAdapter.this.runWorker();
            }

         }
      }

      protected void processQueue() {
         boolean isMultiple = SessionControllerAdapter.this.connectQueue.size() > 1;

         while(!SessionControllerAdapter.this.connectQueue.isEmpty()) {
            SessionController.SessionConnectNode node = (SessionController.SessionConnectNode)SessionControllerAdapter.this.connectQueue.poll();

            try {
               node.run(isMultiple && SessionControllerAdapter.this.connectQueue.isEmpty());
               isMultiple = true;
               SessionControllerAdapter.this.lastConnect = System.currentTimeMillis();
               if (SessionControllerAdapter.this.connectQueue.isEmpty()) {
                  break;
               }

               if (this.delay > 0L) {
                  Thread.sleep(this.delay);
               }
            } catch (IllegalStateException var5) {
               Throwable t = var5.getCause();
               if (t instanceof OpeningHandshakeException) {
                  SessionControllerAdapter.log.error((String)"Failed opening handshake, appending to queue. Message: {}", (Object)var5.getMessage());
               } else if (t != null && !JDA.Status.RECONNECT_QUEUED.name().equals(t.getMessage())) {
                  SessionControllerAdapter.log.error((String)"Failed to establish connection for a node, appending to queue", (Throwable)var5);
               } else {
                  SessionControllerAdapter.log.error((String)"Unexpected exception when running connect node", (Throwable)var5);
               }

               SessionControllerAdapter.this.appendSession(node);
            } catch (InterruptedException var6) {
               SessionControllerAdapter.log.error((String)"Failed to run node", (Throwable)var6);
               SessionControllerAdapter.this.appendSession(node);
               return;
            }
         }

      }
   }
}

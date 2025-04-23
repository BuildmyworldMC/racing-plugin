package net.dv8tion.jda.internal.requests;

import java.util.Iterator;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.internal.utils.JDALogger;
import okhttp3.Response;
import org.slf4j.Logger;

public abstract class RateLimiter {
   protected static final Logger log = JDALogger.getLog(RateLimiter.class);
   protected final Requester requester;
   protected volatile boolean isShutdown = false;
   protected volatile boolean isStopped = false;

   protected RateLimiter(Requester requester) {
      this.requester = requester;
   }

   protected boolean isSkipped(Iterator<Request> it, Request request) {
      if (request.isSkipped()) {
         this.cancel(it, request);
         return true;
      } else {
         return false;
      }
   }

   private void cancel(Iterator<Request> it, Request request) {
      request.onCancelled();
      it.remove();
   }

   public abstract Long getRateLimit(Route.CompiledRoute var1);

   protected abstract void queueRequest(Request var1);

   protected abstract Long handleResponse(Route.CompiledRoute var1, Response var2);

   public boolean isRateLimited(Route.CompiledRoute route) {
      Long rateLimit = this.getRateLimit(route);
      return rateLimit != null && rateLimit > 0L;
   }

   public abstract int cancelRequests();

   public void init() {
   }

   protected boolean stop() {
      this.isStopped = true;
      return true;
   }

   protected void shutdown() {
      this.isShutdown = true;
      this.stop();
   }
}

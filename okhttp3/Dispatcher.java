package okhttp3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.internal.Util;

public final class Dispatcher {
   private int maxRequests = 64;
   private int maxRequestsPerHost = 5;
   @Nullable
   private Runnable idleCallback;
   @Nullable
   private ExecutorService executorService;
   private final Deque<RealCall.AsyncCall> readyAsyncCalls = new ArrayDeque();
   private final Deque<RealCall.AsyncCall> runningAsyncCalls = new ArrayDeque();
   private final Deque<RealCall> runningSyncCalls = new ArrayDeque();

   public Dispatcher(ExecutorService executorService) {
      this.executorService = executorService;
   }

   public Dispatcher() {
   }

   public synchronized ExecutorService executorService() {
      if (this.executorService == null) {
         this.executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp Dispatcher", false));
      }

      return this.executorService;
   }

   public void setMaxRequests(int maxRequests) {
      if (maxRequests < 1) {
         throw new IllegalArgumentException("max < 1: " + maxRequests);
      } else {
         synchronized(this) {
            this.maxRequests = maxRequests;
         }

         this.promoteAndExecute();
      }
   }

   public synchronized int getMaxRequests() {
      return this.maxRequests;
   }

   public void setMaxRequestsPerHost(int maxRequestsPerHost) {
      if (maxRequestsPerHost < 1) {
         throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
      } else {
         synchronized(this) {
            this.maxRequestsPerHost = maxRequestsPerHost;
         }

         this.promoteAndExecute();
      }
   }

   public synchronized int getMaxRequestsPerHost() {
      return this.maxRequestsPerHost;
   }

   public synchronized void setIdleCallback(@Nullable Runnable idleCallback) {
      this.idleCallback = idleCallback;
   }

   void enqueue(RealCall.AsyncCall call) {
      synchronized(this) {
         this.readyAsyncCalls.add(call);
         if (!call.get().forWebSocket) {
            RealCall.AsyncCall existingCall = this.findExistingCallWithHost(call.host());
            if (existingCall != null) {
               call.reuseCallsPerHostFrom(existingCall);
            }
         }
      }

      this.promoteAndExecute();
   }

   @Nullable
   private RealCall.AsyncCall findExistingCallWithHost(String host) {
      Iterator var2 = this.runningAsyncCalls.iterator();

      RealCall.AsyncCall existingCall;
      do {
         if (!var2.hasNext()) {
            var2 = this.readyAsyncCalls.iterator();

            do {
               if (!var2.hasNext()) {
                  return null;
               }

               existingCall = (RealCall.AsyncCall)var2.next();
            } while(!existingCall.host().equals(host));

            return existingCall;
         }

         existingCall = (RealCall.AsyncCall)var2.next();
      } while(!existingCall.host().equals(host));

      return existingCall;
   }

   public synchronized void cancelAll() {
      Iterator var1 = this.readyAsyncCalls.iterator();

      RealCall.AsyncCall call;
      while(var1.hasNext()) {
         call = (RealCall.AsyncCall)var1.next();
         call.get().cancel();
      }

      var1 = this.runningAsyncCalls.iterator();

      while(var1.hasNext()) {
         call = (RealCall.AsyncCall)var1.next();
         call.get().cancel();
      }

      var1 = this.runningSyncCalls.iterator();

      while(var1.hasNext()) {
         RealCall call = (RealCall)var1.next();
         call.cancel();
      }

   }

   private boolean promoteAndExecute() {
      assert !Thread.holdsLock(this);

      List<RealCall.AsyncCall> executableCalls = new ArrayList();
      boolean isRunning;
      RealCall.AsyncCall asyncCall;
      synchronized(this) {
         Iterator i = this.readyAsyncCalls.iterator();

         while(true) {
            if (i.hasNext()) {
               asyncCall = (RealCall.AsyncCall)i.next();
               if (this.runningAsyncCalls.size() < this.maxRequests) {
                  if (asyncCall.callsPerHost().get() < this.maxRequestsPerHost) {
                     i.remove();
                     asyncCall.callsPerHost().incrementAndGet();
                     executableCalls.add(asyncCall);
                     this.runningAsyncCalls.add(asyncCall);
                  }
                  continue;
               }
            }

            isRunning = this.runningCallsCount() > 0;
            break;
         }
      }

      int i = 0;

      for(int size = executableCalls.size(); i < size; ++i) {
         asyncCall = (RealCall.AsyncCall)executableCalls.get(i);
         asyncCall.executeOn(this.executorService());
      }

      return isRunning;
   }

   synchronized void executed(RealCall call) {
      this.runningSyncCalls.add(call);
   }

   void finished(RealCall.AsyncCall call) {
      call.callsPerHost().decrementAndGet();
      this.finished(this.runningAsyncCalls, call);
   }

   void finished(RealCall call) {
      this.finished(this.runningSyncCalls, call);
   }

   private <T> void finished(Deque<T> calls, T call) {
      Runnable idleCallback;
      synchronized(this) {
         if (!calls.remove(call)) {
            throw new AssertionError("Call wasn't in-flight!");
         }

         idleCallback = this.idleCallback;
      }

      boolean isRunning = this.promoteAndExecute();
      if (!isRunning && idleCallback != null) {
         idleCallback.run();
      }

   }

   public synchronized List<Call> queuedCalls() {
      List<Call> result = new ArrayList();
      Iterator var2 = this.readyAsyncCalls.iterator();

      while(var2.hasNext()) {
         RealCall.AsyncCall asyncCall = (RealCall.AsyncCall)var2.next();
         result.add(asyncCall.get());
      }

      return Collections.unmodifiableList(result);
   }

   public synchronized List<Call> runningCalls() {
      List<Call> result = new ArrayList();
      result.addAll(this.runningSyncCalls);
      Iterator var2 = this.runningAsyncCalls.iterator();

      while(var2.hasNext()) {
         RealCall.AsyncCall asyncCall = (RealCall.AsyncCall)var2.next();
         result.add(asyncCall.get());
      }

      return Collections.unmodifiableList(result);
   }

   public synchronized int queuedCallsCount() {
      return this.readyAsyncCalls.size();
   }

   public synchronized int runningCallsCount() {
      return this.runningAsyncCalls.size() + this.runningSyncCalls.size();
   }
}

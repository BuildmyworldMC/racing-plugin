package okhttp3;

import java.lang.ref.Reference;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.RouteDatabase;
import okhttp3.internal.connection.StreamAllocation;
import okhttp3.internal.platform.Platform;

public final class ConnectionPool {
   private static final Executor executor;
   private final int maxIdleConnections;
   private final long keepAliveDurationNs;
   private final Runnable cleanupRunnable;
   private final Deque<RealConnection> connections;
   final RouteDatabase routeDatabase;
   boolean cleanupRunning;

   public ConnectionPool() {
      this(5, 5L, TimeUnit.MINUTES);
   }

   public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
      this.cleanupRunnable = () -> {
         while(true) {
            long waitNanos = this.cleanup(System.nanoTime());
            if (waitNanos == -1L) {
               return;
            }

            if (waitNanos > 0L) {
               long waitMillis = waitNanos / 1000000L;
               waitNanos -= waitMillis * 1000000L;
               synchronized(this) {
                  try {
                     this.wait(waitMillis, (int)waitNanos);
                  } catch (InterruptedException var8) {
                  }
               }
            }
         }
      };
      this.connections = new ArrayDeque();
      this.routeDatabase = new RouteDatabase();
      this.maxIdleConnections = maxIdleConnections;
      this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);
      if (keepAliveDuration <= 0L) {
         throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
      }
   }

   public synchronized int idleConnectionCount() {
      int total = 0;
      Iterator var2 = this.connections.iterator();

      while(var2.hasNext()) {
         RealConnection connection = (RealConnection)var2.next();
         if (connection.allocations.isEmpty()) {
            ++total;
         }
      }

      return total;
   }

   public synchronized int connectionCount() {
      return this.connections.size();
   }

   void acquire(Address address, StreamAllocation streamAllocation, @Nullable Route route) {
      assert Thread.holdsLock(this);

      Iterator var4 = this.connections.iterator();

      RealConnection connection;
      do {
         if (!var4.hasNext()) {
            return;
         }

         connection = (RealConnection)var4.next();
      } while(!connection.isEligible(address, route));

      streamAllocation.acquire(connection, true);
   }

   @Nullable
   Socket deduplicate(Address address, StreamAllocation streamAllocation) {
      assert Thread.holdsLock(this);

      Iterator var3 = this.connections.iterator();

      RealConnection connection;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         connection = (RealConnection)var3.next();
      } while(!connection.isEligible(address, (Route)null) || !connection.isMultiplexed() || connection == streamAllocation.connection());

      return streamAllocation.releaseAndAcquire(connection);
   }

   void put(RealConnection connection) {
      assert Thread.holdsLock(this);

      if (!this.cleanupRunning) {
         this.cleanupRunning = true;
         executor.execute(this.cleanupRunnable);
      }

      this.connections.add(connection);
   }

   boolean connectionBecameIdle(RealConnection connection) {
      assert Thread.holdsLock(this);

      if (!connection.noNewStreams && this.maxIdleConnections != 0) {
         this.notifyAll();
         return false;
      } else {
         this.connections.remove(connection);
         return true;
      }
   }

   public void evictAll() {
      List<RealConnection> evictedConnections = new ArrayList();
      synchronized(this) {
         Iterator i = this.connections.iterator();

         while(true) {
            if (!i.hasNext()) {
               break;
            }

            RealConnection connection = (RealConnection)i.next();
            if (connection.allocations.isEmpty()) {
               connection.noNewStreams = true;
               evictedConnections.add(connection);
               i.remove();
            }
         }
      }

      Iterator var2 = evictedConnections.iterator();

      while(var2.hasNext()) {
         RealConnection connection = (RealConnection)var2.next();
         Util.closeQuietly(connection.socket());
      }

   }

   long cleanup(long now) {
      int inUseConnectionCount = 0;
      int idleConnectionCount = 0;
      RealConnection longestIdleConnection = null;
      long longestIdleDurationNs = Long.MIN_VALUE;
      synchronized(this) {
         Iterator i = this.connections.iterator();

         while(i.hasNext()) {
            RealConnection connection = (RealConnection)i.next();
            if (this.pruneAndGetAllocationCount(connection, now) > 0) {
               ++inUseConnectionCount;
            } else {
               ++idleConnectionCount;
               long idleDurationNs = now - connection.idleAtNanos;
               if (idleDurationNs > longestIdleDurationNs) {
                  longestIdleDurationNs = idleDurationNs;
                  longestIdleConnection = connection;
               }
            }
         }

         if (longestIdleDurationNs < this.keepAliveDurationNs && idleConnectionCount <= this.maxIdleConnections) {
            if (idleConnectionCount > 0) {
               return this.keepAliveDurationNs - longestIdleDurationNs;
            }

            if (inUseConnectionCount > 0) {
               return this.keepAliveDurationNs;
            }

            this.cleanupRunning = false;
            return -1L;
         }

         this.connections.remove(longestIdleConnection);
      }

      Util.closeQuietly(longestIdleConnection.socket());
      return 0L;
   }

   private int pruneAndGetAllocationCount(RealConnection connection, long now) {
      List<Reference<StreamAllocation>> references = connection.allocations;
      int i = 0;

      while(i < references.size()) {
         Reference<StreamAllocation> reference = (Reference)references.get(i);
         if (reference.get() != null) {
            ++i;
         } else {
            StreamAllocation.StreamAllocationReference streamAllocRef = (StreamAllocation.StreamAllocationReference)reference;
            String message = "A connection to " + connection.route().address().url() + " was leaked. Did you forget to close a response body?";
            Platform.get().logCloseableLeak(message, streamAllocRef.callStackTrace);
            references.remove(i);
            connection.noNewStreams = true;
            if (references.isEmpty()) {
               connection.idleAtNanos = now - this.keepAliveDurationNs;
               return 0;
            }
         }
      }

      return references.size();
   }

   static {
      executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp ConnectionPool", true));
   }
}

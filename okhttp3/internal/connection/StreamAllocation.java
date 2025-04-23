package okhttp3.internal.connection;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.List;
import okhttp3.Address;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Route;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http2.ConnectionShutdownException;
import okhttp3.internal.http2.ErrorCode;
import okhttp3.internal.http2.StreamResetException;

public final class StreamAllocation {
   public final Address address;
   private RouteSelector.Selection routeSelection;
   private Route route;
   private final ConnectionPool connectionPool;
   public final Call call;
   public final EventListener eventListener;
   private final Object callStackTrace;
   private final RouteSelector routeSelector;
   private int refusedStreamCount;
   private RealConnection connection;
   private boolean reportedAcquired;
   private boolean released;
   private boolean canceled;
   private HttpCodec codec;

   public StreamAllocation(ConnectionPool connectionPool, Address address, Call call, EventListener eventListener, Object callStackTrace) {
      this.connectionPool = connectionPool;
      this.address = address;
      this.call = call;
      this.eventListener = eventListener;
      this.routeSelector = new RouteSelector(address, this.routeDatabase(), call, eventListener);
      this.callStackTrace = callStackTrace;
   }

   public HttpCodec newStream(OkHttpClient client, Interceptor.Chain chain, boolean doExtensiveHealthChecks) {
      int connectTimeout = chain.connectTimeoutMillis();
      int readTimeout = chain.readTimeoutMillis();
      int writeTimeout = chain.writeTimeoutMillis();
      int pingIntervalMillis = client.pingIntervalMillis();
      boolean connectionRetryEnabled = client.retryOnConnectionFailure();

      try {
         RealConnection resultConnection = this.findHealthyConnection(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis, connectionRetryEnabled, doExtensiveHealthChecks);
         HttpCodec resultCodec = resultConnection.newCodec(client, chain, this);
         synchronized(this.connectionPool) {
            this.codec = resultCodec;
            return resultCodec;
         }
      } catch (IOException var14) {
         throw new RouteException(var14);
      }
   }

   private RealConnection findHealthyConnection(int connectTimeout, int readTimeout, int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks) throws IOException {
      while(true) {
         RealConnection candidate = this.findConnection(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis, connectionRetryEnabled);
         synchronized(this.connectionPool) {
            if (candidate.successCount == 0) {
               return candidate;
            }
         }

         if (candidate.isHealthy(doExtensiveHealthChecks)) {
            return candidate;
         }

         this.noNewStreams();
      }
   }

   private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout, int pingIntervalMillis, boolean connectionRetryEnabled) throws IOException {
      boolean foundPooledConnection = false;
      RealConnection result = null;
      Route selectedRoute = null;
      RealConnection releasedConnection;
      Socket toClose;
      synchronized(this.connectionPool) {
         if (this.released) {
            throw new IllegalStateException("released");
         }

         if (this.codec != null) {
            throw new IllegalStateException("codec != null");
         }

         if (this.canceled) {
            throw new IOException("Canceled");
         }

         releasedConnection = this.connection;
         toClose = this.releaseIfNoNewStreams();
         if (this.connection != null) {
            result = this.connection;
            releasedConnection = null;
         }

         if (!this.reportedAcquired) {
            releasedConnection = null;
         }

         if (result == null) {
            Internal.instance.acquire(this.connectionPool, this.address, this, (Route)null);
            if (this.connection != null) {
               foundPooledConnection = true;
               result = this.connection;
            } else {
               selectedRoute = this.route;
            }
         }
      }

      Util.closeQuietly(toClose);
      if (releasedConnection != null) {
         this.eventListener.connectionReleased(this.call, releasedConnection);
      }

      if (foundPooledConnection) {
         this.eventListener.connectionAcquired(this.call, result);
      }

      if (result != null) {
         return result;
      } else {
         boolean newRouteSelection = false;
         if (selectedRoute == null && (this.routeSelection == null || !this.routeSelection.hasNext())) {
            newRouteSelection = true;
            this.routeSelection = this.routeSelector.next();
         }

         synchronized(this.connectionPool) {
            if (this.canceled) {
               throw new IOException("Canceled");
            }

            if (newRouteSelection) {
               List<Route> routes = this.routeSelection.getAll();
               int i = 0;

               for(int size = routes.size(); i < size; ++i) {
                  Route route = (Route)routes.get(i);
                  Internal.instance.acquire(this.connectionPool, this.address, this, route);
                  if (this.connection != null) {
                     foundPooledConnection = true;
                     result = this.connection;
                     this.route = route;
                     break;
                  }
               }
            }

            if (!foundPooledConnection) {
               if (selectedRoute == null) {
                  selectedRoute = this.routeSelection.next();
               }

               this.route = selectedRoute;
               this.refusedStreamCount = 0;
               result = new RealConnection(this.connectionPool, selectedRoute);
               this.acquire(result, false);
            }
         }

         if (foundPooledConnection) {
            this.eventListener.connectionAcquired(this.call, result);
            return result;
         } else {
            result.connect(connectTimeout, readTimeout, writeTimeout, pingIntervalMillis, connectionRetryEnabled, this.call, this.eventListener);
            this.routeDatabase().connected(result.route());
            Socket socket = null;
            synchronized(this.connectionPool) {
               this.reportedAcquired = true;
               Internal.instance.put(this.connectionPool, result);
               if (result.isMultiplexed()) {
                  socket = Internal.instance.deduplicate(this.connectionPool, this.address, this);
                  result = this.connection;
               }
            }

            Util.closeQuietly(socket);
            this.eventListener.connectionAcquired(this.call, result);
            return result;
         }
      }
   }

   private Socket releaseIfNoNewStreams() {
      assert Thread.holdsLock(this.connectionPool);

      RealConnection allocatedConnection = this.connection;
      return allocatedConnection != null && allocatedConnection.noNewStreams ? this.deallocate(false, false, true) : null;
   }

   public void streamFinished(boolean noNewStreams, HttpCodec codec, long bytesRead, IOException e) {
      this.eventListener.responseBodyEnd(this.call, bytesRead);
      Socket socket;
      RealConnection releasedConnection;
      boolean callEnd;
      synchronized(this.connectionPool) {
         if (codec == null || codec != this.codec) {
            throw new IllegalStateException("expected " + this.codec + " but was " + codec);
         }

         if (!noNewStreams) {
            ++this.connection.successCount;
         }

         releasedConnection = this.connection;
         socket = this.deallocate(noNewStreams, false, true);
         if (this.connection != null) {
            releasedConnection = null;
         }

         callEnd = this.released;
      }

      Util.closeQuietly(socket);
      if (releasedConnection != null) {
         this.eventListener.connectionReleased(this.call, releasedConnection);
      }

      if (e != null) {
         e = Internal.instance.timeoutExit(this.call, e);
         this.eventListener.callFailed(this.call, e);
      } else if (callEnd) {
         Internal.instance.timeoutExit(this.call, (IOException)null);
         this.eventListener.callEnd(this.call);
      }

   }

   public HttpCodec codec() {
      synchronized(this.connectionPool) {
         return this.codec;
      }
   }

   private RouteDatabase routeDatabase() {
      return Internal.instance.routeDatabase(this.connectionPool);
   }

   public Route route() {
      return this.route;
   }

   public synchronized RealConnection connection() {
      return this.connection;
   }

   public void release(boolean callEnd) {
      Socket socket;
      RealConnection releasedConnection;
      synchronized(this.connectionPool) {
         releasedConnection = this.connection;
         socket = this.deallocate(false, true, false);
         if (this.connection != null) {
            releasedConnection = null;
         }
      }

      Util.closeQuietly(socket);
      if (releasedConnection != null) {
         if (callEnd) {
            Internal.instance.timeoutExit(this.call, (IOException)null);
         }

         this.eventListener.connectionReleased(this.call, releasedConnection);
         if (callEnd) {
            this.eventListener.callEnd(this.call);
         }
      }

   }

   public void noNewStreams() {
      Socket socket;
      RealConnection releasedConnection;
      synchronized(this.connectionPool) {
         releasedConnection = this.connection;
         socket = this.deallocate(true, false, false);
         if (this.connection != null) {
            releasedConnection = null;
         }
      }

      Util.closeQuietly(socket);
      if (releasedConnection != null) {
         this.eventListener.connectionReleased(this.call, releasedConnection);
      }

   }

   private Socket deallocate(boolean noNewStreams, boolean released, boolean streamFinished) {
      assert Thread.holdsLock(this.connectionPool);

      if (streamFinished) {
         this.codec = null;
      }

      if (released) {
         this.released = true;
      }

      Socket socket = null;
      if (this.connection != null) {
         if (noNewStreams) {
            this.connection.noNewStreams = true;
         }

         if (this.codec == null && (this.released || this.connection.noNewStreams)) {
            this.release(this.connection);
            if (this.connection.allocations.isEmpty()) {
               this.connection.idleAtNanos = System.nanoTime();
               if (Internal.instance.connectionBecameIdle(this.connectionPool, this.connection)) {
                  socket = this.connection.socket();
               }
            }

            this.connection = null;
         }
      }

      return socket;
   }

   public void cancel() {
      HttpCodec codecToCancel;
      RealConnection connectionToCancel;
      synchronized(this.connectionPool) {
         this.canceled = true;
         codecToCancel = this.codec;
         connectionToCancel = this.connection;
      }

      if (codecToCancel != null) {
         codecToCancel.cancel();
      } else if (connectionToCancel != null) {
         connectionToCancel.cancel();
      }

   }

   public void streamFailed(IOException e) {
      boolean noNewStreams = false;
      Socket socket;
      RealConnection releasedConnection;
      synchronized(this.connectionPool) {
         if (e instanceof StreamResetException) {
            ErrorCode errorCode = ((StreamResetException)e).errorCode;
            if (errorCode == ErrorCode.REFUSED_STREAM) {
               ++this.refusedStreamCount;
               if (this.refusedStreamCount > 1) {
                  noNewStreams = true;
                  this.route = null;
               }
            } else if (errorCode != ErrorCode.CANCEL) {
               noNewStreams = true;
               this.route = null;
            }
         } else if (this.connection != null && (!this.connection.isMultiplexed() || e instanceof ConnectionShutdownException)) {
            noNewStreams = true;
            if (this.connection.successCount == 0) {
               if (this.route != null && e != null) {
                  this.routeSelector.connectFailed(this.route, e);
               }

               this.route = null;
            }
         }

         releasedConnection = this.connection;
         socket = this.deallocate(noNewStreams, false, true);
         if (this.connection != null || !this.reportedAcquired) {
            releasedConnection = null;
         }
      }

      Util.closeQuietly(socket);
      if (releasedConnection != null) {
         this.eventListener.connectionReleased(this.call, releasedConnection);
      }

   }

   public void acquire(RealConnection connection, boolean reportedAcquired) {
      assert Thread.holdsLock(this.connectionPool);

      if (this.connection != null) {
         throw new IllegalStateException();
      } else {
         this.connection = connection;
         this.reportedAcquired = reportedAcquired;
         connection.allocations.add(new StreamAllocation.StreamAllocationReference(this, this.callStackTrace));
      }
   }

   private void release(RealConnection connection) {
      int i = 0;

      for(int size = connection.allocations.size(); i < size; ++i) {
         Reference<StreamAllocation> reference = (Reference)connection.allocations.get(i);
         if (reference.get() == this) {
            connection.allocations.remove(i);
            return;
         }
      }

      throw new IllegalStateException();
   }

   public Socket releaseAndAcquire(RealConnection newConnection) {
      assert Thread.holdsLock(this.connectionPool);

      if (this.codec == null && this.connection.allocations.size() == 1) {
         Reference<StreamAllocation> onlyAllocation = (Reference)this.connection.allocations.get(0);
         Socket socket = this.deallocate(true, false, false);
         this.connection = newConnection;
         newConnection.allocations.add(onlyAllocation);
         return socket;
      } else {
         throw new IllegalStateException();
      }
   }

   public boolean hasMoreRoutes() {
      return this.route != null || this.routeSelection != null && this.routeSelection.hasNext() || this.routeSelector.hasNext();
   }

   public String toString() {
      RealConnection connection = this.connection();
      return connection != null ? connection.toString() : this.address.toString();
   }

   public static final class StreamAllocationReference extends WeakReference<StreamAllocation> {
      public final Object callStackTrace;

      StreamAllocationReference(StreamAllocation referent, Object callStackTrace) {
         super(referent);
         this.callStackTrace = callStackTrace;
      }
   }
}

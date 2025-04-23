package okhttp3;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import okhttp3.internal.NamedRunnable;
import okhttp3.internal.cache.CacheInterceptor;
import okhttp3.internal.connection.ConnectInterceptor;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.StreamAllocation;
import okhttp3.internal.http.BridgeInterceptor;
import okhttp3.internal.http.CallServerInterceptor;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http.RealInterceptorChain;
import okhttp3.internal.http.RetryAndFollowUpInterceptor;
import okhttp3.internal.platform.Platform;
import okio.AsyncTimeout;
import okio.Timeout;

final class RealCall implements Call {
   final OkHttpClient client;
   final RetryAndFollowUpInterceptor retryAndFollowUpInterceptor;
   final AsyncTimeout timeout;
   @Nullable
   private EventListener eventListener;
   final Request originalRequest;
   final boolean forWebSocket;
   private boolean executed;

   private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
      this.client = client;
      this.originalRequest = originalRequest;
      this.forWebSocket = forWebSocket;
      this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client);
      this.timeout = new AsyncTimeout() {
         protected void timedOut() {
            RealCall.this.cancel();
         }
      };
      this.timeout.timeout((long)client.callTimeoutMillis(), TimeUnit.MILLISECONDS);
   }

   static RealCall newRealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
      RealCall call = new RealCall(client, originalRequest, forWebSocket);
      call.eventListener = client.eventListenerFactory().create(call);
      return call;
   }

   public Request request() {
      return this.originalRequest;
   }

   public Response execute() throws IOException {
      synchronized(this) {
         if (this.executed) {
            throw new IllegalStateException("Already Executed");
         }

         this.executed = true;
      }

      this.captureCallStackTrace();
      this.timeout.enter();
      this.eventListener.callStart(this);

      Response var2;
      try {
         this.client.dispatcher().executed(this);
         Response result = this.getResponseWithInterceptorChain();
         if (result == null) {
            throw new IOException("Canceled");
         }

         var2 = result;
      } catch (IOException var7) {
         IOException e = this.timeoutExit(var7);
         this.eventListener.callFailed(this, e);
         throw e;
      } finally {
         this.client.dispatcher().finished(this);
      }

      return var2;
   }

   @Nullable
   IOException timeoutExit(@Nullable IOException cause) {
      if (!this.timeout.exit()) {
         return cause;
      } else {
         InterruptedIOException e = new InterruptedIOException("timeout");
         if (cause != null) {
            e.initCause(cause);
         }

         return e;
      }
   }

   private void captureCallStackTrace() {
      Object callStackTrace = Platform.get().getStackTraceForCloseable("response.body().close()");
      this.retryAndFollowUpInterceptor.setCallStackTrace(callStackTrace);
   }

   public void enqueue(Callback responseCallback) {
      synchronized(this) {
         if (this.executed) {
            throw new IllegalStateException("Already Executed");
         }

         this.executed = true;
      }

      this.captureCallStackTrace();
      this.eventListener.callStart(this);
      this.client.dispatcher().enqueue(new RealCall.AsyncCall(responseCallback));
   }

   public void cancel() {
      this.retryAndFollowUpInterceptor.cancel();
   }

   public Timeout timeout() {
      return this.timeout;
   }

   public synchronized boolean isExecuted() {
      return this.executed;
   }

   public boolean isCanceled() {
      return this.retryAndFollowUpInterceptor.isCanceled();
   }

   public RealCall clone() {
      return newRealCall(this.client, this.originalRequest, this.forWebSocket);
   }

   StreamAllocation streamAllocation() {
      return this.retryAndFollowUpInterceptor.streamAllocation();
   }

   String toLoggableString() {
      return (this.isCanceled() ? "canceled " : "") + (this.forWebSocket ? "web socket" : "call") + " to " + this.redactedUrl();
   }

   String redactedUrl() {
      return this.originalRequest.url().redact();
   }

   Response getResponseWithInterceptorChain() throws IOException {
      List<Interceptor> interceptors = new ArrayList();
      interceptors.addAll(this.client.interceptors());
      interceptors.add(this.retryAndFollowUpInterceptor);
      interceptors.add(new BridgeInterceptor(this.client.cookieJar()));
      interceptors.add(new CacheInterceptor(this.client.internalCache()));
      interceptors.add(new ConnectInterceptor(this.client));
      if (!this.forWebSocket) {
         interceptors.addAll(this.client.networkInterceptors());
      }

      interceptors.add(new CallServerInterceptor(this.forWebSocket));
      Interceptor.Chain chain = new RealInterceptorChain(interceptors, (StreamAllocation)null, (HttpCodec)null, (RealConnection)null, 0, this.originalRequest, this, this.eventListener, this.client.connectTimeoutMillis(), this.client.readTimeoutMillis(), this.client.writeTimeoutMillis());
      return chain.proceed(this.originalRequest);
   }

   final class AsyncCall extends NamedRunnable {
      private final Callback responseCallback;
      private volatile AtomicInteger callsPerHost = new AtomicInteger(0);

      AsyncCall(Callback responseCallback) {
         super("OkHttp %s", RealCall.this.redactedUrl());
         this.responseCallback = responseCallback;
      }

      AtomicInteger callsPerHost() {
         return this.callsPerHost;
      }

      void reuseCallsPerHostFrom(RealCall.AsyncCall other) {
         this.callsPerHost = other.callsPerHost;
      }

      String host() {
         return RealCall.this.originalRequest.url().host();
      }

      Request request() {
         return RealCall.this.originalRequest;
      }

      RealCall get() {
         return RealCall.this;
      }

      void executeOn(ExecutorService executorService) {
         assert !Thread.holdsLock(RealCall.this.client.dispatcher());

         boolean success = false;

         try {
            executorService.execute(this);
            success = true;
         } catch (RejectedExecutionException var8) {
            InterruptedIOException ioException = new InterruptedIOException("executor rejected");
            ioException.initCause(var8);
            RealCall.this.eventListener.callFailed(RealCall.this, ioException);
            this.responseCallback.onFailure(RealCall.this, ioException);
         } finally {
            if (!success) {
               RealCall.this.client.dispatcher().finished(this);
            }

         }

      }

      protected void execute() {
         boolean signalledCallback = false;
         RealCall.this.timeout.enter();

         try {
            Response response = RealCall.this.getResponseWithInterceptorChain();
            if (RealCall.this.retryAndFollowUpInterceptor.isCanceled()) {
               signalledCallback = true;
               this.responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
            } else {
               signalledCallback = true;
               this.responseCallback.onResponse(RealCall.this, response);
            }
         } catch (IOException var6) {
            IOException e = RealCall.this.timeoutExit(var6);
            if (signalledCallback) {
               Platform.get().log(4, "Callback failure for " + RealCall.this.toLoggableString(), e);
            } else {
               RealCall.this.eventListener.callFailed(RealCall.this, e);
               this.responseCallback.onFailure(RealCall.this, e);
            }
         } finally {
            RealCall.this.client.dispatcher().finished(this);
         }

      }
   }
}

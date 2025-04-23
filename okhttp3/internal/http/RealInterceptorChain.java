package okhttp3.internal.http;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.StreamAllocation;

public final class RealInterceptorChain implements Interceptor.Chain {
   private final List<Interceptor> interceptors;
   private final StreamAllocation streamAllocation;
   private final HttpCodec httpCodec;
   private final RealConnection connection;
   private final int index;
   private final Request request;
   private final Call call;
   private final EventListener eventListener;
   private final int connectTimeout;
   private final int readTimeout;
   private final int writeTimeout;
   private int calls;

   public RealInterceptorChain(List<Interceptor> interceptors, StreamAllocation streamAllocation, HttpCodec httpCodec, RealConnection connection, int index, Request request, Call call, EventListener eventListener, int connectTimeout, int readTimeout, int writeTimeout) {
      this.interceptors = interceptors;
      this.connection = connection;
      this.streamAllocation = streamAllocation;
      this.httpCodec = httpCodec;
      this.index = index;
      this.request = request;
      this.call = call;
      this.eventListener = eventListener;
      this.connectTimeout = connectTimeout;
      this.readTimeout = readTimeout;
      this.writeTimeout = writeTimeout;
   }

   public Connection connection() {
      return this.connection;
   }

   public int connectTimeoutMillis() {
      return this.connectTimeout;
   }

   public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
      int millis = Util.checkDuration("timeout", (long)timeout, unit);
      return new RealInterceptorChain(this.interceptors, this.streamAllocation, this.httpCodec, this.connection, this.index, this.request, this.call, this.eventListener, millis, this.readTimeout, this.writeTimeout);
   }

   public int readTimeoutMillis() {
      return this.readTimeout;
   }

   public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
      int millis = Util.checkDuration("timeout", (long)timeout, unit);
      return new RealInterceptorChain(this.interceptors, this.streamAllocation, this.httpCodec, this.connection, this.index, this.request, this.call, this.eventListener, this.connectTimeout, millis, this.writeTimeout);
   }

   public int writeTimeoutMillis() {
      return this.writeTimeout;
   }

   public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
      int millis = Util.checkDuration("timeout", (long)timeout, unit);
      return new RealInterceptorChain(this.interceptors, this.streamAllocation, this.httpCodec, this.connection, this.index, this.request, this.call, this.eventListener, this.connectTimeout, this.readTimeout, millis);
   }

   public StreamAllocation streamAllocation() {
      return this.streamAllocation;
   }

   public HttpCodec httpStream() {
      return this.httpCodec;
   }

   public Call call() {
      return this.call;
   }

   public EventListener eventListener() {
      return this.eventListener;
   }

   public Request request() {
      return this.request;
   }

   public Response proceed(Request request) throws IOException {
      return this.proceed(request, this.streamAllocation, this.httpCodec, this.connection);
   }

   public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec, RealConnection connection) throws IOException {
      if (this.index >= this.interceptors.size()) {
         throw new AssertionError();
      } else {
         ++this.calls;
         if (this.httpCodec != null && !this.connection.supportsUrl(request.url())) {
            throw new IllegalStateException("network interceptor " + this.interceptors.get(this.index - 1) + " must retain the same host and port");
         } else if (this.httpCodec != null && this.calls > 1) {
            throw new IllegalStateException("network interceptor " + this.interceptors.get(this.index - 1) + " must call proceed() exactly once");
         } else {
            RealInterceptorChain next = new RealInterceptorChain(this.interceptors, streamAllocation, httpCodec, connection, this.index + 1, request, this.call, this.eventListener, this.connectTimeout, this.readTimeout, this.writeTimeout);
            Interceptor interceptor = (Interceptor)this.interceptors.get(this.index);
            Response response = interceptor.intercept(next);
            if (httpCodec != null && this.index + 1 < this.interceptors.size() && next.calls != 1) {
               throw new IllegalStateException("network interceptor " + interceptor + " must call proceed() exactly once");
            } else if (response == null) {
               throw new NullPointerException("interceptor " + interceptor + " returned null");
            } else if (response.body() == null) {
               throw new IllegalStateException("interceptor " + interceptor + " returned a response with no body");
            } else {
               return response;
            }
         }
      }
   }
}

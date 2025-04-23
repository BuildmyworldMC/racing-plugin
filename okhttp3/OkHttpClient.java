package okhttp3;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.internal.Internal;
import okhttp3.internal.Util;
import okhttp3.internal.cache.InternalCache;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.connection.RouteDatabase;
import okhttp3.internal.connection.StreamAllocation;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.platform.Platform;
import okhttp3.internal.proxy.NullProxySelector;
import okhttp3.internal.tls.CertificateChainCleaner;
import okhttp3.internal.tls.OkHostnameVerifier;
import okhttp3.internal.ws.RealWebSocket;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

public class OkHttpClient implements Cloneable, Call.Factory, WebSocket.Factory {
   static final List<Protocol> DEFAULT_PROTOCOLS;
   static final List<ConnectionSpec> DEFAULT_CONNECTION_SPECS;
   final Dispatcher dispatcher;
   @Nullable
   final Proxy proxy;
   final List<Protocol> protocols;
   final List<ConnectionSpec> connectionSpecs;
   final List<Interceptor> interceptors;
   final List<Interceptor> networkInterceptors;
   final EventListener.Factory eventListenerFactory;
   final ProxySelector proxySelector;
   final CookieJar cookieJar;
   @Nullable
   final Cache cache;
   @Nullable
   final InternalCache internalCache;
   final SocketFactory socketFactory;
   final SSLSocketFactory sslSocketFactory;
   final CertificateChainCleaner certificateChainCleaner;
   final HostnameVerifier hostnameVerifier;
   final CertificatePinner certificatePinner;
   final Authenticator proxyAuthenticator;
   final Authenticator authenticator;
   final ConnectionPool connectionPool;
   final Dns dns;
   final boolean followSslRedirects;
   final boolean followRedirects;
   final boolean retryOnConnectionFailure;
   final int callTimeout;
   final int connectTimeout;
   final int readTimeout;
   final int writeTimeout;
   final int pingInterval;

   public OkHttpClient() {
      this(new OkHttpClient.Builder());
   }

   OkHttpClient(OkHttpClient.Builder builder) {
      this.dispatcher = builder.dispatcher;
      this.proxy = builder.proxy;
      this.protocols = builder.protocols;
      this.connectionSpecs = builder.connectionSpecs;
      this.interceptors = Util.immutableList(builder.interceptors);
      this.networkInterceptors = Util.immutableList(builder.networkInterceptors);
      this.eventListenerFactory = builder.eventListenerFactory;
      this.proxySelector = builder.proxySelector;
      this.cookieJar = builder.cookieJar;
      this.cache = builder.cache;
      this.internalCache = builder.internalCache;
      this.socketFactory = builder.socketFactory;
      boolean isTLS = false;

      ConnectionSpec spec;
      for(Iterator var3 = this.connectionSpecs.iterator(); var3.hasNext(); isTLS = isTLS || spec.isTls()) {
         spec = (ConnectionSpec)var3.next();
      }

      if (builder.sslSocketFactory == null && isTLS) {
         X509TrustManager trustManager = Util.platformTrustManager();
         this.sslSocketFactory = newSslSocketFactory(trustManager);
         this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
      } else {
         this.sslSocketFactory = builder.sslSocketFactory;
         this.certificateChainCleaner = builder.certificateChainCleaner;
      }

      if (this.sslSocketFactory != null) {
         Platform.get().configureSslSocketFactory(this.sslSocketFactory);
      }

      this.hostnameVerifier = builder.hostnameVerifier;
      this.certificatePinner = builder.certificatePinner.withCertificateChainCleaner(this.certificateChainCleaner);
      this.proxyAuthenticator = builder.proxyAuthenticator;
      this.authenticator = builder.authenticator;
      this.connectionPool = builder.connectionPool;
      this.dns = builder.dns;
      this.followSslRedirects = builder.followSslRedirects;
      this.followRedirects = builder.followRedirects;
      this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
      this.callTimeout = builder.callTimeout;
      this.connectTimeout = builder.connectTimeout;
      this.readTimeout = builder.readTimeout;
      this.writeTimeout = builder.writeTimeout;
      this.pingInterval = builder.pingInterval;
      if (this.interceptors.contains((Object)null)) {
         throw new IllegalStateException("Null interceptor: " + this.interceptors);
      } else if (this.networkInterceptors.contains((Object)null)) {
         throw new IllegalStateException("Null network interceptor: " + this.networkInterceptors);
      }
   }

   private static SSLSocketFactory newSslSocketFactory(X509TrustManager trustManager) {
      try {
         SSLContext sslContext = Platform.get().getSSLContext();
         sslContext.init((KeyManager[])null, new TrustManager[]{trustManager}, (SecureRandom)null);
         return sslContext.getSocketFactory();
      } catch (GeneralSecurityException var2) {
         throw new AssertionError("No System TLS", var2);
      }
   }

   public int callTimeoutMillis() {
      return this.callTimeout;
   }

   public int connectTimeoutMillis() {
      return this.connectTimeout;
   }

   public int readTimeoutMillis() {
      return this.readTimeout;
   }

   public int writeTimeoutMillis() {
      return this.writeTimeout;
   }

   public int pingIntervalMillis() {
      return this.pingInterval;
   }

   @Nullable
   public Proxy proxy() {
      return this.proxy;
   }

   public ProxySelector proxySelector() {
      return this.proxySelector;
   }

   public CookieJar cookieJar() {
      return this.cookieJar;
   }

   @Nullable
   public Cache cache() {
      return this.cache;
   }

   @Nullable
   InternalCache internalCache() {
      return this.cache != null ? this.cache.internalCache : this.internalCache;
   }

   public Dns dns() {
      return this.dns;
   }

   public SocketFactory socketFactory() {
      return this.socketFactory;
   }

   public SSLSocketFactory sslSocketFactory() {
      return this.sslSocketFactory;
   }

   public HostnameVerifier hostnameVerifier() {
      return this.hostnameVerifier;
   }

   public CertificatePinner certificatePinner() {
      return this.certificatePinner;
   }

   public Authenticator authenticator() {
      return this.authenticator;
   }

   public Authenticator proxyAuthenticator() {
      return this.proxyAuthenticator;
   }

   public ConnectionPool connectionPool() {
      return this.connectionPool;
   }

   public boolean followSslRedirects() {
      return this.followSslRedirects;
   }

   public boolean followRedirects() {
      return this.followRedirects;
   }

   public boolean retryOnConnectionFailure() {
      return this.retryOnConnectionFailure;
   }

   public Dispatcher dispatcher() {
      return this.dispatcher;
   }

   public List<Protocol> protocols() {
      return this.protocols;
   }

   public List<ConnectionSpec> connectionSpecs() {
      return this.connectionSpecs;
   }

   public List<Interceptor> interceptors() {
      return this.interceptors;
   }

   public List<Interceptor> networkInterceptors() {
      return this.networkInterceptors;
   }

   public EventListener.Factory eventListenerFactory() {
      return this.eventListenerFactory;
   }

   public Call newCall(Request request) {
      return RealCall.newRealCall(this, request, false);
   }

   public WebSocket newWebSocket(Request request, WebSocketListener listener) {
      RealWebSocket webSocket = new RealWebSocket(request, listener, new Random(), (long)this.pingInterval);
      webSocket.connect(this);
      return webSocket;
   }

   public OkHttpClient.Builder newBuilder() {
      return new OkHttpClient.Builder(this);
   }

   static {
      DEFAULT_PROTOCOLS = Util.immutableList((Object[])(Protocol.HTTP_2, Protocol.HTTP_1_1));
      DEFAULT_CONNECTION_SPECS = Util.immutableList((Object[])(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT));
      Internal.instance = new Internal() {
         public void addLenient(Headers.Builder builder, String line) {
            builder.addLenient(line);
         }

         public void addLenient(Headers.Builder builder, String name, String value) {
            builder.addLenient(name, value);
         }

         public void setCache(OkHttpClient.Builder builder, InternalCache internalCache) {
            builder.setInternalCache(internalCache);
         }

         public boolean connectionBecameIdle(ConnectionPool pool, RealConnection connection) {
            return pool.connectionBecameIdle(connection);
         }

         public void acquire(ConnectionPool pool, Address address, StreamAllocation streamAllocation, @Nullable Route route) {
            pool.acquire(address, streamAllocation, route);
         }

         public boolean equalsNonHost(Address a, Address b) {
            return a.equalsNonHost(b);
         }

         @Nullable
         public Socket deduplicate(ConnectionPool pool, Address address, StreamAllocation streamAllocation) {
            return pool.deduplicate(address, streamAllocation);
         }

         public void put(ConnectionPool pool, RealConnection connection) {
            pool.put(connection);
         }

         public RouteDatabase routeDatabase(ConnectionPool connectionPool) {
            return connectionPool.routeDatabase;
         }

         public int code(Response.Builder responseBuilder) {
            return responseBuilder.code;
         }

         public void apply(ConnectionSpec tlsConfiguration, SSLSocket sslSocket, boolean isFallback) {
            tlsConfiguration.apply(sslSocket, isFallback);
         }

         public boolean isInvalidHttpUrlHost(IllegalArgumentException e) {
            return e.getMessage().startsWith("Invalid URL host");
         }

         public StreamAllocation streamAllocation(Call call) {
            return ((RealCall)call).streamAllocation();
         }

         @Nullable
         public IOException timeoutExit(Call call, @Nullable IOException e) {
            return ((RealCall)call).timeoutExit(e);
         }

         public Call newWebSocketCall(OkHttpClient client, Request originalRequest) {
            return RealCall.newRealCall(client, originalRequest, true);
         }

         public void initCodec(Response.Builder responseBuilder, HttpCodec httpCodec) {
            responseBuilder.initCodec(httpCodec);
         }
      };
   }

   public static final class Builder {
      Dispatcher dispatcher;
      @Nullable
      Proxy proxy;
      List<Protocol> protocols;
      List<ConnectionSpec> connectionSpecs;
      final List<Interceptor> interceptors = new ArrayList();
      final List<Interceptor> networkInterceptors = new ArrayList();
      EventListener.Factory eventListenerFactory;
      ProxySelector proxySelector;
      CookieJar cookieJar;
      @Nullable
      Cache cache;
      @Nullable
      InternalCache internalCache;
      SocketFactory socketFactory;
      @Nullable
      SSLSocketFactory sslSocketFactory;
      @Nullable
      CertificateChainCleaner certificateChainCleaner;
      HostnameVerifier hostnameVerifier;
      CertificatePinner certificatePinner;
      Authenticator proxyAuthenticator;
      Authenticator authenticator;
      ConnectionPool connectionPool;
      Dns dns;
      boolean followSslRedirects;
      boolean followRedirects;
      boolean retryOnConnectionFailure;
      int callTimeout;
      int connectTimeout;
      int readTimeout;
      int writeTimeout;
      int pingInterval;

      public Builder() {
         this.dispatcher = new Dispatcher();
         this.protocols = OkHttpClient.DEFAULT_PROTOCOLS;
         this.connectionSpecs = OkHttpClient.DEFAULT_CONNECTION_SPECS;
         this.eventListenerFactory = EventListener.factory(EventListener.NONE);
         this.proxySelector = ProxySelector.getDefault();
         if (this.proxySelector == null) {
            this.proxySelector = new NullProxySelector();
         }

         this.cookieJar = CookieJar.NO_COOKIES;
         this.socketFactory = SocketFactory.getDefault();
         this.hostnameVerifier = OkHostnameVerifier.INSTANCE;
         this.certificatePinner = CertificatePinner.DEFAULT;
         this.proxyAuthenticator = Authenticator.NONE;
         this.authenticator = Authenticator.NONE;
         this.connectionPool = new ConnectionPool();
         this.dns = Dns.SYSTEM;
         this.followSslRedirects = true;
         this.followRedirects = true;
         this.retryOnConnectionFailure = true;
         this.callTimeout = 0;
         this.connectTimeout = 10000;
         this.readTimeout = 10000;
         this.writeTimeout = 10000;
         this.pingInterval = 0;
      }

      Builder(OkHttpClient okHttpClient) {
         this.dispatcher = okHttpClient.dispatcher;
         this.proxy = okHttpClient.proxy;
         this.protocols = okHttpClient.protocols;
         this.connectionSpecs = okHttpClient.connectionSpecs;
         this.interceptors.addAll(okHttpClient.interceptors);
         this.networkInterceptors.addAll(okHttpClient.networkInterceptors);
         this.eventListenerFactory = okHttpClient.eventListenerFactory;
         this.proxySelector = okHttpClient.proxySelector;
         this.cookieJar = okHttpClient.cookieJar;
         this.internalCache = okHttpClient.internalCache;
         this.cache = okHttpClient.cache;
         this.socketFactory = okHttpClient.socketFactory;
         this.sslSocketFactory = okHttpClient.sslSocketFactory;
         this.certificateChainCleaner = okHttpClient.certificateChainCleaner;
         this.hostnameVerifier = okHttpClient.hostnameVerifier;
         this.certificatePinner = okHttpClient.certificatePinner;
         this.proxyAuthenticator = okHttpClient.proxyAuthenticator;
         this.authenticator = okHttpClient.authenticator;
         this.connectionPool = okHttpClient.connectionPool;
         this.dns = okHttpClient.dns;
         this.followSslRedirects = okHttpClient.followSslRedirects;
         this.followRedirects = okHttpClient.followRedirects;
         this.retryOnConnectionFailure = okHttpClient.retryOnConnectionFailure;
         this.callTimeout = okHttpClient.callTimeout;
         this.connectTimeout = okHttpClient.connectTimeout;
         this.readTimeout = okHttpClient.readTimeout;
         this.writeTimeout = okHttpClient.writeTimeout;
         this.pingInterval = okHttpClient.pingInterval;
      }

      public OkHttpClient.Builder callTimeout(long timeout, TimeUnit unit) {
         this.callTimeout = Util.checkDuration("timeout", timeout, unit);
         return this;
      }

      @IgnoreJRERequirement
      public OkHttpClient.Builder callTimeout(Duration duration) {
         this.callTimeout = Util.checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
         return this;
      }

      public OkHttpClient.Builder connectTimeout(long timeout, TimeUnit unit) {
         this.connectTimeout = Util.checkDuration("timeout", timeout, unit);
         return this;
      }

      @IgnoreJRERequirement
      public OkHttpClient.Builder connectTimeout(Duration duration) {
         this.connectTimeout = Util.checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
         return this;
      }

      public OkHttpClient.Builder readTimeout(long timeout, TimeUnit unit) {
         this.readTimeout = Util.checkDuration("timeout", timeout, unit);
         return this;
      }

      @IgnoreJRERequirement
      public OkHttpClient.Builder readTimeout(Duration duration) {
         this.readTimeout = Util.checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
         return this;
      }

      public OkHttpClient.Builder writeTimeout(long timeout, TimeUnit unit) {
         this.writeTimeout = Util.checkDuration("timeout", timeout, unit);
         return this;
      }

      @IgnoreJRERequirement
      public OkHttpClient.Builder writeTimeout(Duration duration) {
         this.writeTimeout = Util.checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
         return this;
      }

      public OkHttpClient.Builder pingInterval(long interval, TimeUnit unit) {
         this.pingInterval = Util.checkDuration("interval", interval, unit);
         return this;
      }

      @IgnoreJRERequirement
      public OkHttpClient.Builder pingInterval(Duration duration) {
         this.pingInterval = Util.checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
         return this;
      }

      public OkHttpClient.Builder proxy(@Nullable Proxy proxy) {
         this.proxy = proxy;
         return this;
      }

      public OkHttpClient.Builder proxySelector(ProxySelector proxySelector) {
         if (proxySelector == null) {
            throw new NullPointerException("proxySelector == null");
         } else {
            this.proxySelector = proxySelector;
            return this;
         }
      }

      public OkHttpClient.Builder cookieJar(CookieJar cookieJar) {
         if (cookieJar == null) {
            throw new NullPointerException("cookieJar == null");
         } else {
            this.cookieJar = cookieJar;
            return this;
         }
      }

      void setInternalCache(@Nullable InternalCache internalCache) {
         this.internalCache = internalCache;
         this.cache = null;
      }

      public OkHttpClient.Builder cache(@Nullable Cache cache) {
         this.cache = cache;
         this.internalCache = null;
         return this;
      }

      public OkHttpClient.Builder dns(Dns dns) {
         if (dns == null) {
            throw new NullPointerException("dns == null");
         } else {
            this.dns = dns;
            return this;
         }
      }

      public OkHttpClient.Builder socketFactory(SocketFactory socketFactory) {
         if (socketFactory == null) {
            throw new NullPointerException("socketFactory == null");
         } else if (socketFactory instanceof SSLSocketFactory) {
            throw new IllegalArgumentException("socketFactory instanceof SSLSocketFactory");
         } else {
            this.socketFactory = socketFactory;
            return this;
         }
      }

      /** @deprecated */
      public OkHttpClient.Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
         if (sslSocketFactory == null) {
            throw new NullPointerException("sslSocketFactory == null");
         } else {
            this.sslSocketFactory = sslSocketFactory;
            this.certificateChainCleaner = Platform.get().buildCertificateChainCleaner(sslSocketFactory);
            return this;
         }
      }

      public OkHttpClient.Builder sslSocketFactory(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
         if (sslSocketFactory == null) {
            throw new NullPointerException("sslSocketFactory == null");
         } else if (trustManager == null) {
            throw new NullPointerException("trustManager == null");
         } else {
            this.sslSocketFactory = sslSocketFactory;
            this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
            return this;
         }
      }

      public OkHttpClient.Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
         if (hostnameVerifier == null) {
            throw new NullPointerException("hostnameVerifier == null");
         } else {
            this.hostnameVerifier = hostnameVerifier;
            return this;
         }
      }

      public OkHttpClient.Builder certificatePinner(CertificatePinner certificatePinner) {
         if (certificatePinner == null) {
            throw new NullPointerException("certificatePinner == null");
         } else {
            this.certificatePinner = certificatePinner;
            return this;
         }
      }

      public OkHttpClient.Builder authenticator(Authenticator authenticator) {
         if (authenticator == null) {
            throw new NullPointerException("authenticator == null");
         } else {
            this.authenticator = authenticator;
            return this;
         }
      }

      public OkHttpClient.Builder proxyAuthenticator(Authenticator proxyAuthenticator) {
         if (proxyAuthenticator == null) {
            throw new NullPointerException("proxyAuthenticator == null");
         } else {
            this.proxyAuthenticator = proxyAuthenticator;
            return this;
         }
      }

      public OkHttpClient.Builder connectionPool(ConnectionPool connectionPool) {
         if (connectionPool == null) {
            throw new NullPointerException("connectionPool == null");
         } else {
            this.connectionPool = connectionPool;
            return this;
         }
      }

      public OkHttpClient.Builder followSslRedirects(boolean followProtocolRedirects) {
         this.followSslRedirects = followProtocolRedirects;
         return this;
      }

      public OkHttpClient.Builder followRedirects(boolean followRedirects) {
         this.followRedirects = followRedirects;
         return this;
      }

      public OkHttpClient.Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
         this.retryOnConnectionFailure = retryOnConnectionFailure;
         return this;
      }

      public OkHttpClient.Builder dispatcher(Dispatcher dispatcher) {
         if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher == null");
         } else {
            this.dispatcher = dispatcher;
            return this;
         }
      }

      public OkHttpClient.Builder protocols(List<Protocol> protocols) {
         List<Protocol> protocols = new ArrayList(protocols);
         if (!protocols.contains(Protocol.H2_PRIOR_KNOWLEDGE) && !protocols.contains(Protocol.HTTP_1_1)) {
            throw new IllegalArgumentException("protocols must contain h2_prior_knowledge or http/1.1: " + protocols);
         } else if (protocols.contains(Protocol.H2_PRIOR_KNOWLEDGE) && protocols.size() > 1) {
            throw new IllegalArgumentException("protocols containing h2_prior_knowledge cannot use other protocols: " + protocols);
         } else if (protocols.contains(Protocol.HTTP_1_0)) {
            throw new IllegalArgumentException("protocols must not contain http/1.0: " + protocols);
         } else if (protocols.contains((Object)null)) {
            throw new IllegalArgumentException("protocols must not contain null");
         } else {
            protocols.remove(Protocol.SPDY_3);
            this.protocols = Collections.unmodifiableList(protocols);
            return this;
         }
      }

      public OkHttpClient.Builder connectionSpecs(List<ConnectionSpec> connectionSpecs) {
         this.connectionSpecs = Util.immutableList(connectionSpecs);
         return this;
      }

      public List<Interceptor> interceptors() {
         return this.interceptors;
      }

      public OkHttpClient.Builder addInterceptor(Interceptor interceptor) {
         if (interceptor == null) {
            throw new IllegalArgumentException("interceptor == null");
         } else {
            this.interceptors.add(interceptor);
            return this;
         }
      }

      public List<Interceptor> networkInterceptors() {
         return this.networkInterceptors;
      }

      public OkHttpClient.Builder addNetworkInterceptor(Interceptor interceptor) {
         if (interceptor == null) {
            throw new IllegalArgumentException("interceptor == null");
         } else {
            this.networkInterceptors.add(interceptor);
            return this;
         }
      }

      public OkHttpClient.Builder eventListener(EventListener eventListener) {
         if (eventListener == null) {
            throw new NullPointerException("eventListener == null");
         } else {
            this.eventListenerFactory = EventListener.factory(eventListener);
            return this;
         }
      }

      public OkHttpClient.Builder eventListenerFactory(EventListener.Factory eventListenerFactory) {
         if (eventListenerFactory == null) {
            throw new NullPointerException("eventListenerFactory == null");
         } else {
            this.eventListenerFactory = eventListenerFactory;
            return this;
         }
      }

      public OkHttpClient build() {
         return new OkHttpClient(this);
      }
   }
}

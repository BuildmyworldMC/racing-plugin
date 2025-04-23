package okhttp3.internal.platform;

import android.os.Build.VERSION;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Protocol;
import okhttp3.internal.Util;
import okhttp3.internal.tls.CertificateChainCleaner;

class AndroidPlatform extends Platform {
   private static final int MAX_LOG_LENGTH = 4000;
   private final Class<?> sslParametersClass;
   private final Method setUseSessionTickets;
   private final Method setHostname;
   private final Method getAlpnSelectedProtocol;
   private final Method setAlpnProtocols;
   private final AndroidPlatform.CloseGuard closeGuard = AndroidPlatform.CloseGuard.get();

   AndroidPlatform(Class<?> sslParametersClass, Method setUseSessionTickets, Method setHostname, Method getAlpnSelectedProtocol, Method setAlpnProtocols) {
      this.sslParametersClass = sslParametersClass;
      this.setUseSessionTickets = setUseSessionTickets;
      this.setHostname = setHostname;
      this.getAlpnSelectedProtocol = getAlpnSelectedProtocol;
      this.setAlpnProtocols = setAlpnProtocols;
   }

   public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
      try {
         socket.connect(address, connectTimeout);
      } catch (AssertionError var5) {
         if (Util.isAndroidGetsocknameError(var5)) {
            throw new IOException(var5);
         } else {
            throw var5;
         }
      } catch (ClassCastException var6) {
         if (VERSION.SDK_INT == 26) {
            throw new IOException("Exception in connect", var6);
         } else {
            throw var6;
         }
      }
   }

   @Nullable
   protected X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
      Object context = readFieldOrNull(sslSocketFactory, this.sslParametersClass, "sslParameters");
      if (context == null) {
         try {
            Class<?> gmsSslParametersClass = Class.forName("com.google.android.gms.org.conscrypt.SSLParametersImpl", false, sslSocketFactory.getClass().getClassLoader());
            context = readFieldOrNull(sslSocketFactory, gmsSslParametersClass, "sslParameters");
         } catch (ClassNotFoundException var4) {
            return super.trustManager(sslSocketFactory);
         }
      }

      X509TrustManager x509TrustManager = (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "x509TrustManager");
      return x509TrustManager != null ? x509TrustManager : (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "trustManager");
   }

   public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
      try {
         if (hostname != null) {
            this.setUseSessionTickets.invoke(sslSocket, true);
            this.setHostname.invoke(sslSocket, hostname);
         }

         this.setAlpnProtocols.invoke(sslSocket, concatLengthPrefixed(protocols));
      } catch (InvocationTargetException | IllegalAccessException var5) {
         throw new AssertionError(var5);
      }
   }

   @Nullable
   public String getSelectedProtocol(SSLSocket socket) {
      try {
         byte[] alpnResult = (byte[])this.getAlpnSelectedProtocol.invoke(socket);
         return alpnResult != null ? new String(alpnResult, StandardCharsets.UTF_8) : null;
      } catch (InvocationTargetException | IllegalAccessException var3) {
         throw new AssertionError(var3);
      }
   }

   public void log(int level, String message, @Nullable Throwable t) {
      int logLevel = level == 5 ? 5 : 3;
      if (t != null) {
         message = message + '\n' + Log.getStackTraceString(t);
      }

      int i = 0;

      int end;
      for(int length = message.length(); i < length; i = end + 1) {
         int newline = message.indexOf(10, i);
         newline = newline != -1 ? newline : length;

         do {
            end = Math.min(newline, i + 4000);
            Log.println(logLevel, "OkHttp", message.substring(i, end));
            i = end;
         } while(end < newline);
      }

   }

   public Object getStackTraceForCloseable(String closer) {
      return this.closeGuard.createAndOpen(closer);
   }

   public void logCloseableLeak(String message, Object stackTrace) {
      boolean reported = this.closeGuard.warnIfOpen(stackTrace);
      if (!reported) {
         this.log(5, message, (Throwable)null);
      }

   }

   public boolean isCleartextTrafficPermitted(String hostname) {
      try {
         Class<?> networkPolicyClass = Class.forName("android.security.NetworkSecurityPolicy");
         Method getInstanceMethod = networkPolicyClass.getMethod("getInstance");
         Object networkSecurityPolicy = getInstanceMethod.invoke((Object)null);
         return this.api24IsCleartextTrafficPermitted(hostname, networkPolicyClass, networkSecurityPolicy);
      } catch (NoSuchMethodException | ClassNotFoundException var5) {
         return super.isCleartextTrafficPermitted(hostname);
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var6) {
         throw new AssertionError("unable to determine cleartext support", var6);
      }
   }

   private boolean api24IsCleartextTrafficPermitted(String hostname, Class<?> networkPolicyClass, Object networkSecurityPolicy) throws InvocationTargetException, IllegalAccessException {
      try {
         Method isCleartextTrafficPermittedMethod = networkPolicyClass.getMethod("isCleartextTrafficPermitted", String.class);
         return (Boolean)isCleartextTrafficPermittedMethod.invoke(networkSecurityPolicy, hostname);
      } catch (NoSuchMethodException var5) {
         return this.api23IsCleartextTrafficPermitted(hostname, networkPolicyClass, networkSecurityPolicy);
      }
   }

   private boolean api23IsCleartextTrafficPermitted(String hostname, Class<?> networkPolicyClass, Object networkSecurityPolicy) throws InvocationTargetException, IllegalAccessException {
      try {
         Method isCleartextTrafficPermittedMethod = networkPolicyClass.getMethod("isCleartextTrafficPermitted");
         return (Boolean)isCleartextTrafficPermittedMethod.invoke(networkSecurityPolicy);
      } catch (NoSuchMethodException var5) {
         return super.isCleartextTrafficPermitted(hostname);
      }
   }

   public CertificateChainCleaner buildCertificateChainCleaner(X509TrustManager trustManager) {
      try {
         Class<?> extensionsClass = Class.forName("android.net.http.X509TrustManagerExtensions");
         Constructor<?> constructor = extensionsClass.getConstructor(X509TrustManager.class);
         Object extensions = constructor.newInstance(trustManager);
         Method checkServerTrusted = extensionsClass.getMethod("checkServerTrusted", X509Certificate[].class, String.class, String.class);
         return new AndroidPlatform.AndroidCertificateChainCleaner(extensions, checkServerTrusted);
      } catch (Exception var6) {
         throw new AssertionError(var6);
      }
   }

   @Nullable
   public static Platform buildIfSupported() {
      Class sslParametersClass;
      Class sslSocketClass;
      try {
         sslParametersClass = Class.forName("com.android.org.conscrypt.SSLParametersImpl");
         sslSocketClass = Class.forName("com.android.org.conscrypt.OpenSSLSocketImpl");
      } catch (ClassNotFoundException var6) {
         return null;
      }

      if (VERSION.SDK_INT >= 21) {
         try {
            Method setUseSessionTickets = sslSocketClass.getDeclaredMethod("setUseSessionTickets", Boolean.TYPE);
            Method setHostname = sslSocketClass.getMethod("setHostname", String.class);
            Method getAlpnSelectedProtocol = sslSocketClass.getMethod("getAlpnSelectedProtocol");
            Method setAlpnProtocols = sslSocketClass.getMethod("setAlpnProtocols", byte[].class);
            return new AndroidPlatform(sslParametersClass, setUseSessionTickets, setHostname, getAlpnSelectedProtocol, setAlpnProtocols);
         } catch (NoSuchMethodException var7) {
         }
      }

      throw new IllegalStateException("Expected Android API level 21+ but was " + VERSION.SDK_INT);
   }

   public SSLContext getSSLContext() {
      boolean tryTls12;
      try {
         tryTls12 = VERSION.SDK_INT >= 16 && VERSION.SDK_INT < 22;
      } catch (NoClassDefFoundError var5) {
         tryTls12 = true;
      }

      if (tryTls12) {
         try {
            return SSLContext.getInstance("TLSv1.2");
         } catch (NoSuchAlgorithmException var4) {
         }
      }

      try {
         return SSLContext.getInstance("TLS");
      } catch (NoSuchAlgorithmException var3) {
         throw new IllegalStateException("No TLS provider", var3);
      }
   }

   static final class CloseGuard {
      private final Method getMethod;
      private final Method openMethod;
      private final Method warnIfOpenMethod;

      CloseGuard(Method getMethod, Method openMethod, Method warnIfOpenMethod) {
         this.getMethod = getMethod;
         this.openMethod = openMethod;
         this.warnIfOpenMethod = warnIfOpenMethod;
      }

      Object createAndOpen(String closer) {
         if (this.getMethod != null) {
            try {
               Object closeGuardInstance = this.getMethod.invoke((Object)null);
               this.openMethod.invoke(closeGuardInstance, closer);
               return closeGuardInstance;
            } catch (Exception var3) {
            }
         }

         return null;
      }

      boolean warnIfOpen(Object closeGuardInstance) {
         boolean reported = false;
         if (closeGuardInstance != null) {
            try {
               this.warnIfOpenMethod.invoke(closeGuardInstance);
               reported = true;
            } catch (Exception var4) {
            }
         }

         return reported;
      }

      static AndroidPlatform.CloseGuard get() {
         Method getMethod;
         Method openMethod;
         Method warnIfOpenMethod;
         try {
            Class<?> closeGuardClass = Class.forName("dalvik.system.CloseGuard");
            getMethod = closeGuardClass.getMethod("get");
            openMethod = closeGuardClass.getMethod("open", String.class);
            warnIfOpenMethod = closeGuardClass.getMethod("warnIfOpen");
         } catch (Exception var4) {
            getMethod = null;
            openMethod = null;
            warnIfOpenMethod = null;
         }

         return new AndroidPlatform.CloseGuard(getMethod, openMethod, warnIfOpenMethod);
      }
   }

   static final class AndroidCertificateChainCleaner extends CertificateChainCleaner {
      private final Object x509TrustManagerExtensions;
      private final Method checkServerTrusted;

      AndroidCertificateChainCleaner(Object x509TrustManagerExtensions, Method checkServerTrusted) {
         this.x509TrustManagerExtensions = x509TrustManagerExtensions;
         this.checkServerTrusted = checkServerTrusted;
      }

      public List<Certificate> clean(List<Certificate> chain, String hostname) throws SSLPeerUnverifiedException {
         try {
            X509Certificate[] certificates = (X509Certificate[])chain.toArray(new X509Certificate[chain.size()]);
            return (List)this.checkServerTrusted.invoke(this.x509TrustManagerExtensions, certificates, "RSA", hostname);
         } catch (InvocationTargetException var5) {
            SSLPeerUnverifiedException exception = new SSLPeerUnverifiedException(var5.getMessage());
            exception.initCause(var5);
            throw exception;
         } catch (IllegalAccessException var6) {
            throw new AssertionError(var6);
         }
      }

      public boolean equals(Object other) {
         return other instanceof AndroidPlatform.AndroidCertificateChainCleaner;
      }

      public int hashCode() {
         return 0;
      }
   }
}

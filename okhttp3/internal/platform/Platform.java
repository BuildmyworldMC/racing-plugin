package okhttp3.internal.platform;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.internal.Util;
import okhttp3.internal.tls.BasicCertificateChainCleaner;
import okhttp3.internal.tls.CertificateChainCleaner;
import okio.Buffer;

public class Platform {
   private static final Platform PLATFORM = findPlatform();
   public static final int INFO = 4;
   public static final int WARN = 5;
   private static final Logger logger = Logger.getLogger(OkHttpClient.class.getName());

   public static Platform get() {
      return PLATFORM;
   }

   public String getPrefix() {
      return "OkHttp";
   }

   @Nullable
   protected X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
      try {
         Class<?> sslContextClass = Class.forName("sun.security.ssl.SSLContextImpl");
         Object context = readFieldOrNull(sslSocketFactory, sslContextClass, "context");
         return context == null ? null : (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "trustManager");
      } catch (ClassNotFoundException var4) {
         return null;
      }
   }

   public void configureTlsExtensions(SSLSocket sslSocket, @Nullable String hostname, List<Protocol> protocols) {
   }

   public void afterHandshake(SSLSocket sslSocket) {
   }

   @Nullable
   public String getSelectedProtocol(SSLSocket socket) {
      return null;
   }

   public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
      socket.connect(address, connectTimeout);
   }

   public void log(int level, String message, @Nullable Throwable t) {
      Level logLevel = level == 5 ? Level.WARNING : Level.INFO;
      logger.log(logLevel, message, t);
   }

   public boolean isCleartextTrafficPermitted(String hostname) {
      return true;
   }

   public Object getStackTraceForCloseable(String closer) {
      return logger.isLoggable(Level.FINE) ? new Throwable(closer) : null;
   }

   public void logCloseableLeak(String message, Object stackTrace) {
      if (stackTrace == null) {
         message = message + " To see where this was allocated, set the OkHttpClient logger level to FINE: Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);";
      }

      this.log(5, message, (Throwable)stackTrace);
   }

   public static List<String> alpnProtocolNames(List<Protocol> protocols) {
      List<String> names = new ArrayList(protocols.size());
      int i = 0;

      for(int size = protocols.size(); i < size; ++i) {
         Protocol protocol = (Protocol)protocols.get(i);
         if (protocol != Protocol.HTTP_1_0) {
            names.add(protocol.toString());
         }
      }

      return names;
   }

   public CertificateChainCleaner buildCertificateChainCleaner(X509TrustManager trustManager) {
      return new BasicCertificateChainCleaner(trustManager.getAcceptedIssuers());
   }

   public CertificateChainCleaner buildCertificateChainCleaner(SSLSocketFactory sslSocketFactory) {
      X509TrustManager trustManager = this.trustManager(sslSocketFactory);
      if (trustManager == null) {
         throw new IllegalStateException("Unable to extract the trust manager on " + get() + ", sslSocketFactory is " + sslSocketFactory.getClass());
      } else {
         return this.buildCertificateChainCleaner(trustManager);
      }
   }

   public static boolean isConscryptPreferred() {
      if ("conscrypt".equals(Util.getSystemProperty("okhttp.platform", (String)null))) {
         return true;
      } else {
         String preferredProvider = Security.getProviders()[0].getName();
         return "Conscrypt".equals(preferredProvider);
      }
   }

   private static Platform findPlatform() {
      Platform android = AndroidPlatform.buildIfSupported();
      if (android != null) {
         return android;
      } else {
         if (isConscryptPreferred()) {
            Platform conscrypt = ConscryptPlatform.buildIfSupported();
            if (conscrypt != null) {
               return conscrypt;
            }
         }

         Platform jdk9 = Jdk9Platform.buildIfSupported();
         if (jdk9 != null) {
            return jdk9;
         } else {
            Platform jdkWithJettyBoot = Jdk8WithJettyBootPlatform.buildIfSupported();
            return jdkWithJettyBoot != null ? jdkWithJettyBoot : new Platform();
         }
      }
   }

   static byte[] concatLengthPrefixed(List<Protocol> protocols) {
      Buffer result = new Buffer();
      int i = 0;

      for(int size = protocols.size(); i < size; ++i) {
         Protocol protocol = (Protocol)protocols.get(i);
         if (protocol != Protocol.HTTP_1_0) {
            result.writeByte(protocol.toString().length());
            result.writeUtf8(protocol.toString());
         }
      }

      return result.readByteArray();
   }

   @Nullable
   static <T> T readFieldOrNull(Object instance, Class<T> fieldType, String fieldName) {
      Class c = instance.getClass();

      while(c != Object.class) {
         try {
            Field field = c.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(instance);
            if (!fieldType.isInstance(value)) {
               return null;
            }

            return fieldType.cast(value);
         } catch (NoSuchFieldException var6) {
            c = c.getSuperclass();
         } catch (IllegalAccessException var7) {
            throw new AssertionError();
         }
      }

      if (!fieldName.equals("delegate")) {
         Object delegate = readFieldOrNull(instance, Object.class, "delegate");
         if (delegate != null) {
            return readFieldOrNull(delegate, fieldType, fieldName);
         }
      }

      return null;
   }

   public SSLContext getSSLContext() {
      try {
         return SSLContext.getInstance("TLS");
      } catch (NoSuchAlgorithmException var2) {
         throw new IllegalStateException("No TLS provider", var2);
      }
   }

   public void configureSslSocketFactory(SSLSocketFactory socketFactory) {
   }

   public String toString() {
      return this.getClass().getSimpleName();
   }
}

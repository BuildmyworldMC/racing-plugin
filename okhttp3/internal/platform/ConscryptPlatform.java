package okhttp3.internal.platform;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.List;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Protocol;
import org.conscrypt.Conscrypt;

public class ConscryptPlatform extends Platform {
   private ConscryptPlatform() {
   }

   private Provider getProvider() {
      return Conscrypt.newProviderBuilder().provideTrustManager().build();
   }

   @Nullable
   public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
      if (!Conscrypt.isConscrypt(sslSocketFactory)) {
         return super.trustManager(sslSocketFactory);
      } else {
         try {
            Object sp = readFieldOrNull(sslSocketFactory, Object.class, "sslParameters");
            return sp != null ? (X509TrustManager)readFieldOrNull(sp, X509TrustManager.class, "x509TrustManager") : null;
         } catch (Exception var3) {
            throw new UnsupportedOperationException("clientBuilder.sslSocketFactory(SSLSocketFactory) not supported on Conscrypt", var3);
         }
      }
   }

   public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
      if (Conscrypt.isConscrypt(sslSocket)) {
         if (hostname != null) {
            Conscrypt.setUseSessionTickets(sslSocket, true);
            Conscrypt.setHostname(sslSocket, hostname);
         }

         List<String> names = Platform.alpnProtocolNames(protocols);
         Conscrypt.setApplicationProtocols(sslSocket, (String[])names.toArray(new String[0]));
      } else {
         super.configureTlsExtensions(sslSocket, hostname, protocols);
      }

   }

   @Nullable
   public String getSelectedProtocol(SSLSocket sslSocket) {
      return Conscrypt.isConscrypt(sslSocket) ? Conscrypt.getApplicationProtocol(sslSocket) : super.getSelectedProtocol(sslSocket);
   }

   public SSLContext getSSLContext() {
      try {
         return SSLContext.getInstance("TLSv1.3", this.getProvider());
      } catch (NoSuchAlgorithmException var4) {
         try {
            return SSLContext.getInstance("TLS", this.getProvider());
         } catch (NoSuchAlgorithmException var3) {
            throw new IllegalStateException("No TLS provider", var4);
         }
      }
   }

   public static ConscryptPlatform buildIfSupported() {
      try {
         Class.forName("org.conscrypt.Conscrypt");
         return !Conscrypt.isAvailable() ? null : new ConscryptPlatform();
      } catch (ClassNotFoundException var1) {
         return null;
      }
   }

   public void configureSslSocketFactory(SSLSocketFactory socketFactory) {
      if (Conscrypt.isConscrypt(socketFactory)) {
         Conscrypt.setUseEngineSocket(socketFactory, true);
      }

   }
}

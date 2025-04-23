package okhttp3;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocket;
import okhttp3.internal.Util;

public final class ConnectionSpec {
   private static final CipherSuite[] RESTRICTED_CIPHER_SUITES;
   private static final CipherSuite[] APPROVED_CIPHER_SUITES;
   public static final ConnectionSpec RESTRICTED_TLS;
   public static final ConnectionSpec MODERN_TLS;
   public static final ConnectionSpec COMPATIBLE_TLS;
   public static final ConnectionSpec CLEARTEXT;
   final boolean tls;
   final boolean supportsTlsExtensions;
   @Nullable
   final String[] cipherSuites;
   @Nullable
   final String[] tlsVersions;

   ConnectionSpec(ConnectionSpec.Builder builder) {
      this.tls = builder.tls;
      this.cipherSuites = builder.cipherSuites;
      this.tlsVersions = builder.tlsVersions;
      this.supportsTlsExtensions = builder.supportsTlsExtensions;
   }

   public boolean isTls() {
      return this.tls;
   }

   @Nullable
   public List<CipherSuite> cipherSuites() {
      return this.cipherSuites != null ? CipherSuite.forJavaNames(this.cipherSuites) : null;
   }

   @Nullable
   public List<TlsVersion> tlsVersions() {
      return this.tlsVersions != null ? TlsVersion.forJavaNames(this.tlsVersions) : null;
   }

   public boolean supportsTlsExtensions() {
      return this.supportsTlsExtensions;
   }

   void apply(SSLSocket sslSocket, boolean isFallback) {
      ConnectionSpec specToApply = this.supportedSpec(sslSocket, isFallback);
      if (specToApply.tlsVersions != null) {
         sslSocket.setEnabledProtocols(specToApply.tlsVersions);
      }

      if (specToApply.cipherSuites != null) {
         sslSocket.setEnabledCipherSuites(specToApply.cipherSuites);
      }

   }

   private ConnectionSpec supportedSpec(SSLSocket sslSocket, boolean isFallback) {
      String[] cipherSuitesIntersection = this.cipherSuites != null ? Util.intersect(CipherSuite.ORDER_BY_NAME, sslSocket.getEnabledCipherSuites(), this.cipherSuites) : sslSocket.getEnabledCipherSuites();
      String[] tlsVersionsIntersection = this.tlsVersions != null ? Util.intersect(Util.NATURAL_ORDER, sslSocket.getEnabledProtocols(), this.tlsVersions) : sslSocket.getEnabledProtocols();
      String[] supportedCipherSuites = sslSocket.getSupportedCipherSuites();
      int indexOfFallbackScsv = Util.indexOf(CipherSuite.ORDER_BY_NAME, supportedCipherSuites, "TLS_FALLBACK_SCSV");
      if (isFallback && indexOfFallbackScsv != -1) {
         cipherSuitesIntersection = Util.concat(cipherSuitesIntersection, supportedCipherSuites[indexOfFallbackScsv]);
      }

      return (new ConnectionSpec.Builder(this)).cipherSuites(cipherSuitesIntersection).tlsVersions(tlsVersionsIntersection).build();
   }

   public boolean isCompatible(SSLSocket socket) {
      if (!this.tls) {
         return false;
      } else if (this.tlsVersions != null && !Util.nonEmptyIntersection(Util.NATURAL_ORDER, this.tlsVersions, socket.getEnabledProtocols())) {
         return false;
      } else {
         return this.cipherSuites == null || Util.nonEmptyIntersection(CipherSuite.ORDER_BY_NAME, this.cipherSuites, socket.getEnabledCipherSuites());
      }
   }

   public boolean equals(@Nullable Object other) {
      if (!(other instanceof ConnectionSpec)) {
         return false;
      } else if (other == this) {
         return true;
      } else {
         ConnectionSpec that = (ConnectionSpec)other;
         if (this.tls != that.tls) {
            return false;
         } else {
            if (this.tls) {
               if (!Arrays.equals(this.cipherSuites, that.cipherSuites)) {
                  return false;
               }

               if (!Arrays.equals(this.tlsVersions, that.tlsVersions)) {
                  return false;
               }

               if (this.supportsTlsExtensions != that.supportsTlsExtensions) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public int hashCode() {
      int result = 17;
      if (this.tls) {
         result = 31 * result + Arrays.hashCode(this.cipherSuites);
         result = 31 * result + Arrays.hashCode(this.tlsVersions);
         result = 31 * result + (this.supportsTlsExtensions ? 0 : 1);
      }

      return result;
   }

   public String toString() {
      return !this.tls ? "ConnectionSpec()" : "ConnectionSpec(cipherSuites=" + Objects.toString(this.cipherSuites(), "[all enabled]") + ", tlsVersions=" + Objects.toString(this.tlsVersions(), "[all enabled]") + ", supportsTlsExtensions=" + this.supportsTlsExtensions + ")";
   }

   static {
      RESTRICTED_CIPHER_SUITES = new CipherSuite[]{CipherSuite.TLS_AES_128_GCM_SHA256, CipherSuite.TLS_AES_256_GCM_SHA384, CipherSuite.TLS_CHACHA20_POLY1305_SHA256, CipherSuite.TLS_AES_128_CCM_SHA256, CipherSuite.TLS_AES_256_CCM_8_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256};
      APPROVED_CIPHER_SUITES = new CipherSuite[]{CipherSuite.TLS_AES_128_GCM_SHA256, CipherSuite.TLS_AES_256_GCM_SHA384, CipherSuite.TLS_CHACHA20_POLY1305_SHA256, CipherSuite.TLS_AES_128_CCM_SHA256, CipherSuite.TLS_AES_256_CCM_8_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384, CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA};
      RESTRICTED_TLS = (new ConnectionSpec.Builder(true)).cipherSuites(RESTRICTED_CIPHER_SUITES).tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2).supportsTlsExtensions(true).build();
      MODERN_TLS = (new ConnectionSpec.Builder(true)).cipherSuites(APPROVED_CIPHER_SUITES).tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2).supportsTlsExtensions(true).build();
      COMPATIBLE_TLS = (new ConnectionSpec.Builder(true)).cipherSuites(APPROVED_CIPHER_SUITES).tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0).supportsTlsExtensions(true).build();
      CLEARTEXT = (new ConnectionSpec.Builder(false)).build();
   }

   public static final class Builder {
      boolean tls;
      @Nullable
      String[] cipherSuites;
      @Nullable
      String[] tlsVersions;
      boolean supportsTlsExtensions;

      Builder(boolean tls) {
         this.tls = tls;
      }

      public Builder(ConnectionSpec connectionSpec) {
         this.tls = connectionSpec.tls;
         this.cipherSuites = connectionSpec.cipherSuites;
         this.tlsVersions = connectionSpec.tlsVersions;
         this.supportsTlsExtensions = connectionSpec.supportsTlsExtensions;
      }

      public ConnectionSpec.Builder allEnabledCipherSuites() {
         if (!this.tls) {
            throw new IllegalStateException("no cipher suites for cleartext connections");
         } else {
            this.cipherSuites = null;
            return this;
         }
      }

      public ConnectionSpec.Builder cipherSuites(CipherSuite... cipherSuites) {
         if (!this.tls) {
            throw new IllegalStateException("no cipher suites for cleartext connections");
         } else {
            String[] strings = new String[cipherSuites.length];

            for(int i = 0; i < cipherSuites.length; ++i) {
               strings[i] = cipherSuites[i].javaName;
            }

            return this.cipherSuites(strings);
         }
      }

      public ConnectionSpec.Builder cipherSuites(String... cipherSuites) {
         if (!this.tls) {
            throw new IllegalStateException("no cipher suites for cleartext connections");
         } else if (cipherSuites.length == 0) {
            throw new IllegalArgumentException("At least one cipher suite is required");
         } else {
            this.cipherSuites = (String[])cipherSuites.clone();
            return this;
         }
      }

      public ConnectionSpec.Builder allEnabledTlsVersions() {
         if (!this.tls) {
            throw new IllegalStateException("no TLS versions for cleartext connections");
         } else {
            this.tlsVersions = null;
            return this;
         }
      }

      public ConnectionSpec.Builder tlsVersions(TlsVersion... tlsVersions) {
         if (!this.tls) {
            throw new IllegalStateException("no TLS versions for cleartext connections");
         } else {
            String[] strings = new String[tlsVersions.length];

            for(int i = 0; i < tlsVersions.length; ++i) {
               strings[i] = tlsVersions[i].javaName;
            }

            return this.tlsVersions(strings);
         }
      }

      public ConnectionSpec.Builder tlsVersions(String... tlsVersions) {
         if (!this.tls) {
            throw new IllegalStateException("no TLS versions for cleartext connections");
         } else if (tlsVersions.length == 0) {
            throw new IllegalArgumentException("At least one TLS version is required");
         } else {
            this.tlsVersions = (String[])tlsVersions.clone();
            return this;
         }
      }

      /** @deprecated */
      public ConnectionSpec.Builder supportsTlsExtensions(boolean supportsTlsExtensions) {
         if (!this.tls) {
            throw new IllegalStateException("no TLS extensions for cleartext connections");
         } else {
            this.supportsTlsExtensions = supportsTlsExtensions;
            return this;
         }
      }

      public ConnectionSpec build() {
         return new ConnectionSpec(this);
      }
   }
}

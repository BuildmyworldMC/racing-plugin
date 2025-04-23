package okhttp3;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.net.ssl.SSLPeerUnverifiedException;
import okhttp3.internal.tls.CertificateChainCleaner;
import okio.ByteString;

public final class CertificatePinner {
   public static final CertificatePinner DEFAULT = (new CertificatePinner.Builder()).build();
   private final Set<CertificatePinner.Pin> pins;
   @Nullable
   private final CertificateChainCleaner certificateChainCleaner;

   CertificatePinner(Set<CertificatePinner.Pin> pins, @Nullable CertificateChainCleaner certificateChainCleaner) {
      this.pins = pins;
      this.certificateChainCleaner = certificateChainCleaner;
   }

   public boolean equals(@Nullable Object other) {
      if (other == this) {
         return true;
      } else {
         return other instanceof CertificatePinner && Objects.equals(this.certificateChainCleaner, ((CertificatePinner)other).certificateChainCleaner) && this.pins.equals(((CertificatePinner)other).pins);
      }
   }

   public int hashCode() {
      int result = Objects.hashCode(this.certificateChainCleaner);
      result = 31 * result + this.pins.hashCode();
      return result;
   }

   public void check(String hostname, List<Certificate> peerCertificates) throws SSLPeerUnverifiedException {
      List<CertificatePinner.Pin> pins = this.findMatchingPins(hostname);
      if (!pins.isEmpty()) {
         if (this.certificateChainCleaner != null) {
            peerCertificates = this.certificateChainCleaner.clean(peerCertificates, hostname);
         }

         int c = 0;

         int p;
         for(p = peerCertificates.size(); c < p; ++c) {
            X509Certificate x509Certificate = (X509Certificate)peerCertificates.get(c);
            ByteString sha1 = null;
            ByteString sha256 = null;
            int p = 0;

            for(int pinsSize = pins.size(); p < pinsSize; ++p) {
               CertificatePinner.Pin pin = (CertificatePinner.Pin)pins.get(p);
               if (pin.hashAlgorithm.equals("sha256/")) {
                  if (sha256 == null) {
                     sha256 = sha256(x509Certificate);
                  }

                  if (pin.hash.equals(sha256)) {
                     return;
                  }
               } else {
                  if (!pin.hashAlgorithm.equals("sha1/")) {
                     throw new AssertionError("unsupported hashAlgorithm: " + pin.hashAlgorithm);
                  }

                  if (sha1 == null) {
                     sha1 = sha1(x509Certificate);
                  }

                  if (pin.hash.equals(sha1)) {
                     return;
                  }
               }
            }
         }

         StringBuilder message = (new StringBuilder()).append("Certificate pinning failure!").append("\n  Peer certificate chain:");
         p = 0;

         int pinsSize;
         for(pinsSize = peerCertificates.size(); p < pinsSize; ++p) {
            X509Certificate x509Certificate = (X509Certificate)peerCertificates.get(p);
            message.append("\n    ").append(pin(x509Certificate)).append(": ").append(x509Certificate.getSubjectDN().getName());
         }

         message.append("\n  Pinned certificates for ").append(hostname).append(":");
         p = 0;

         for(pinsSize = pins.size(); p < pinsSize; ++p) {
            CertificatePinner.Pin pin = (CertificatePinner.Pin)pins.get(p);
            message.append("\n    ").append(pin);
         }

         throw new SSLPeerUnverifiedException(message.toString());
      }
   }

   /** @deprecated */
   public void check(String hostname, Certificate... peerCertificates) throws SSLPeerUnverifiedException {
      this.check(hostname, Arrays.asList(peerCertificates));
   }

   List<CertificatePinner.Pin> findMatchingPins(String hostname) {
      List<CertificatePinner.Pin> result = Collections.emptyList();
      Iterator var3 = this.pins.iterator();

      while(var3.hasNext()) {
         CertificatePinner.Pin pin = (CertificatePinner.Pin)var3.next();
         if (pin.matches(hostname)) {
            if (((List)result).isEmpty()) {
               result = new ArrayList();
            }

            ((List)result).add(pin);
         }
      }

      return (List)result;
   }

   CertificatePinner withCertificateChainCleaner(@Nullable CertificateChainCleaner certificateChainCleaner) {
      return Objects.equals(this.certificateChainCleaner, certificateChainCleaner) ? this : new CertificatePinner(this.pins, certificateChainCleaner);
   }

   public static String pin(Certificate certificate) {
      if (!(certificate instanceof X509Certificate)) {
         throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
      } else {
         return "sha256/" + sha256((X509Certificate)certificate).base64();
      }
   }

   static ByteString sha1(X509Certificate x509Certificate) {
      return ByteString.of(x509Certificate.getPublicKey().getEncoded()).sha1();
   }

   static ByteString sha256(X509Certificate x509Certificate) {
      return ByteString.of(x509Certificate.getPublicKey().getEncoded()).sha256();
   }

   public static final class Builder {
      private final List<CertificatePinner.Pin> pins = new ArrayList();

      public CertificatePinner.Builder add(String pattern, String... pins) {
         if (pattern == null) {
            throw new NullPointerException("pattern == null");
         } else {
            String[] var3 = pins;
            int var4 = pins.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               String pin = var3[var5];
               this.pins.add(new CertificatePinner.Pin(pattern, pin));
            }

            return this;
         }
      }

      public CertificatePinner build() {
         return new CertificatePinner(new LinkedHashSet(this.pins), (CertificateChainCleaner)null);
      }
   }

   static final class Pin {
      private static final String WILDCARD = "*.";
      final String pattern;
      final String canonicalHostname;
      final String hashAlgorithm;
      final ByteString hash;

      Pin(String pattern, String pin) {
         this.pattern = pattern;
         this.canonicalHostname = pattern.startsWith("*.") ? HttpUrl.get("http://" + pattern.substring("*.".length())).host() : HttpUrl.get("http://" + pattern).host();
         if (pin.startsWith("sha1/")) {
            this.hashAlgorithm = "sha1/";
            this.hash = ByteString.decodeBase64(pin.substring("sha1/".length()));
         } else {
            if (!pin.startsWith("sha256/")) {
               throw new IllegalArgumentException("pins must start with 'sha256/' or 'sha1/': " + pin);
            }

            this.hashAlgorithm = "sha256/";
            this.hash = ByteString.decodeBase64(pin.substring("sha256/".length()));
         }

         if (this.hash == null) {
            throw new IllegalArgumentException("pins must be base64: " + pin);
         }
      }

      boolean matches(String hostname) {
         if (!this.pattern.startsWith("*.")) {
            return hostname.equals(this.canonicalHostname);
         } else {
            int firstDot = hostname.indexOf(46);
            return hostname.length() - firstDot - 1 == this.canonicalHostname.length() && hostname.regionMatches(false, firstDot + 1, this.canonicalHostname, 0, this.canonicalHostname.length());
         }
      }

      public boolean equals(Object other) {
         return other instanceof CertificatePinner.Pin && this.pattern.equals(((CertificatePinner.Pin)other).pattern) && this.hashAlgorithm.equals(((CertificatePinner.Pin)other).hashAlgorithm) && this.hash.equals(((CertificatePinner.Pin)other).hash);
      }

      public int hashCode() {
         int result = 17;
         int result = 31 * result + this.pattern.hashCode();
         result = 31 * result + this.hashAlgorithm.hashCode();
         result = 31 * result + this.hash.hashCode();
         return result;
      }

      public String toString() {
         return this.hashAlgorithm + this.hash.base64();
      }
   }
}

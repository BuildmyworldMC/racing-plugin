package okhttp3.internal.tls;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.auth.x500.X500Principal;

public final class BasicCertificateChainCleaner extends CertificateChainCleaner {
   private static final int MAX_SIGNERS = 9;
   private final Map<X500Principal, Set<X509Certificate>> subjectToCaCerts = new LinkedHashMap();

   public BasicCertificateChainCleaner(X509Certificate... caCerts) {
      X509Certificate[] var2 = caCerts;
      int var3 = caCerts.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         X509Certificate caCert = var2[var4];
         X500Principal subject = caCert.getSubjectX500Principal();
         Set<X509Certificate> subjectCaCerts = (Set)this.subjectToCaCerts.get(subject);
         if (subjectCaCerts == null) {
            subjectCaCerts = new LinkedHashSet(1);
            this.subjectToCaCerts.put(subject, subjectCaCerts);
         }

         ((Set)subjectCaCerts).add(caCert);
      }

   }

   public List<Certificate> clean(List<Certificate> chain, String hostname) throws SSLPeerUnverifiedException {
      Deque<Certificate> queue = new ArrayDeque(chain);
      List<Certificate> result = new ArrayList();
      result.add((Certificate)queue.removeFirst());
      boolean foundTrustedCertificate = false;

      for(int c = 0; c < 9; ++c) {
         X509Certificate toVerify = (X509Certificate)result.get(result.size() - 1);
         X509Certificate trustedCert = this.findByIssuerAndSignature(toVerify);
         if (trustedCert == null) {
            Iterator i = queue.iterator();

            X509Certificate signingCert;
            do {
               if (!i.hasNext()) {
                  if (foundTrustedCertificate) {
                     return result;
                  }

                  throw new SSLPeerUnverifiedException("Failed to find a trusted cert that signed " + toVerify);
               }

               signingCert = (X509Certificate)i.next();
            } while(!this.verifySignature(toVerify, signingCert));

            i.remove();
            result.add(signingCert);
         } else {
            if (result.size() > 1 || !toVerify.equals(trustedCert)) {
               result.add(trustedCert);
            }

            if (this.verifySignature(trustedCert, trustedCert)) {
               return result;
            }

            foundTrustedCertificate = true;
         }
      }

      throw new SSLPeerUnverifiedException("Certificate chain too long: " + result);
   }

   private boolean verifySignature(X509Certificate toVerify, X509Certificate signingCert) {
      if (!toVerify.getIssuerDN().equals(signingCert.getSubjectDN())) {
         return false;
      } else {
         try {
            toVerify.verify(signingCert.getPublicKey());
            return true;
         } catch (GeneralSecurityException var4) {
            return false;
         }
      }
   }

   private X509Certificate findByIssuerAndSignature(X509Certificate cert) {
      X500Principal issuer = cert.getIssuerX500Principal();
      Set<X509Certificate> subjectCaCerts = (Set)this.subjectToCaCerts.get(issuer);
      if (subjectCaCerts == null) {
         return null;
      } else {
         Iterator var4 = subjectCaCerts.iterator();

         while(var4.hasNext()) {
            X509Certificate caCert = (X509Certificate)var4.next();
            PublicKey publicKey = caCert.getPublicKey();

            try {
               cert.verify(publicKey);
               return caCert;
            } catch (Exception var8) {
            }
         }

         return null;
      }
   }

   public int hashCode() {
      return this.subjectToCaCerts.hashCode();
   }

   public boolean equals(Object other) {
      if (other == this) {
         return true;
      } else {
         return other instanceof BasicCertificateChainCleaner && ((BasicCertificateChainCleaner)other).subjectToCaCerts.equals(this.subjectToCaCerts);
      }
   }
}

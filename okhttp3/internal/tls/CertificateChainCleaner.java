package okhttp3.internal.tls;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.X509TrustManager;
import okhttp3.internal.platform.Platform;

public abstract class CertificateChainCleaner {
   public abstract List<Certificate> clean(List<Certificate> var1, String var2) throws SSLPeerUnverifiedException;

   public static CertificateChainCleaner get(X509TrustManager trustManager) {
      return Platform.get().buildCertificateChainCleaner(trustManager);
   }

   public static CertificateChainCleaner get(X509Certificate... caCerts) {
      return new BasicCertificateChainCleaner(caCerts);
   }
}

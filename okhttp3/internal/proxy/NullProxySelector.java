package okhttp3.internal.proxy;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class NullProxySelector extends ProxySelector {
   public List<Proxy> select(URI uri) {
      if (uri == null) {
         throw new IllegalArgumentException("uri must not be null");
      } else {
         return Collections.singletonList(Proxy.NO_PROXY);
      }
   }

   public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
   }
}

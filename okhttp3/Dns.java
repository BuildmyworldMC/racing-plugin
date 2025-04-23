package okhttp3;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public interface Dns {
   Dns SYSTEM = (hostname) -> {
      if (hostname == null) {
         throw new UnknownHostException("hostname == null");
      } else {
         try {
            return Arrays.asList(InetAddress.getAllByName(hostname));
         } catch (NullPointerException var3) {
            UnknownHostException unknownHostException = new UnknownHostException("Broken system behaviour for dns lookup of " + hostname);
            unknownHostException.initCause(var3);
            throw unknownHostException;
         }
      }
   };

   List<InetAddress> lookup(String var1) throws UnknownHostException;
}

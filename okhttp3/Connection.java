package okhttp3;

import java.net.Socket;
import javax.annotation.Nullable;

public interface Connection {
   Route route();

   Socket socket();

   @Nullable
   Handshake handshake();

   Protocol protocol();
}

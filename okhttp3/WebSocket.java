package okhttp3;

import javax.annotation.Nullable;
import okio.ByteString;

public interface WebSocket {
   Request request();

   long queueSize();

   boolean send(String var1);

   boolean send(ByteString var1);

   boolean close(int var1, @Nullable String var2);

   void cancel();

   public interface Factory {
      WebSocket newWebSocket(Request var1, WebSocketListener var2);
   }
}

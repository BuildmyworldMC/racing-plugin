package okhttp3;

import java.io.IOException;
import okio.Timeout;

public interface Call extends Cloneable {
   Request request();

   Response execute() throws IOException;

   void enqueue(Callback var1);

   void cancel();

   boolean isExecuted();

   boolean isCanceled();

   Timeout timeout();

   Call clone();

   public interface Factory {
      Call newCall(Request var1);
   }
}

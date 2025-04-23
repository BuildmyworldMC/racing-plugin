package okhttp3.internal.cache;

import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.Request;
import okhttp3.Response;

public interface InternalCache {
   @Nullable
   Response get(Request var1) throws IOException;

   @Nullable
   CacheRequest put(Response var1) throws IOException;

   void remove(Request var1) throws IOException;

   void update(Response var1, Response var2);

   void trackConditionalCacheHit();

   void trackResponse(CacheStrategy var1);
}

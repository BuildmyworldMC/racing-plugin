package okhttp3;

import java.util.Collections;
import java.util.List;

public interface CookieJar {
   CookieJar NO_COOKIES = new CookieJar() {
      public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
      }

      public List<Cookie> loadForRequest(HttpUrl url) {
         return Collections.emptyList();
      }
   };

   void saveFromResponse(HttpUrl var1, List<Cookie> var2);

   List<Cookie> loadForRequest(HttpUrl var1);
}

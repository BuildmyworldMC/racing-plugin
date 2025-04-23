package okhttp3;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public interface Interceptor {
   Response intercept(Interceptor.Chain var1) throws IOException;

   public interface Chain {
      Request request();

      Response proceed(Request var1) throws IOException;

      @Nullable
      Connection connection();

      Call call();

      int connectTimeoutMillis();

      Interceptor.Chain withConnectTimeout(int var1, TimeUnit var2);

      int readTimeoutMillis();

      Interceptor.Chain withReadTimeout(int var1, TimeUnit var2);

      int writeTimeoutMillis();

      Interceptor.Chain withWriteTimeout(int var1, TimeUnit var2);
   }
}

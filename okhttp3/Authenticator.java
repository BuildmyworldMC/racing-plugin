package okhttp3;

import java.io.IOException;
import javax.annotation.Nullable;

public interface Authenticator {
   Authenticator NONE = (route, response) -> {
      return null;
   };

   @Nullable
   Request authenticate(@Nullable Route var1, Response var2) throws IOException;
}

package okhttp3;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

public final class Challenge {
   private final String scheme;
   private final Map<String, String> authParams;

   public Challenge(String scheme, Map<String, String> authParams) {
      if (scheme == null) {
         throw new NullPointerException("scheme == null");
      } else if (authParams == null) {
         throw new NullPointerException("authParams == null");
      } else {
         this.scheme = scheme;
         Map<String, String> newAuthParams = new LinkedHashMap();
         Iterator var4 = authParams.entrySet().iterator();

         while(var4.hasNext()) {
            Entry<String, String> authParam = (Entry)var4.next();
            String key = authParam.getKey() == null ? null : ((String)authParam.getKey()).toLowerCase(Locale.US);
            newAuthParams.put(key, (String)authParam.getValue());
         }

         this.authParams = Collections.unmodifiableMap(newAuthParams);
      }
   }

   public Challenge(String scheme, String realm) {
      if (scheme == null) {
         throw new NullPointerException("scheme == null");
      } else if (realm == null) {
         throw new NullPointerException("realm == null");
      } else {
         this.scheme = scheme;
         this.authParams = Collections.singletonMap("realm", realm);
      }
   }

   public Challenge withCharset(Charset charset) {
      if (charset == null) {
         throw new NullPointerException("charset == null");
      } else {
         Map<String, String> authParams = new LinkedHashMap(this.authParams);
         authParams.put("charset", charset.name());
         return new Challenge(this.scheme, authParams);
      }
   }

   public String scheme() {
      return this.scheme;
   }

   public Map<String, String> authParams() {
      return this.authParams;
   }

   public String realm() {
      return (String)this.authParams.get("realm");
   }

   public Charset charset() {
      String charset = (String)this.authParams.get("charset");
      if (charset != null) {
         try {
            return Charset.forName(charset);
         } catch (Exception var3) {
         }
      }

      return StandardCharsets.ISO_8859_1;
   }

   public boolean equals(@Nullable Object other) {
      return other instanceof Challenge && ((Challenge)other).scheme.equals(this.scheme) && ((Challenge)other).authParams.equals(this.authParams);
   }

   public int hashCode() {
      int result = 29;
      int result = 31 * result + this.scheme.hashCode();
      result = 31 * result + this.authParams.hashCode();
      return result;
   }

   public String toString() {
      return this.scheme + " authParams=" + this.authParams;
   }
}

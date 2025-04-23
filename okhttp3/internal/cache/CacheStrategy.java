package okhttp3.internal.cache;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.http.HttpDate;
import okhttp3.internal.http.HttpHeaders;

public final class CacheStrategy {
   @Nullable
   public final Request networkRequest;
   @Nullable
   public final Response cacheResponse;

   CacheStrategy(Request networkRequest, Response cacheResponse) {
      this.networkRequest = networkRequest;
      this.cacheResponse = cacheResponse;
   }

   public static boolean isCacheable(Response response, Request request) {
      switch(response.code()) {
      case 200:
      case 203:
      case 204:
      case 300:
      case 301:
      case 308:
      case 404:
      case 405:
      case 410:
      case 414:
      case 501:
         break;
      case 302:
      case 307:
         if (response.header("Expires") != null || response.cacheControl().maxAgeSeconds() != -1 || response.cacheControl().isPublic() || response.cacheControl().isPrivate()) {
            break;
         }
      default:
         return false;
      }

      return !response.cacheControl().noStore() && !request.cacheControl().noStore();
   }

   public static class Factory {
      final long nowMillis;
      final Request request;
      final Response cacheResponse;
      private Date servedDate;
      private String servedDateString;
      private Date lastModified;
      private String lastModifiedString;
      private Date expires;
      private long sentRequestMillis;
      private long receivedResponseMillis;
      private String etag;
      private int ageSeconds = -1;

      public Factory(long nowMillis, Request request, Response cacheResponse) {
         this.nowMillis = nowMillis;
         this.request = request;
         this.cacheResponse = cacheResponse;
         if (cacheResponse != null) {
            this.sentRequestMillis = cacheResponse.sentRequestAtMillis();
            this.receivedResponseMillis = cacheResponse.receivedResponseAtMillis();
            Headers headers = cacheResponse.headers();
            int i = 0;

            for(int size = headers.size(); i < size; ++i) {
               String fieldName = headers.name(i);
               String value = headers.value(i);
               if ("Date".equalsIgnoreCase(fieldName)) {
                  this.servedDate = HttpDate.parse(value);
                  this.servedDateString = value;
               } else if ("Expires".equalsIgnoreCase(fieldName)) {
                  this.expires = HttpDate.parse(value);
               } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
                  this.lastModified = HttpDate.parse(value);
                  this.lastModifiedString = value;
               } else if ("ETag".equalsIgnoreCase(fieldName)) {
                  this.etag = value;
               } else if ("Age".equalsIgnoreCase(fieldName)) {
                  this.ageSeconds = HttpHeaders.parseSeconds(value, -1);
               }
            }
         }

      }

      public CacheStrategy get() {
         CacheStrategy candidate = this.getCandidate();
         return candidate.networkRequest != null && this.request.cacheControl().onlyIfCached() ? new CacheStrategy((Request)null, (Response)null) : candidate;
      }

      private CacheStrategy getCandidate() {
         if (this.cacheResponse == null) {
            return new CacheStrategy(this.request, (Response)null);
         } else if (this.request.isHttps() && this.cacheResponse.handshake() == null) {
            return new CacheStrategy(this.request, (Response)null);
         } else if (!CacheStrategy.isCacheable(this.cacheResponse, this.request)) {
            return new CacheStrategy(this.request, (Response)null);
         } else {
            CacheControl requestCaching = this.request.cacheControl();
            if (!requestCaching.noCache() && !hasConditions(this.request)) {
               CacheControl responseCaching = this.cacheResponse.cacheControl();
               long ageMillis = this.cacheResponseAge();
               long freshMillis = this.computeFreshnessLifetime();
               if (requestCaching.maxAgeSeconds() != -1) {
                  freshMillis = Math.min(freshMillis, TimeUnit.SECONDS.toMillis((long)requestCaching.maxAgeSeconds()));
               }

               long minFreshMillis = 0L;
               if (requestCaching.minFreshSeconds() != -1) {
                  minFreshMillis = TimeUnit.SECONDS.toMillis((long)requestCaching.minFreshSeconds());
               }

               long maxStaleMillis = 0L;
               if (!responseCaching.mustRevalidate() && requestCaching.maxStaleSeconds() != -1) {
                  maxStaleMillis = TimeUnit.SECONDS.toMillis((long)requestCaching.maxStaleSeconds());
               }

               if (!responseCaching.noCache() && ageMillis + minFreshMillis < freshMillis + maxStaleMillis) {
                  Response.Builder builder = this.cacheResponse.newBuilder();
                  if (ageMillis + minFreshMillis >= freshMillis) {
                     builder.addHeader("Warning", "110 HttpURLConnection \"Response is stale\"");
                  }

                  long oneDayMillis = 86400000L;
                  if (ageMillis > oneDayMillis && this.isFreshnessLifetimeHeuristic()) {
                     builder.addHeader("Warning", "113 HttpURLConnection \"Heuristic expiration\"");
                  }

                  return new CacheStrategy((Request)null, builder.build());
               } else {
                  String conditionName;
                  String conditionValue;
                  if (this.etag != null) {
                     conditionName = "If-None-Match";
                     conditionValue = this.etag;
                  } else if (this.lastModified != null) {
                     conditionName = "If-Modified-Since";
                     conditionValue = this.lastModifiedString;
                  } else {
                     if (this.servedDate == null) {
                        return new CacheStrategy(this.request, (Response)null);
                     }

                     conditionName = "If-Modified-Since";
                     conditionValue = this.servedDateString;
                  }

                  Headers.Builder conditionalRequestHeaders = this.request.headers().newBuilder();
                  Internal.instance.addLenient(conditionalRequestHeaders, conditionName, conditionValue);
                  Request conditionalRequest = this.request.newBuilder().headers(conditionalRequestHeaders.build()).build();
                  return new CacheStrategy(conditionalRequest, this.cacheResponse);
               }
            } else {
               return new CacheStrategy(this.request, (Response)null);
            }
         }
      }

      private long computeFreshnessLifetime() {
         CacheControl responseCaching = this.cacheResponse.cacheControl();
         if (responseCaching.maxAgeSeconds() != -1) {
            return TimeUnit.SECONDS.toMillis((long)responseCaching.maxAgeSeconds());
         } else {
            long servedMillis;
            long delta;
            if (this.expires != null) {
               servedMillis = this.servedDate != null ? this.servedDate.getTime() : this.receivedResponseMillis;
               delta = this.expires.getTime() - servedMillis;
               return delta > 0L ? delta : 0L;
            } else if (this.lastModified != null && this.cacheResponse.request().url().query() == null) {
               servedMillis = this.servedDate != null ? this.servedDate.getTime() : this.sentRequestMillis;
               delta = servedMillis - this.lastModified.getTime();
               return delta > 0L ? delta / 10L : 0L;
            } else {
               return 0L;
            }
         }
      }

      private long cacheResponseAge() {
         long apparentReceivedAge = this.servedDate != null ? Math.max(0L, this.receivedResponseMillis - this.servedDate.getTime()) : 0L;
         long receivedAge = this.ageSeconds != -1 ? Math.max(apparentReceivedAge, TimeUnit.SECONDS.toMillis((long)this.ageSeconds)) : apparentReceivedAge;
         long responseDuration = this.receivedResponseMillis - this.sentRequestMillis;
         long residentDuration = this.nowMillis - this.receivedResponseMillis;
         return receivedAge + responseDuration + residentDuration;
      }

      private boolean isFreshnessLifetimeHeuristic() {
         return this.cacheResponse.cacheControl().maxAgeSeconds() == -1 && this.expires == null;
      }

      private static boolean hasConditions(Request request) {
         return request.header("If-Modified-Since") != null || request.header("If-None-Match") != null;
      }
   }
}

package okhttp3;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpMethod;

public final class Request {
   final HttpUrl url;
   final String method;
   final Headers headers;
   @Nullable
   final RequestBody body;
   final Map<Class<?>, Object> tags;
   @Nullable
   private volatile CacheControl cacheControl;

   Request(Request.Builder builder) {
      this.url = builder.url;
      this.method = builder.method;
      this.headers = builder.headers.build();
      this.body = builder.body;
      this.tags = Util.immutableMap(builder.tags);
   }

   public HttpUrl url() {
      return this.url;
   }

   public String method() {
      return this.method;
   }

   public Headers headers() {
      return this.headers;
   }

   @Nullable
   public String header(String name) {
      return this.headers.get(name);
   }

   public List<String> headers(String name) {
      return this.headers.values(name);
   }

   @Nullable
   public RequestBody body() {
      return this.body;
   }

   @Nullable
   public Object tag() {
      return this.tag(Object.class);
   }

   @Nullable
   public <T> T tag(Class<? extends T> type) {
      return type.cast(this.tags.get(type));
   }

   public Request.Builder newBuilder() {
      return new Request.Builder(this);
   }

   public CacheControl cacheControl() {
      CacheControl result = this.cacheControl;
      return result != null ? result : (this.cacheControl = CacheControl.parse(this.headers));
   }

   public boolean isHttps() {
      return this.url.isHttps();
   }

   public String toString() {
      return "Request{method=" + this.method + ", url=" + this.url + ", tags=" + this.tags + '}';
   }

   public static class Builder {
      @Nullable
      HttpUrl url;
      String method;
      Headers.Builder headers;
      @Nullable
      RequestBody body;
      Map<Class<?>, Object> tags = Collections.emptyMap();

      public Builder() {
         this.method = "GET";
         this.headers = new Headers.Builder();
      }

      Builder(Request request) {
         this.url = request.url;
         this.method = request.method;
         this.body = request.body;
         this.tags = (Map)(request.tags.isEmpty() ? Collections.emptyMap() : new LinkedHashMap(request.tags));
         this.headers = request.headers.newBuilder();
      }

      public Request.Builder url(HttpUrl url) {
         if (url == null) {
            throw new NullPointerException("url == null");
         } else {
            this.url = url;
            return this;
         }
      }

      public Request.Builder url(String url) {
         if (url == null) {
            throw new NullPointerException("url == null");
         } else {
            if (url.regionMatches(true, 0, "ws:", 0, 3)) {
               url = "http:" + url.substring(3);
            } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
               url = "https:" + url.substring(4);
            }

            return this.url(HttpUrl.get(url));
         }
      }

      public Request.Builder url(URL url) {
         if (url == null) {
            throw new NullPointerException("url == null");
         } else {
            return this.url(HttpUrl.get(url.toString()));
         }
      }

      public Request.Builder header(String name, String value) {
         this.headers.set(name, value);
         return this;
      }

      public Request.Builder addHeader(String name, String value) {
         this.headers.add(name, value);
         return this;
      }

      public Request.Builder removeHeader(String name) {
         this.headers.removeAll(name);
         return this;
      }

      public Request.Builder headers(Headers headers) {
         this.headers = headers.newBuilder();
         return this;
      }

      public Request.Builder cacheControl(CacheControl cacheControl) {
         String value = cacheControl.toString();
         return value.isEmpty() ? this.removeHeader("Cache-Control") : this.header("Cache-Control", value);
      }

      public Request.Builder get() {
         return this.method("GET", (RequestBody)null);
      }

      public Request.Builder head() {
         return this.method("HEAD", (RequestBody)null);
      }

      public Request.Builder post(RequestBody body) {
         return this.method("POST", body);
      }

      public Request.Builder delete(@Nullable RequestBody body) {
         return this.method("DELETE", body);
      }

      public Request.Builder delete() {
         return this.delete(Util.EMPTY_REQUEST);
      }

      public Request.Builder put(RequestBody body) {
         return this.method("PUT", body);
      }

      public Request.Builder patch(RequestBody body) {
         return this.method("PATCH", body);
      }

      public Request.Builder method(String method, @Nullable RequestBody body) {
         if (method == null) {
            throw new NullPointerException("method == null");
         } else if (method.length() == 0) {
            throw new IllegalArgumentException("method.length() == 0");
         } else if (body != null && !HttpMethod.permitsRequestBody(method)) {
            throw new IllegalArgumentException("method " + method + " must not have a request body.");
         } else if (body == null && HttpMethod.requiresRequestBody(method)) {
            throw new IllegalArgumentException("method " + method + " must have a request body.");
         } else {
            this.method = method;
            this.body = body;
            return this;
         }
      }

      public Request.Builder tag(@Nullable Object tag) {
         return this.tag(Object.class, tag);
      }

      public <T> Request.Builder tag(Class<? super T> type, @Nullable T tag) {
         if (type == null) {
            throw new NullPointerException("type == null");
         } else {
            if (tag == null) {
               this.tags.remove(type);
            } else {
               if (this.tags.isEmpty()) {
                  this.tags = new LinkedHashMap();
               }

               this.tags.put(type, type.cast(tag));
            }

            return this;
         }
      }

      public Request build() {
         if (this.url == null) {
            throw new IllegalStateException("url == null");
         } else {
            return new Request(this);
         }
      }
   }
}

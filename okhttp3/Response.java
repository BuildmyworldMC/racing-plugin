package okhttp3;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import okhttp3.internal.http.HttpCodec;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.Source;

public final class Response implements Closeable {
   final Request request;
   final Protocol protocol;
   final int code;
   final String message;
   @Nullable
   final Handshake handshake;
   final Headers headers;
   @Nullable
   final ResponseBody body;
   @Nullable
   final Response networkResponse;
   @Nullable
   final Response cacheResponse;
   @Nullable
   final Response priorResponse;
   final long sentRequestAtMillis;
   final long receivedResponseAtMillis;
   @Nullable
   final HttpCodec httpCodec;
   @Nullable
   private volatile CacheControl cacheControl;

   Response(Response.Builder builder) {
      this.request = builder.request;
      this.protocol = builder.protocol;
      this.code = builder.code;
      this.message = builder.message;
      this.handshake = builder.handshake;
      this.headers = builder.headers.build();
      this.body = builder.body;
      this.networkResponse = builder.networkResponse;
      this.cacheResponse = builder.cacheResponse;
      this.priorResponse = builder.priorResponse;
      this.sentRequestAtMillis = builder.sentRequestAtMillis;
      this.receivedResponseAtMillis = builder.receivedResponseAtMillis;
      this.httpCodec = builder.httpCodec;
   }

   public Request request() {
      return this.request;
   }

   public Protocol protocol() {
      return this.protocol;
   }

   public int code() {
      return this.code;
   }

   public boolean isSuccessful() {
      return this.code >= 200 && this.code < 300;
   }

   public String message() {
      return this.message;
   }

   @Nullable
   public Handshake handshake() {
      return this.handshake;
   }

   public List<String> headers(String name) {
      return this.headers.values(name);
   }

   @Nullable
   public String header(String name) {
      return this.header(name, (String)null);
   }

   @Nullable
   public String header(String name, @Nullable String defaultValue) {
      String result = this.headers.get(name);
      return result != null ? result : defaultValue;
   }

   public Headers headers() {
      return this.headers;
   }

   public Headers trailers() throws IOException {
      return this.httpCodec.trailers();
   }

   public ResponseBody peekBody(long byteCount) throws IOException {
      BufferedSource peeked = this.body.source().peek();
      Buffer buffer = new Buffer();
      peeked.request(byteCount);
      buffer.write((Source)peeked, Math.min(byteCount, peeked.getBuffer().size()));
      return ResponseBody.create(this.body.contentType(), buffer.size(), buffer);
   }

   @Nullable
   public ResponseBody body() {
      return this.body;
   }

   public Response.Builder newBuilder() {
      return new Response.Builder(this);
   }

   public boolean isRedirect() {
      switch(this.code) {
      case 300:
      case 301:
      case 302:
      case 303:
      case 307:
      case 308:
         return true;
      case 304:
      case 305:
      case 306:
      default:
         return false;
      }
   }

   @Nullable
   public Response networkResponse() {
      return this.networkResponse;
   }

   @Nullable
   public Response cacheResponse() {
      return this.cacheResponse;
   }

   @Nullable
   public Response priorResponse() {
      return this.priorResponse;
   }

   public List<Challenge> challenges() {
      String responseField;
      if (this.code == 401) {
         responseField = "WWW-Authenticate";
      } else {
         if (this.code != 407) {
            return Collections.emptyList();
         }

         responseField = "Proxy-Authenticate";
      }

      return HttpHeaders.parseChallenges(this.headers(), responseField);
   }

   public CacheControl cacheControl() {
      CacheControl result = this.cacheControl;
      return result != null ? result : (this.cacheControl = CacheControl.parse(this.headers));
   }

   public long sentRequestAtMillis() {
      return this.sentRequestAtMillis;
   }

   public long receivedResponseAtMillis() {
      return this.receivedResponseAtMillis;
   }

   public void close() {
      if (this.body == null) {
         throw new IllegalStateException("response is not eligible for a body and must not be closed");
      } else {
         this.body.close();
      }
   }

   public String toString() {
      return "Response{protocol=" + this.protocol + ", code=" + this.code + ", message=" + this.message + ", url=" + this.request.url() + '}';
   }

   public static class Builder {
      @Nullable
      Request request;
      @Nullable
      Protocol protocol;
      int code = -1;
      String message;
      @Nullable
      Handshake handshake;
      Headers.Builder headers;
      @Nullable
      ResponseBody body;
      @Nullable
      Response networkResponse;
      @Nullable
      Response cacheResponse;
      @Nullable
      Response priorResponse;
      long sentRequestAtMillis;
      long receivedResponseAtMillis;
      @Nullable
      HttpCodec httpCodec;

      public Builder() {
         this.headers = new Headers.Builder();
      }

      Builder(Response response) {
         this.request = response.request;
         this.protocol = response.protocol;
         this.code = response.code;
         this.message = response.message;
         this.handshake = response.handshake;
         this.headers = response.headers.newBuilder();
         this.body = response.body;
         this.networkResponse = response.networkResponse;
         this.cacheResponse = response.cacheResponse;
         this.priorResponse = response.priorResponse;
         this.sentRequestAtMillis = response.sentRequestAtMillis;
         this.receivedResponseAtMillis = response.receivedResponseAtMillis;
         this.httpCodec = response.httpCodec;
      }

      public Response.Builder request(Request request) {
         this.request = request;
         return this;
      }

      public Response.Builder protocol(Protocol protocol) {
         this.protocol = protocol;
         return this;
      }

      public Response.Builder code(int code) {
         this.code = code;
         return this;
      }

      public Response.Builder message(String message) {
         this.message = message;
         return this;
      }

      public Response.Builder handshake(@Nullable Handshake handshake) {
         this.handshake = handshake;
         return this;
      }

      public Response.Builder header(String name, String value) {
         this.headers.set(name, value);
         return this;
      }

      public Response.Builder addHeader(String name, String value) {
         this.headers.add(name, value);
         return this;
      }

      public Response.Builder removeHeader(String name) {
         this.headers.removeAll(name);
         return this;
      }

      public Response.Builder headers(Headers headers) {
         this.headers = headers.newBuilder();
         return this;
      }

      public Response.Builder body(@Nullable ResponseBody body) {
         this.body = body;
         return this;
      }

      public Response.Builder networkResponse(@Nullable Response networkResponse) {
         if (networkResponse != null) {
            this.checkSupportResponse("networkResponse", networkResponse);
         }

         this.networkResponse = networkResponse;
         return this;
      }

      public Response.Builder cacheResponse(@Nullable Response cacheResponse) {
         if (cacheResponse != null) {
            this.checkSupportResponse("cacheResponse", cacheResponse);
         }

         this.cacheResponse = cacheResponse;
         return this;
      }

      private void checkSupportResponse(String name, Response response) {
         if (response.body != null) {
            throw new IllegalArgumentException(name + ".body != null");
         } else if (response.networkResponse != null) {
            throw new IllegalArgumentException(name + ".networkResponse != null");
         } else if (response.cacheResponse != null) {
            throw new IllegalArgumentException(name + ".cacheResponse != null");
         } else if (response.priorResponse != null) {
            throw new IllegalArgumentException(name + ".priorResponse != null");
         }
      }

      public Response.Builder priorResponse(@Nullable Response priorResponse) {
         if (priorResponse != null) {
            this.checkPriorResponse(priorResponse);
         }

         this.priorResponse = priorResponse;
         return this;
      }

      private void checkPriorResponse(Response response) {
         if (response.body != null) {
            throw new IllegalArgumentException("priorResponse.body != null");
         }
      }

      public Response.Builder sentRequestAtMillis(long sentRequestAtMillis) {
         this.sentRequestAtMillis = sentRequestAtMillis;
         return this;
      }

      public Response.Builder receivedResponseAtMillis(long receivedResponseAtMillis) {
         this.receivedResponseAtMillis = receivedResponseAtMillis;
         return this;
      }

      void initCodec(HttpCodec httpCodec) {
         this.httpCodec = httpCodec;
      }

      public Response build() {
         if (this.request == null) {
            throw new IllegalStateException("request == null");
         } else if (this.protocol == null) {
            throw new IllegalStateException("protocol == null");
         } else if (this.code < 0) {
            throw new IllegalStateException("code < 0: " + this.code);
         } else if (this.message == null) {
            throw new IllegalStateException("message == null");
         } else {
            return new Response(this);
         }
      }
   }
}

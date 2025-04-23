package okhttp3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

public final class MultipartBody extends RequestBody {
   public static final MediaType MIXED = MediaType.get("multipart/mixed");
   public static final MediaType ALTERNATIVE = MediaType.get("multipart/alternative");
   public static final MediaType DIGEST = MediaType.get("multipart/digest");
   public static final MediaType PARALLEL = MediaType.get("multipart/parallel");
   public static final MediaType FORM = MediaType.get("multipart/form-data");
   private static final byte[] COLONSPACE = new byte[]{58, 32};
   private static final byte[] CRLF = new byte[]{13, 10};
   private static final byte[] DASHDASH = new byte[]{45, 45};
   private final ByteString boundary;
   private final MediaType originalType;
   private final MediaType contentType;
   private final List<MultipartBody.Part> parts;
   private long contentLength = -1L;

   MultipartBody(ByteString boundary, MediaType type, List<MultipartBody.Part> parts) {
      this.boundary = boundary;
      this.originalType = type;
      this.contentType = MediaType.get(type + "; boundary=" + boundary.utf8());
      this.parts = Util.immutableList(parts);
   }

   public MediaType type() {
      return this.originalType;
   }

   public String boundary() {
      return this.boundary.utf8();
   }

   public int size() {
      return this.parts.size();
   }

   public List<MultipartBody.Part> parts() {
      return this.parts;
   }

   public MultipartBody.Part part(int index) {
      return (MultipartBody.Part)this.parts.get(index);
   }

   public MediaType contentType() {
      return this.contentType;
   }

   public long contentLength() throws IOException {
      long result = this.contentLength;
      return result != -1L ? result : (this.contentLength = this.writeOrCountBytes((BufferedSink)null, true));
   }

   public void writeTo(BufferedSink sink) throws IOException {
      this.writeOrCountBytes(sink, false);
   }

   private long writeOrCountBytes(@Nullable BufferedSink sink, boolean countBytes) throws IOException {
      long byteCount = 0L;
      Buffer byteCountBuffer = null;
      if (countBytes) {
         sink = byteCountBuffer = new Buffer();
      }

      int p = 0;

      for(int partCount = this.parts.size(); p < partCount; ++p) {
         MultipartBody.Part part = (MultipartBody.Part)this.parts.get(p);
         Headers headers = part.headers;
         RequestBody body = part.body;
         ((BufferedSink)sink).write(DASHDASH);
         ((BufferedSink)sink).write(this.boundary);
         ((BufferedSink)sink).write(CRLF);
         if (headers != null) {
            int h = 0;

            for(int headerCount = headers.size(); h < headerCount; ++h) {
               ((BufferedSink)sink).writeUtf8(headers.name(h)).write(COLONSPACE).writeUtf8(headers.value(h)).write(CRLF);
            }
         }

         MediaType contentType = body.contentType();
         if (contentType != null) {
            ((BufferedSink)sink).writeUtf8("Content-Type: ").writeUtf8(contentType.toString()).write(CRLF);
         }

         long contentLength = body.contentLength();
         if (contentLength != -1L) {
            ((BufferedSink)sink).writeUtf8("Content-Length: ").writeDecimalLong(contentLength).write(CRLF);
         } else if (countBytes) {
            byteCountBuffer.clear();
            return -1L;
         }

         ((BufferedSink)sink).write(CRLF);
         if (countBytes) {
            byteCount += contentLength;
         } else {
            body.writeTo((BufferedSink)sink);
         }

         ((BufferedSink)sink).write(CRLF);
      }

      ((BufferedSink)sink).write(DASHDASH);
      ((BufferedSink)sink).write(this.boundary);
      ((BufferedSink)sink).write(DASHDASH);
      ((BufferedSink)sink).write(CRLF);
      if (countBytes) {
         byteCount += byteCountBuffer.size();
         byteCountBuffer.clear();
      }

      return byteCount;
   }

   static void appendQuotedString(StringBuilder target, String key) {
      target.append('"');
      int i = 0;

      for(int len = key.length(); i < len; ++i) {
         char ch = key.charAt(i);
         switch(ch) {
         case '\n':
            target.append("%0A");
            break;
         case '\r':
            target.append("%0D");
            break;
         case '"':
            target.append("%22");
            break;
         default:
            target.append(ch);
         }
      }

      target.append('"');
   }

   public static final class Builder {
      private final ByteString boundary;
      private MediaType type;
      private final List<MultipartBody.Part> parts;

      public Builder() {
         this(UUID.randomUUID().toString());
      }

      public Builder(String boundary) {
         this.type = MultipartBody.MIXED;
         this.parts = new ArrayList();
         this.boundary = ByteString.encodeUtf8(boundary);
      }

      public MultipartBody.Builder setType(MediaType type) {
         if (type == null) {
            throw new NullPointerException("type == null");
         } else if (!type.type().equals("multipart")) {
            throw new IllegalArgumentException("multipart != " + type);
         } else {
            this.type = type;
            return this;
         }
      }

      public MultipartBody.Builder addPart(RequestBody body) {
         return this.addPart(MultipartBody.Part.create(body));
      }

      public MultipartBody.Builder addPart(@Nullable Headers headers, RequestBody body) {
         return this.addPart(MultipartBody.Part.create(headers, body));
      }

      public MultipartBody.Builder addFormDataPart(String name, String value) {
         return this.addPart(MultipartBody.Part.createFormData(name, value));
      }

      public MultipartBody.Builder addFormDataPart(String name, @Nullable String filename, RequestBody body) {
         return this.addPart(MultipartBody.Part.createFormData(name, filename, body));
      }

      public MultipartBody.Builder addPart(MultipartBody.Part part) {
         if (part == null) {
            throw new NullPointerException("part == null");
         } else {
            this.parts.add(part);
            return this;
         }
      }

      public MultipartBody build() {
         if (this.parts.isEmpty()) {
            throw new IllegalStateException("Multipart body must have at least one part.");
         } else {
            return new MultipartBody(this.boundary, this.type, this.parts);
         }
      }
   }

   public static final class Part {
      @Nullable
      final Headers headers;
      final RequestBody body;

      public static MultipartBody.Part create(RequestBody body) {
         return create((Headers)null, body);
      }

      public static MultipartBody.Part create(@Nullable Headers headers, RequestBody body) {
         if (body == null) {
            throw new NullPointerException("body == null");
         } else if (headers != null && headers.get("Content-Type") != null) {
            throw new IllegalArgumentException("Unexpected header: Content-Type");
         } else if (headers != null && headers.get("Content-Length") != null) {
            throw new IllegalArgumentException("Unexpected header: Content-Length");
         } else {
            return new MultipartBody.Part(headers, body);
         }
      }

      public static MultipartBody.Part createFormData(String name, String value) {
         return createFormData(name, (String)null, RequestBody.create((MediaType)null, (String)value));
      }

      public static MultipartBody.Part createFormData(String name, @Nullable String filename, RequestBody body) {
         if (name == null) {
            throw new NullPointerException("name == null");
         } else {
            StringBuilder disposition = new StringBuilder("form-data; name=");
            MultipartBody.appendQuotedString(disposition, name);
            if (filename != null) {
               disposition.append("; filename=");
               MultipartBody.appendQuotedString(disposition, filename);
            }

            Headers headers = (new Headers.Builder()).addUnsafeNonAscii("Content-Disposition", disposition.toString()).build();
            return create(headers, body);
         }
      }

      private Part(@Nullable Headers headers, RequestBody body) {
         this.headers = headers;
         this.body = body;
      }

      @Nullable
      public Headers headers() {
         return this.headers;
      }

      public RequestBody body() {
         return this.body;
      }
   }
}

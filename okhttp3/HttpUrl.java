package okhttp3;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import okhttp3.internal.Util;
import okhttp3.internal.publicsuffix.PublicSuffixDatabase;
import okio.Buffer;

public final class HttpUrl {
   private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
   static final String USERNAME_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
   static final String PASSWORD_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
   static final String PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#";
   static final String PATH_SEGMENT_ENCODE_SET_URI = "[]";
   static final String QUERY_ENCODE_SET = " \"'<>#";
   static final String QUERY_COMPONENT_REENCODE_SET = " \"'<>#&=";
   static final String QUERY_COMPONENT_ENCODE_SET = " !\"#$&'(),/:;<=>?@[]\\^`{|}~";
   static final String QUERY_COMPONENT_ENCODE_SET_URI = "\\^`{|}";
   static final String FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~";
   static final String FRAGMENT_ENCODE_SET = "";
   static final String FRAGMENT_ENCODE_SET_URI = " \"#<>\\^`{|}";
   final String scheme;
   private final String username;
   private final String password;
   final String host;
   final int port;
   private final List<String> pathSegments;
   @Nullable
   private final List<String> queryNamesAndValues;
   @Nullable
   private final String fragment;
   private final String url;

   HttpUrl(HttpUrl.Builder builder) {
      this.scheme = builder.scheme;
      this.username = percentDecode(builder.encodedUsername, false);
      this.password = percentDecode(builder.encodedPassword, false);
      this.host = builder.host;
      this.port = builder.effectivePort();
      this.pathSegments = this.percentDecode(builder.encodedPathSegments, false);
      this.queryNamesAndValues = builder.encodedQueryNamesAndValues != null ? this.percentDecode(builder.encodedQueryNamesAndValues, true) : null;
      this.fragment = builder.encodedFragment != null ? percentDecode(builder.encodedFragment, false) : null;
      this.url = builder.toString();
   }

   public URL url() {
      try {
         return new URL(this.url);
      } catch (MalformedURLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public URI uri() {
      String uri = this.newBuilder().reencodeForUri().toString();

      try {
         return new URI(uri);
      } catch (URISyntaxException var5) {
         try {
            String stripped = uri.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\p{javaWhitespace}]", "");
            return URI.create(stripped);
         } catch (Exception var4) {
            throw new RuntimeException(var5);
         }
      }
   }

   public String scheme() {
      return this.scheme;
   }

   public boolean isHttps() {
      return this.scheme.equals("https");
   }

   public String encodedUsername() {
      if (this.username.isEmpty()) {
         return "";
      } else {
         int usernameStart = this.scheme.length() + 3;
         int usernameEnd = Util.delimiterOffset(this.url, usernameStart, this.url.length(), ":@");
         return this.url.substring(usernameStart, usernameEnd);
      }
   }

   public String username() {
      return this.username;
   }

   public String encodedPassword() {
      if (this.password.isEmpty()) {
         return "";
      } else {
         int passwordStart = this.url.indexOf(58, this.scheme.length() + 3) + 1;
         int passwordEnd = this.url.indexOf(64);
         return this.url.substring(passwordStart, passwordEnd);
      }
   }

   public String password() {
      return this.password;
   }

   public String host() {
      return this.host;
   }

   public int port() {
      return this.port;
   }

   public static int defaultPort(String scheme) {
      if (scheme.equals("http")) {
         return 80;
      } else {
         return scheme.equals("https") ? 443 : -1;
      }
   }

   public int pathSize() {
      return this.pathSegments.size();
   }

   public String encodedPath() {
      int pathStart = this.url.indexOf(47, this.scheme.length() + 3);
      int pathEnd = Util.delimiterOffset(this.url, pathStart, this.url.length(), "?#");
      return this.url.substring(pathStart, pathEnd);
   }

   static void pathSegmentsToString(StringBuilder out, List<String> pathSegments) {
      int i = 0;

      for(int size = pathSegments.size(); i < size; ++i) {
         out.append('/');
         out.append((String)pathSegments.get(i));
      }

   }

   public List<String> encodedPathSegments() {
      int pathStart = this.url.indexOf(47, this.scheme.length() + 3);
      int pathEnd = Util.delimiterOffset(this.url, pathStart, this.url.length(), "?#");
      List<String> result = new ArrayList();

      int segmentEnd;
      for(int i = pathStart; i < pathEnd; i = segmentEnd) {
         ++i;
         segmentEnd = Util.delimiterOffset(this.url, i, pathEnd, '/');
         result.add(this.url.substring(i, segmentEnd));
      }

      return result;
   }

   public List<String> pathSegments() {
      return this.pathSegments;
   }

   @Nullable
   public String encodedQuery() {
      if (this.queryNamesAndValues == null) {
         return null;
      } else {
         int queryStart = this.url.indexOf(63) + 1;
         int queryEnd = Util.delimiterOffset(this.url, queryStart, this.url.length(), '#');
         return this.url.substring(queryStart, queryEnd);
      }
   }

   static void namesAndValuesToQueryString(StringBuilder out, List<String> namesAndValues) {
      int i = 0;

      for(int size = namesAndValues.size(); i < size; i += 2) {
         String name = (String)namesAndValues.get(i);
         String value = (String)namesAndValues.get(i + 1);
         if (i > 0) {
            out.append('&');
         }

         out.append(name);
         if (value != null) {
            out.append('=');
            out.append(value);
         }
      }

   }

   static List<String> queryStringToNamesAndValues(String encodedQuery) {
      List<String> result = new ArrayList();

      int ampersandOffset;
      for(int pos = 0; pos <= encodedQuery.length(); pos = ampersandOffset + 1) {
         ampersandOffset = encodedQuery.indexOf(38, pos);
         if (ampersandOffset == -1) {
            ampersandOffset = encodedQuery.length();
         }

         int equalsOffset = encodedQuery.indexOf(61, pos);
         if (equalsOffset != -1 && equalsOffset <= ampersandOffset) {
            result.add(encodedQuery.substring(pos, equalsOffset));
            result.add(encodedQuery.substring(equalsOffset + 1, ampersandOffset));
         } else {
            result.add(encodedQuery.substring(pos, ampersandOffset));
            result.add((Object)null);
         }
      }

      return result;
   }

   @Nullable
   public String query() {
      if (this.queryNamesAndValues == null) {
         return null;
      } else {
         StringBuilder result = new StringBuilder();
         namesAndValuesToQueryString(result, this.queryNamesAndValues);
         return result.toString();
      }
   }

   public int querySize() {
      return this.queryNamesAndValues != null ? this.queryNamesAndValues.size() / 2 : 0;
   }

   @Nullable
   public String queryParameter(String name) {
      if (this.queryNamesAndValues == null) {
         return null;
      } else {
         int i = 0;

         for(int size = this.queryNamesAndValues.size(); i < size; i += 2) {
            if (name.equals(this.queryNamesAndValues.get(i))) {
               return (String)this.queryNamesAndValues.get(i + 1);
            }
         }

         return null;
      }
   }

   public Set<String> queryParameterNames() {
      if (this.queryNamesAndValues == null) {
         return Collections.emptySet();
      } else {
         Set<String> result = new LinkedHashSet();
         int i = 0;

         for(int size = this.queryNamesAndValues.size(); i < size; i += 2) {
            result.add((String)this.queryNamesAndValues.get(i));
         }

         return Collections.unmodifiableSet(result);
      }
   }

   public List<String> queryParameterValues(String name) {
      if (this.queryNamesAndValues == null) {
         return Collections.emptyList();
      } else {
         List<String> result = new ArrayList();
         int i = 0;

         for(int size = this.queryNamesAndValues.size(); i < size; i += 2) {
            if (name.equals(this.queryNamesAndValues.get(i))) {
               result.add((String)this.queryNamesAndValues.get(i + 1));
            }
         }

         return Collections.unmodifiableList(result);
      }
   }

   public String queryParameterName(int index) {
      if (this.queryNamesAndValues == null) {
         throw new IndexOutOfBoundsException();
      } else {
         return (String)this.queryNamesAndValues.get(index * 2);
      }
   }

   public String queryParameterValue(int index) {
      if (this.queryNamesAndValues == null) {
         throw new IndexOutOfBoundsException();
      } else {
         return (String)this.queryNamesAndValues.get(index * 2 + 1);
      }
   }

   @Nullable
   public String encodedFragment() {
      if (this.fragment == null) {
         return null;
      } else {
         int fragmentStart = this.url.indexOf(35) + 1;
         return this.url.substring(fragmentStart);
      }
   }

   @Nullable
   public String fragment() {
      return this.fragment;
   }

   public String redact() {
      return this.newBuilder("/...").username("").password("").build().toString();
   }

   @Nullable
   public HttpUrl resolve(String link) {
      HttpUrl.Builder builder = this.newBuilder(link);
      return builder != null ? builder.build() : null;
   }

   public HttpUrl.Builder newBuilder() {
      HttpUrl.Builder result = new HttpUrl.Builder();
      result.scheme = this.scheme;
      result.encodedUsername = this.encodedUsername();
      result.encodedPassword = this.encodedPassword();
      result.host = this.host;
      result.port = this.port != defaultPort(this.scheme) ? this.port : -1;
      result.encodedPathSegments.clear();
      result.encodedPathSegments.addAll(this.encodedPathSegments());
      result.encodedQuery(this.encodedQuery());
      result.encodedFragment = this.encodedFragment();
      return result;
   }

   @Nullable
   public HttpUrl.Builder newBuilder(String link) {
      try {
         return (new HttpUrl.Builder()).parse(this, link);
      } catch (IllegalArgumentException var3) {
         return null;
      }
   }

   @Nullable
   public static HttpUrl parse(String url) {
      try {
         return get(url);
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   public static HttpUrl get(String url) {
      return (new HttpUrl.Builder()).parse((HttpUrl)null, url).build();
   }

   @Nullable
   public static HttpUrl get(URL url) {
      return parse(url.toString());
   }

   @Nullable
   public static HttpUrl get(URI uri) {
      return parse(uri.toString());
   }

   public boolean equals(@Nullable Object other) {
      return other instanceof HttpUrl && ((HttpUrl)other).url.equals(this.url);
   }

   public int hashCode() {
      return this.url.hashCode();
   }

   public String toString() {
      return this.url;
   }

   @Nullable
   public String topPrivateDomain() {
      return Util.verifyAsIpAddress(this.host) ? null : PublicSuffixDatabase.get().getEffectiveTldPlusOne(this.host);
   }

   static String percentDecode(String encoded, boolean plusIsSpace) {
      return percentDecode(encoded, 0, encoded.length(), plusIsSpace);
   }

   private List<String> percentDecode(List<String> list, boolean plusIsSpace) {
      int size = list.size();
      List<String> result = new ArrayList(size);

      for(int i = 0; i < size; ++i) {
         String s = (String)list.get(i);
         result.add(s != null ? percentDecode(s, plusIsSpace) : null);
      }

      return Collections.unmodifiableList(result);
   }

   static String percentDecode(String encoded, int pos, int limit, boolean plusIsSpace) {
      for(int i = pos; i < limit; ++i) {
         char c = encoded.charAt(i);
         if (c == '%' || c == '+' && plusIsSpace) {
            Buffer out = new Buffer();
            out.writeUtf8(encoded, pos, i);
            percentDecode(out, encoded, i, limit, plusIsSpace);
            return out.readUtf8();
         }
      }

      return encoded.substring(pos, limit);
   }

   static void percentDecode(Buffer out, String encoded, int pos, int limit, boolean plusIsSpace) {
      int codePoint;
      for(int i = pos; i < limit; i += Character.charCount(codePoint)) {
         codePoint = encoded.codePointAt(i);
         if (codePoint == 37 && i + 2 < limit) {
            int d1 = Util.decodeHexDigit(encoded.charAt(i + 1));
            int d2 = Util.decodeHexDigit(encoded.charAt(i + 2));
            if (d1 != -1 && d2 != -1) {
               out.writeByte((d1 << 4) + d2);
               i += 2;
               continue;
            }
         } else if (codePoint == 43 && plusIsSpace) {
            out.writeByte(32);
            continue;
         }

         out.writeUtf8CodePoint(codePoint);
      }

   }

   static boolean percentEncoded(String encoded, int pos, int limit) {
      return pos + 2 < limit && encoded.charAt(pos) == '%' && Util.decodeHexDigit(encoded.charAt(pos + 1)) != -1 && Util.decodeHexDigit(encoded.charAt(pos + 2)) != -1;
   }

   static String canonicalize(String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly, @Nullable Charset charset) {
      int codePoint;
      for(int i = pos; i < limit; i += Character.charCount(codePoint)) {
         codePoint = input.codePointAt(i);
         if (codePoint < 32 || codePoint == 127 || codePoint >= 128 && asciiOnly || encodeSet.indexOf(codePoint) != -1 || codePoint == 37 && (!alreadyEncoded || strict && !percentEncoded(input, i, limit)) || codePoint == 43 && plusIsSpace) {
            Buffer out = new Buffer();
            out.writeUtf8(input, pos, i);
            canonicalize(out, input, i, limit, encodeSet, alreadyEncoded, strict, plusIsSpace, asciiOnly, charset);
            return out.readUtf8();
         }
      }

      return input.substring(pos, limit);
   }

   static void canonicalize(Buffer out, String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly, @Nullable Charset charset) {
      Buffer encodedCharBuffer = null;

      int codePoint;
      for(int i = pos; i < limit; i += Character.charCount(codePoint)) {
         codePoint = input.codePointAt(i);
         if (!alreadyEncoded || codePoint != 9 && codePoint != 10 && codePoint != 12 && codePoint != 13) {
            if (codePoint == 43 && plusIsSpace) {
               out.writeUtf8(alreadyEncoded ? "+" : "%2B");
            } else if (codePoint < 32 || codePoint == 127 || codePoint >= 128 && asciiOnly || encodeSet.indexOf(codePoint) != -1 || codePoint == 37 && (!alreadyEncoded || strict && !percentEncoded(input, i, limit))) {
               if (encodedCharBuffer == null) {
                  encodedCharBuffer = new Buffer();
               }

               if (charset != null && !charset.equals(StandardCharsets.UTF_8)) {
                  encodedCharBuffer.writeString(input, i, i + Character.charCount(codePoint), charset);
               } else {
                  encodedCharBuffer.writeUtf8CodePoint(codePoint);
               }

               while(!encodedCharBuffer.exhausted()) {
                  int b = encodedCharBuffer.readByte() & 255;
                  out.writeByte(37);
                  out.writeByte(HEX_DIGITS[b >> 4 & 15]);
                  out.writeByte(HEX_DIGITS[b & 15]);
               }
            } else {
               out.writeUtf8CodePoint(codePoint);
            }
         }
      }

   }

   static String canonicalize(String input, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly, @Nullable Charset charset) {
      return canonicalize(input, 0, input.length(), encodeSet, alreadyEncoded, strict, plusIsSpace, asciiOnly, charset);
   }

   static String canonicalize(String input, String encodeSet, boolean alreadyEncoded, boolean strict, boolean plusIsSpace, boolean asciiOnly) {
      return canonicalize(input, 0, input.length(), encodeSet, alreadyEncoded, strict, plusIsSpace, asciiOnly, (Charset)null);
   }

   public static final class Builder {
      @Nullable
      String scheme;
      String encodedUsername = "";
      String encodedPassword = "";
      @Nullable
      String host;
      int port = -1;
      final List<String> encodedPathSegments = new ArrayList();
      @Nullable
      List<String> encodedQueryNamesAndValues;
      @Nullable
      String encodedFragment;
      static final String INVALID_HOST = "Invalid URL host";

      public Builder() {
         this.encodedPathSegments.add("");
      }

      public HttpUrl.Builder scheme(String scheme) {
         if (scheme == null) {
            throw new NullPointerException("scheme == null");
         } else {
            if (scheme.equalsIgnoreCase("http")) {
               this.scheme = "http";
            } else {
               if (!scheme.equalsIgnoreCase("https")) {
                  throw new IllegalArgumentException("unexpected scheme: " + scheme);
               }

               this.scheme = "https";
            }

            return this;
         }
      }

      public HttpUrl.Builder username(String username) {
         if (username == null) {
            throw new NullPointerException("username == null");
         } else {
            this.encodedUsername = HttpUrl.canonicalize(username, " \"':;<=>@[]^`{}|/\\?#", false, false, false, true);
            return this;
         }
      }

      public HttpUrl.Builder encodedUsername(String encodedUsername) {
         if (encodedUsername == null) {
            throw new NullPointerException("encodedUsername == null");
         } else {
            this.encodedUsername = HttpUrl.canonicalize(encodedUsername, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true);
            return this;
         }
      }

      public HttpUrl.Builder password(String password) {
         if (password == null) {
            throw new NullPointerException("password == null");
         } else {
            this.encodedPassword = HttpUrl.canonicalize(password, " \"':;<=>@[]^`{}|/\\?#", false, false, false, true);
            return this;
         }
      }

      public HttpUrl.Builder encodedPassword(String encodedPassword) {
         if (encodedPassword == null) {
            throw new NullPointerException("encodedPassword == null");
         } else {
            this.encodedPassword = HttpUrl.canonicalize(encodedPassword, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true);
            return this;
         }
      }

      public HttpUrl.Builder host(String host) {
         if (host == null) {
            throw new NullPointerException("host == null");
         } else {
            String encoded = canonicalizeHost(host, 0, host.length());
            if (encoded == null) {
               throw new IllegalArgumentException("unexpected host: " + host);
            } else {
               this.host = encoded;
               return this;
            }
         }
      }

      public HttpUrl.Builder port(int port) {
         if (port > 0 && port <= 65535) {
            this.port = port;
            return this;
         } else {
            throw new IllegalArgumentException("unexpected port: " + port);
         }
      }

      int effectivePort() {
         return this.port != -1 ? this.port : HttpUrl.defaultPort(this.scheme);
      }

      public HttpUrl.Builder addPathSegment(String pathSegment) {
         if (pathSegment == null) {
            throw new NullPointerException("pathSegment == null");
         } else {
            this.push(pathSegment, 0, pathSegment.length(), false, false);
            return this;
         }
      }

      public HttpUrl.Builder addPathSegments(String pathSegments) {
         if (pathSegments == null) {
            throw new NullPointerException("pathSegments == null");
         } else {
            return this.addPathSegments(pathSegments, false);
         }
      }

      public HttpUrl.Builder addEncodedPathSegment(String encodedPathSegment) {
         if (encodedPathSegment == null) {
            throw new NullPointerException("encodedPathSegment == null");
         } else {
            this.push(encodedPathSegment, 0, encodedPathSegment.length(), false, true);
            return this;
         }
      }

      public HttpUrl.Builder addEncodedPathSegments(String encodedPathSegments) {
         if (encodedPathSegments == null) {
            throw new NullPointerException("encodedPathSegments == null");
         } else {
            return this.addPathSegments(encodedPathSegments, true);
         }
      }

      private HttpUrl.Builder addPathSegments(String pathSegments, boolean alreadyEncoded) {
         int offset = 0;

         do {
            int segmentEnd = Util.delimiterOffset(pathSegments, offset, pathSegments.length(), "/\\");
            boolean addTrailingSlash = segmentEnd < pathSegments.length();
            this.push(pathSegments, offset, segmentEnd, addTrailingSlash, alreadyEncoded);
            offset = segmentEnd + 1;
         } while(offset <= pathSegments.length());

         return this;
      }

      public HttpUrl.Builder setPathSegment(int index, String pathSegment) {
         if (pathSegment == null) {
            throw new NullPointerException("pathSegment == null");
         } else {
            String canonicalPathSegment = HttpUrl.canonicalize(pathSegment, 0, pathSegment.length(), " \"<>^`{}|/\\?#", false, false, false, true, (Charset)null);
            if (!this.isDot(canonicalPathSegment) && !this.isDotDot(canonicalPathSegment)) {
               this.encodedPathSegments.set(index, canonicalPathSegment);
               return this;
            } else {
               throw new IllegalArgumentException("unexpected path segment: " + pathSegment);
            }
         }
      }

      public HttpUrl.Builder setEncodedPathSegment(int index, String encodedPathSegment) {
         if (encodedPathSegment == null) {
            throw new NullPointerException("encodedPathSegment == null");
         } else {
            String canonicalPathSegment = HttpUrl.canonicalize(encodedPathSegment, 0, encodedPathSegment.length(), " \"<>^`{}|/\\?#", true, false, false, true, (Charset)null);
            this.encodedPathSegments.set(index, canonicalPathSegment);
            if (!this.isDot(canonicalPathSegment) && !this.isDotDot(canonicalPathSegment)) {
               return this;
            } else {
               throw new IllegalArgumentException("unexpected path segment: " + encodedPathSegment);
            }
         }
      }

      public HttpUrl.Builder removePathSegment(int index) {
         this.encodedPathSegments.remove(index);
         if (this.encodedPathSegments.isEmpty()) {
            this.encodedPathSegments.add("");
         }

         return this;
      }

      public HttpUrl.Builder encodedPath(String encodedPath) {
         if (encodedPath == null) {
            throw new NullPointerException("encodedPath == null");
         } else if (!encodedPath.startsWith("/")) {
            throw new IllegalArgumentException("unexpected encodedPath: " + encodedPath);
         } else {
            this.resolvePath(encodedPath, 0, encodedPath.length());
            return this;
         }
      }

      public HttpUrl.Builder query(@Nullable String query) {
         this.encodedQueryNamesAndValues = query != null ? HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(query, " \"'<>#", false, false, true, true)) : null;
         return this;
      }

      public HttpUrl.Builder encodedQuery(@Nullable String encodedQuery) {
         this.encodedQueryNamesAndValues = encodedQuery != null ? HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(encodedQuery, " \"'<>#", true, false, true, true)) : null;
         return this;
      }

      public HttpUrl.Builder addQueryParameter(String name, @Nullable String value) {
         if (name == null) {
            throw new NullPointerException("name == null");
         } else {
            if (this.encodedQueryNamesAndValues == null) {
               this.encodedQueryNamesAndValues = new ArrayList();
            }

            this.encodedQueryNamesAndValues.add(HttpUrl.canonicalize(name, " !\"#$&'(),/:;<=>?@[]\\^`{|}~", false, false, true, true));
            this.encodedQueryNamesAndValues.add(value != null ? HttpUrl.canonicalize(value, " !\"#$&'(),/:;<=>?@[]\\^`{|}~", false, false, true, true) : null);
            return this;
         }
      }

      public HttpUrl.Builder addEncodedQueryParameter(String encodedName, @Nullable String encodedValue) {
         if (encodedName == null) {
            throw new NullPointerException("encodedName == null");
         } else {
            if (this.encodedQueryNamesAndValues == null) {
               this.encodedQueryNamesAndValues = new ArrayList();
            }

            this.encodedQueryNamesAndValues.add(HttpUrl.canonicalize(encodedName, " \"'<>#&=", true, false, true, true));
            this.encodedQueryNamesAndValues.add(encodedValue != null ? HttpUrl.canonicalize(encodedValue, " \"'<>#&=", true, false, true, true) : null);
            return this;
         }
      }

      public HttpUrl.Builder setQueryParameter(String name, @Nullable String value) {
         this.removeAllQueryParameters(name);
         this.addQueryParameter(name, value);
         return this;
      }

      public HttpUrl.Builder setEncodedQueryParameter(String encodedName, @Nullable String encodedValue) {
         this.removeAllEncodedQueryParameters(encodedName);
         this.addEncodedQueryParameter(encodedName, encodedValue);
         return this;
      }

      public HttpUrl.Builder removeAllQueryParameters(String name) {
         if (name == null) {
            throw new NullPointerException("name == null");
         } else if (this.encodedQueryNamesAndValues == null) {
            return this;
         } else {
            String nameToRemove = HttpUrl.canonicalize(name, " !\"#$&'(),/:;<=>?@[]\\^`{|}~", false, false, true, true);
            this.removeAllCanonicalQueryParameters(nameToRemove);
            return this;
         }
      }

      public HttpUrl.Builder removeAllEncodedQueryParameters(String encodedName) {
         if (encodedName == null) {
            throw new NullPointerException("encodedName == null");
         } else if (this.encodedQueryNamesAndValues == null) {
            return this;
         } else {
            this.removeAllCanonicalQueryParameters(HttpUrl.canonicalize(encodedName, " \"'<>#&=", true, false, true, true));
            return this;
         }
      }

      private void removeAllCanonicalQueryParameters(String canonicalName) {
         for(int i = this.encodedQueryNamesAndValues.size() - 2; i >= 0; i -= 2) {
            if (canonicalName.equals(this.encodedQueryNamesAndValues.get(i))) {
               this.encodedQueryNamesAndValues.remove(i + 1);
               this.encodedQueryNamesAndValues.remove(i);
               if (this.encodedQueryNamesAndValues.isEmpty()) {
                  this.encodedQueryNamesAndValues = null;
                  return;
               }
            }
         }

      }

      public HttpUrl.Builder fragment(@Nullable String fragment) {
         this.encodedFragment = fragment != null ? HttpUrl.canonicalize(fragment, "", false, false, false, false) : null;
         return this;
      }

      public HttpUrl.Builder encodedFragment(@Nullable String encodedFragment) {
         this.encodedFragment = encodedFragment != null ? HttpUrl.canonicalize(encodedFragment, "", true, false, false, false) : null;
         return this;
      }

      HttpUrl.Builder reencodeForUri() {
         int i = 0;

         int size;
         String component;
         for(size = this.encodedPathSegments.size(); i < size; ++i) {
            component = (String)this.encodedPathSegments.get(i);
            this.encodedPathSegments.set(i, HttpUrl.canonicalize(component, "[]", true, true, false, true));
         }

         if (this.encodedQueryNamesAndValues != null) {
            i = 0;

            for(size = this.encodedQueryNamesAndValues.size(); i < size; ++i) {
               component = (String)this.encodedQueryNamesAndValues.get(i);
               if (component != null) {
                  this.encodedQueryNamesAndValues.set(i, HttpUrl.canonicalize(component, "\\^`{|}", true, true, true, true));
               }
            }
         }

         if (this.encodedFragment != null) {
            this.encodedFragment = HttpUrl.canonicalize(this.encodedFragment, " \"#<>\\^`{|}", true, true, false, false);
         }

         return this;
      }

      public HttpUrl build() {
         if (this.scheme == null) {
            throw new IllegalStateException("scheme == null");
         } else if (this.host == null) {
            throw new IllegalStateException("host == null");
         } else {
            return new HttpUrl(this);
         }
      }

      public String toString() {
         StringBuilder result = new StringBuilder();
         if (this.scheme != null) {
            result.append(this.scheme);
            result.append("://");
         } else {
            result.append("//");
         }

         if (!this.encodedUsername.isEmpty() || !this.encodedPassword.isEmpty()) {
            result.append(this.encodedUsername);
            if (!this.encodedPassword.isEmpty()) {
               result.append(':');
               result.append(this.encodedPassword);
            }

            result.append('@');
         }

         if (this.host != null) {
            if (this.host.indexOf(58) != -1) {
               result.append('[');
               result.append(this.host);
               result.append(']');
            } else {
               result.append(this.host);
            }
         }

         if (this.port != -1 || this.scheme != null) {
            int effectivePort = this.effectivePort();
            if (this.scheme == null || effectivePort != HttpUrl.defaultPort(this.scheme)) {
               result.append(':');
               result.append(effectivePort);
            }
         }

         HttpUrl.pathSegmentsToString(result, this.encodedPathSegments);
         if (this.encodedQueryNamesAndValues != null) {
            result.append('?');
            HttpUrl.namesAndValuesToQueryString(result, this.encodedQueryNamesAndValues);
         }

         if (this.encodedFragment != null) {
            result.append('#');
            result.append(this.encodedFragment);
         }

         return result.toString();
      }

      HttpUrl.Builder parse(@Nullable HttpUrl base, String input) {
         int pos = Util.skipLeadingAsciiWhitespace(input, 0, input.length());
         int limit = Util.skipTrailingAsciiWhitespace(input, pos, input.length());
         int schemeDelimiterOffset = schemeDelimiterOffset(input, pos, limit);
         if (schemeDelimiterOffset != -1) {
            if (input.regionMatches(true, pos, "https:", 0, 6)) {
               this.scheme = "https";
               pos += "https:".length();
            } else {
               if (!input.regionMatches(true, pos, "http:", 0, 5)) {
                  throw new IllegalArgumentException("Expected URL scheme 'http' or 'https' but was '" + input.substring(0, schemeDelimiterOffset) + "'");
               }

               this.scheme = "http";
               pos += "http:".length();
            }
         } else {
            if (base == null) {
               throw new IllegalArgumentException("Expected URL scheme 'http' or 'https' but no colon was found");
            }

            this.scheme = base.scheme;
         }

         boolean hasUsername = false;
         boolean hasPassword = false;
         int slashCount = slashCount(input, pos, limit);
         int componentDelimiterOffset;
         int queryDelimiterOffset;
         if (slashCount < 2 && base != null && base.scheme.equals(this.scheme)) {
            this.encodedUsername = base.encodedUsername();
            this.encodedPassword = base.encodedPassword();
            this.host = base.host;
            this.port = base.port;
            this.encodedPathSegments.clear();
            this.encodedPathSegments.addAll(base.encodedPathSegments());
            if (pos == limit || input.charAt(pos) == '#') {
               this.encodedQuery(base.encodedQuery());
            }
         } else {
            pos += slashCount;

            label79:
            while(true) {
               componentDelimiterOffset = Util.delimiterOffset(input, pos, limit, "@/\\?#");
               queryDelimiterOffset = componentDelimiterOffset != limit ? input.charAt(componentDelimiterOffset) : -1;
               int portColonOffset;
               switch(queryDelimiterOffset) {
               case -1:
               case 35:
               case 47:
               case 63:
               case 92:
                  portColonOffset = portColonOffset(input, pos, componentDelimiterOffset);
                  if (portColonOffset + 1 < componentDelimiterOffset) {
                     this.host = canonicalizeHost(input, pos, portColonOffset);
                     this.port = parsePort(input, portColonOffset + 1, componentDelimiterOffset);
                     if (this.port == -1) {
                        throw new IllegalArgumentException("Invalid URL port: \"" + input.substring(portColonOffset + 1, componentDelimiterOffset) + '"');
                     }
                  } else {
                     this.host = canonicalizeHost(input, pos, portColonOffset);
                     this.port = HttpUrl.defaultPort(this.scheme);
                  }

                  if (this.host == null) {
                     throw new IllegalArgumentException("Invalid URL host: \"" + input.substring(pos, portColonOffset) + '"');
                  }

                  pos = componentDelimiterOffset;
                  break label79;
               case 64:
                  if (!hasPassword) {
                     portColonOffset = Util.delimiterOffset(input, pos, componentDelimiterOffset, ':');
                     String canonicalUsername = HttpUrl.canonicalize(input, pos, portColonOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true, (Charset)null);
                     this.encodedUsername = hasUsername ? this.encodedUsername + "%40" + canonicalUsername : canonicalUsername;
                     if (portColonOffset != componentDelimiterOffset) {
                        hasPassword = true;
                        this.encodedPassword = HttpUrl.canonicalize(input, portColonOffset + 1, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true, (Charset)null);
                     }

                     hasUsername = true;
                  } else {
                     this.encodedPassword = this.encodedPassword + "%40" + HttpUrl.canonicalize(input, pos, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, false, true, (Charset)null);
                  }

                  pos = componentDelimiterOffset + 1;
               }
            }
         }

         componentDelimiterOffset = Util.delimiterOffset(input, pos, limit, "?#");
         this.resolvePath(input, pos, componentDelimiterOffset);
         pos = componentDelimiterOffset;
         if (componentDelimiterOffset < limit && input.charAt(componentDelimiterOffset) == '?') {
            queryDelimiterOffset = Util.delimiterOffset(input, componentDelimiterOffset, limit, '#');
            this.encodedQueryNamesAndValues = HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(input, componentDelimiterOffset + 1, queryDelimiterOffset, " \"'<>#", true, false, true, true, (Charset)null));
            pos = queryDelimiterOffset;
         }

         if (pos < limit && input.charAt(pos) == '#') {
            this.encodedFragment = HttpUrl.canonicalize(input, pos + 1, limit, "", true, false, false, false, (Charset)null);
         }

         return this;
      }

      private void resolvePath(String input, int pos, int limit) {
         if (pos != limit) {
            char c = input.charAt(pos);
            if (c != '/' && c != '\\') {
               this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, "");
            } else {
               this.encodedPathSegments.clear();
               this.encodedPathSegments.add("");
               ++pos;
            }

            int i = pos;

            while(i < limit) {
               int pathSegmentDelimiterOffset = Util.delimiterOffset(input, i, limit, "/\\");
               boolean segmentHasTrailingSlash = pathSegmentDelimiterOffset < limit;
               this.push(input, i, pathSegmentDelimiterOffset, segmentHasTrailingSlash, true);
               i = pathSegmentDelimiterOffset;
               if (segmentHasTrailingSlash) {
                  i = pathSegmentDelimiterOffset + 1;
               }
            }

         }
      }

      private void push(String input, int pos, int limit, boolean addTrailingSlash, boolean alreadyEncoded) {
         String segment = HttpUrl.canonicalize(input, pos, limit, " \"<>^`{}|/\\?#", alreadyEncoded, false, false, true, (Charset)null);
         if (!this.isDot(segment)) {
            if (this.isDotDot(segment)) {
               this.pop();
            } else {
               if (((String)this.encodedPathSegments.get(this.encodedPathSegments.size() - 1)).isEmpty()) {
                  this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, segment);
               } else {
                  this.encodedPathSegments.add(segment);
               }

               if (addTrailingSlash) {
                  this.encodedPathSegments.add("");
               }

            }
         }
      }

      private boolean isDot(String input) {
         return input.equals(".") || input.equalsIgnoreCase("%2e");
      }

      private boolean isDotDot(String input) {
         return input.equals("..") || input.equalsIgnoreCase("%2e.") || input.equalsIgnoreCase(".%2e") || input.equalsIgnoreCase("%2e%2e");
      }

      private void pop() {
         String removed = (String)this.encodedPathSegments.remove(this.encodedPathSegments.size() - 1);
         if (removed.isEmpty() && !this.encodedPathSegments.isEmpty()) {
            this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, "");
         } else {
            this.encodedPathSegments.add("");
         }

      }

      private static int schemeDelimiterOffset(String input, int pos, int limit) {
         if (limit - pos < 2) {
            return -1;
         } else {
            char c0 = input.charAt(pos);
            if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) {
               return -1;
            } else {
               for(int i = pos + 1; i < limit; ++i) {
                  char c = input.charAt(i);
                  if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '+' && c != '-' && c != '.') {
                     if (c == ':') {
                        return i;
                     }

                     return -1;
                  }
               }

               return -1;
            }
         }
      }

      private static int slashCount(String input, int pos, int limit) {
         int slashCount;
         for(slashCount = 0; pos < limit; ++pos) {
            char c = input.charAt(pos);
            if (c != '\\' && c != '/') {
               break;
            }

            ++slashCount;
         }

         return slashCount;
      }

      private static int portColonOffset(String input, int pos, int limit) {
         for(int i = pos; i < limit; ++i) {
            switch(input.charAt(i)) {
            case ':':
               return i;
            case '[':
               do {
                  ++i;
               } while(i < limit && input.charAt(i) != ']');
            }
         }

         return limit;
      }

      @Nullable
      private static String canonicalizeHost(String input, int pos, int limit) {
         String percentDecoded = HttpUrl.percentDecode(input, pos, limit, false);
         return Util.canonicalizeHost(percentDecoded);
      }

      private static int parsePort(String input, int pos, int limit) {
         try {
            String portString = HttpUrl.canonicalize(input, pos, limit, "", false, false, false, true, (Charset)null);
            int i = Integer.parseInt(portString);
            return i > 0 && i <= 65535 ? i : -1;
         } catch (NumberFormatException var5) {
            return -1;
         }
      }
   }
}

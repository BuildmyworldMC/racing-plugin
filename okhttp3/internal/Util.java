package okhttp3.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.IDN;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.http2.Header;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.Options;
import okio.Source;

public final class Util {
   public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
   public static final String[] EMPTY_STRING_ARRAY = new String[0];
   public static final Headers EMPTY_HEADERS = Headers.of();
   public static final ResponseBody EMPTY_RESPONSE;
   public static final RequestBody EMPTY_REQUEST;
   private static final Options UNICODE_BOMS;
   private static final Charset UTF_32BE;
   private static final Charset UTF_32LE;
   public static final TimeZone UTC;
   public static final Comparator<String> NATURAL_ORDER;
   private static final Method addSuppressedExceptionMethod;
   private static final Pattern VERIFY_AS_IP_ADDRESS;

   public static void addSuppressedIfPossible(Throwable e, Throwable suppressed) {
      if (addSuppressedExceptionMethod != null) {
         try {
            addSuppressedExceptionMethod.invoke(e, suppressed);
         } catch (IllegalAccessException | InvocationTargetException var3) {
         }
      }

   }

   private Util() {
   }

   public static void checkOffsetAndCount(long arrayLength, long offset, long count) {
      if ((offset | count) < 0L || offset > arrayLength || arrayLength - offset < count) {
         throw new ArrayIndexOutOfBoundsException();
      }
   }

   public static void closeQuietly(Closeable closeable) {
      if (closeable != null) {
         try {
            closeable.close();
         } catch (RuntimeException var2) {
            throw var2;
         } catch (Exception var3) {
         }
      }

   }

   public static void closeQuietly(Socket socket) {
      if (socket != null) {
         try {
            socket.close();
         } catch (AssertionError var2) {
            if (!isAndroidGetsocknameError(var2)) {
               throw var2;
            }
         } catch (RuntimeException var3) {
            throw var3;
         } catch (Exception var4) {
         }
      }

   }

   public static void closeQuietly(ServerSocket serverSocket) {
      if (serverSocket != null) {
         try {
            serverSocket.close();
         } catch (RuntimeException var2) {
            throw var2;
         } catch (Exception var3) {
         }
      }

   }

   public static boolean discard(Source source, int timeout, TimeUnit timeUnit) {
      try {
         return skipAll(source, timeout, timeUnit);
      } catch (IOException var4) {
         return false;
      }
   }

   public static boolean skipAll(Source source, int duration, TimeUnit timeUnit) throws IOException {
      long now = System.nanoTime();
      long originalDuration = source.timeout().hasDeadline() ? source.timeout().deadlineNanoTime() - now : Long.MAX_VALUE;
      source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos((long)duration)));

      boolean var8;
      try {
         Buffer skipBuffer = new Buffer();

         while(source.read(skipBuffer, 8192L) != -1L) {
            skipBuffer.clear();
         }

         var8 = true;
         return var8;
      } catch (InterruptedIOException var12) {
         var8 = false;
      } finally {
         if (originalDuration == Long.MAX_VALUE) {
            source.timeout().clearDeadline();
         } else {
            source.timeout().deadlineNanoTime(now + originalDuration);
         }

      }

      return var8;
   }

   public static <T> List<T> immutableList(List<T> list) {
      return Collections.unmodifiableList(new ArrayList(list));
   }

   public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
      return map.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap(map));
   }

   @SafeVarargs
   public static <T> List<T> immutableList(T... elements) {
      return Collections.unmodifiableList(Arrays.asList((Object[])elements.clone()));
   }

   public static ThreadFactory threadFactory(String name, boolean daemon) {
      return (runnable) -> {
         Thread result = new Thread(runnable, name);
         result.setDaemon(daemon);
         return result;
      };
   }

   public static String[] intersect(Comparator<? super String> comparator, String[] first, String[] second) {
      List<String> result = new ArrayList();
      String[] var4 = first;
      int var5 = first.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String a = var4[var6];
         String[] var8 = second;
         int var9 = second.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            String b = var8[var10];
            if (comparator.compare(a, b) == 0) {
               result.add(a);
               break;
            }
         }
      }

      return (String[])result.toArray(new String[result.size()]);
   }

   public static boolean nonEmptyIntersection(Comparator<String> comparator, String[] first, String[] second) {
      if (first != null && second != null && first.length != 0 && second.length != 0) {
         String[] var3 = first;
         int var4 = first.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String a = var3[var5];
            String[] var7 = second;
            int var8 = second.length;

            for(int var9 = 0; var9 < var8; ++var9) {
               String b = var7[var9];
               if (comparator.compare(a, b) == 0) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static String hostHeader(HttpUrl url, boolean includeDefaultPort) {
      String host = url.host().contains(":") ? "[" + url.host() + "]" : url.host();
      return !includeDefaultPort && url.port() == HttpUrl.defaultPort(url.scheme()) ? host : host + ":" + url.port();
   }

   public static boolean isAndroidGetsocknameError(AssertionError e) {
      return e.getCause() != null && e.getMessage() != null && e.getMessage().contains("getsockname failed");
   }

   public static int indexOf(Comparator<String> comparator, String[] array, String value) {
      int i = 0;

      for(int size = array.length; i < size; ++i) {
         if (comparator.compare(array[i], value) == 0) {
            return i;
         }
      }

      return -1;
   }

   public static String[] concat(String[] array, String value) {
      String[] result = new String[array.length + 1];
      System.arraycopy(array, 0, result, 0, array.length);
      result[result.length - 1] = value;
      return result;
   }

   public static int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
      int i = pos;

      while(i < limit) {
         switch(input.charAt(i)) {
         case '\t':
         case '\n':
         case '\f':
         case '\r':
         case ' ':
            ++i;
            break;
         default:
            return i;
         }
      }

      return limit;
   }

   public static int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
      int i = limit - 1;

      while(i >= pos) {
         switch(input.charAt(i)) {
         case '\t':
         case '\n':
         case '\f':
         case '\r':
         case ' ':
            --i;
            break;
         default:
            return i + 1;
         }
      }

      return pos;
   }

   public static String trimSubstring(String string, int pos, int limit) {
      int start = skipLeadingAsciiWhitespace(string, pos, limit);
      int end = skipTrailingAsciiWhitespace(string, start, limit);
      return string.substring(start, end);
   }

   public static int delimiterOffset(String input, int pos, int limit, String delimiters) {
      for(int i = pos; i < limit; ++i) {
         if (delimiters.indexOf(input.charAt(i)) != -1) {
            return i;
         }
      }

      return limit;
   }

   public static int delimiterOffset(String input, int pos, int limit, char delimiter) {
      for(int i = pos; i < limit; ++i) {
         if (input.charAt(i) == delimiter) {
            return i;
         }
      }

      return limit;
   }

   public static String canonicalizeHost(String host) {
      if (!host.contains(":")) {
         try {
            String result = IDN.toASCII(host).toLowerCase(Locale.US);
            if (result.isEmpty()) {
               return null;
            } else {
               return containsInvalidHostnameAsciiCodes(result) ? null : result;
            }
         } catch (IllegalArgumentException var3) {
            return null;
         }
      } else {
         InetAddress inetAddress = host.startsWith("[") && host.endsWith("]") ? decodeIpv6(host, 1, host.length() - 1) : decodeIpv6(host, 0, host.length());
         if (inetAddress == null) {
            return null;
         } else {
            byte[] address = inetAddress.getAddress();
            if (address.length == 16) {
               return inet6AddressToAscii(address);
            } else if (address.length == 4) {
               return inetAddress.getHostAddress();
            } else {
               throw new AssertionError("Invalid IPv6 address: '" + host + "'");
            }
         }
      }
   }

   private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii) {
      for(int i = 0; i < hostnameAscii.length(); ++i) {
         char c = hostnameAscii.charAt(i);
         if (c <= 31 || c >= 127) {
            return true;
         }

         if (" #%/:?@[\\]".indexOf(c) != -1) {
            return true;
         }
      }

      return false;
   }

   public static int indexOfControlOrNonAscii(String input) {
      int i = 0;

      for(int length = input.length(); i < length; ++i) {
         char c = input.charAt(i);
         if (c <= 31 || c >= 127) {
            return i;
         }
      }

      return -1;
   }

   public static boolean verifyAsIpAddress(String host) {
      return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
   }

   public static String format(String format, Object... args) {
      return String.format(Locale.US, format, args);
   }

   public static Charset bomAwareCharset(BufferedSource source, Charset charset) throws IOException {
      switch(source.select(UNICODE_BOMS)) {
      case -1:
         return charset;
      case 0:
         return StandardCharsets.UTF_8;
      case 1:
         return StandardCharsets.UTF_16BE;
      case 2:
         return StandardCharsets.UTF_16LE;
      case 3:
         return UTF_32BE;
      case 4:
         return UTF_32LE;
      default:
         throw new AssertionError();
      }
   }

   public static int checkDuration(String name, long duration, TimeUnit unit) {
      if (duration < 0L) {
         throw new IllegalArgumentException(name + " < 0");
      } else if (unit == null) {
         throw new NullPointerException("unit == null");
      } else {
         long millis = unit.toMillis(duration);
         if (millis > 2147483647L) {
            throw new IllegalArgumentException(name + " too large.");
         } else if (millis == 0L && duration > 0L) {
            throw new IllegalArgumentException(name + " too small.");
         } else {
            return (int)millis;
         }
      }
   }

   public static int decodeHexDigit(char c) {
      if (c >= '0' && c <= '9') {
         return c - 48;
      } else if (c >= 'a' && c <= 'f') {
         return c - 97 + 10;
      } else {
         return c >= 'A' && c <= 'F' ? c - 65 + 10 : -1;
      }
   }

   @Nullable
   private static InetAddress decodeIpv6(String input, int pos, int limit) {
      byte[] address = new byte[16];
      int b = 0;
      int compress = -1;
      int groupOffset = -1;

      int value;
      for(int i = pos; i < limit; address[b++] = (byte)(value & 255)) {
         if (b == address.length) {
            return null;
         }

         if (i + 2 <= limit && input.regionMatches(i, "::", 0, 2)) {
            if (compress != -1) {
               return null;
            }

            i += 2;
            b += 2;
            compress = b;
            if (i == limit) {
               break;
            }
         } else if (b != 0) {
            if (!input.regionMatches(i, ":", 0, 1)) {
               if (!input.regionMatches(i, ".", 0, 1)) {
                  return null;
               }

               if (!decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) {
                  return null;
               }

               b += 2;
               break;
            }

            ++i;
         }

         value = 0;

         for(groupOffset = i; i < limit; ++i) {
            char c = input.charAt(i);
            int hexDigit = decodeHexDigit(c);
            if (hexDigit == -1) {
               break;
            }

            value = (value << 4) + hexDigit;
         }

         int groupLength = i - groupOffset;
         if (groupLength == 0 || groupLength > 4) {
            return null;
         }

         address[b++] = (byte)(value >>> 8 & 255);
      }

      if (b != address.length) {
         if (compress == -1) {
            return null;
         }

         System.arraycopy(address, compress, address, address.length - (b - compress), b - compress);
         Arrays.fill(address, compress, compress + (address.length - b), (byte)0);
      }

      try {
         return InetAddress.getByAddress(address);
      } catch (UnknownHostException var11) {
         throw new AssertionError();
      }
   }

   private static boolean decodeIpv4Suffix(String input, int pos, int limit, byte[] address, int addressOffset) {
      int b = addressOffset;

      int value;
      for(int i = pos; i < limit; address[b++] = (byte)value) {
         if (b == address.length) {
            return false;
         }

         if (b != addressOffset) {
            if (input.charAt(i) != '.') {
               return false;
            }

            ++i;
         }

         value = 0;

         int groupOffset;
         for(groupOffset = i; i < limit; ++i) {
            char c = input.charAt(i);
            if (c < '0' || c > '9') {
               break;
            }

            if (value == 0 && groupOffset != i) {
               return false;
            }

            value = value * 10 + c - 48;
            if (value > 255) {
               return false;
            }
         }

         int groupLength = i - groupOffset;
         if (groupLength == 0) {
            return false;
         }
      }

      if (b != addressOffset + 4) {
         return false;
      } else {
         return true;
      }
   }

   private static String inet6AddressToAscii(byte[] address) {
      int longestRunOffset = -1;
      int longestRunLength = 0;

      int i;
      int group;
      for(int i = 0; i < address.length; i += 2) {
         for(i = i; i < 16 && address[i] == 0 && address[i + 1] == 0; i += 2) {
         }

         group = i - i;
         if (group > longestRunLength && group >= 4) {
            longestRunOffset = i;
            longestRunLength = group;
         }
      }

      Buffer result = new Buffer();
      i = 0;

      while(i < address.length) {
         if (i == longestRunOffset) {
            result.writeByte(58);
            i += longestRunLength;
            if (i == 16) {
               result.writeByte(58);
            }
         } else {
            if (i > 0) {
               result.writeByte(58);
            }

            group = (address[i] & 255) << 8 | address[i + 1] & 255;
            result.writeHexadecimalUnsignedLong((long)group);
            i += 2;
         }
      }

      return result.readUtf8();
   }

   public static X509TrustManager platformTrustManager() {
      try {
         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         trustManagerFactory.init((KeyStore)null);
         TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
         if (trustManagers.length == 1 && trustManagers[0] instanceof X509TrustManager) {
            return (X509TrustManager)trustManagers[0];
         } else {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
         }
      } catch (GeneralSecurityException var2) {
         throw new AssertionError("No System TLS", var2);
      }
   }

   public static Headers toHeaders(List<Header> headerBlock) {
      Headers.Builder builder = new Headers.Builder();
      Iterator var2 = headerBlock.iterator();

      while(var2.hasNext()) {
         Header header = (Header)var2.next();
         Internal.instance.addLenient(builder, header.name.utf8(), header.value.utf8());
      }

      return builder.build();
   }

   public static List<Header> toHeaderBlock(Headers headers) {
      List<Header> result = new ArrayList();

      for(int i = 0; i < headers.size(); ++i) {
         result.add(new Header(headers.name(i), headers.value(i)));
      }

      return result;
   }

   public static String getSystemProperty(String key, @Nullable String defaultValue) {
      String value;
      try {
         value = System.getProperty(key);
      } catch (AccessControlException var4) {
         return defaultValue;
      }

      return value != null ? value : defaultValue;
   }

   static {
      EMPTY_RESPONSE = ResponseBody.create((MediaType)null, (byte[])EMPTY_BYTE_ARRAY);
      EMPTY_REQUEST = RequestBody.create((MediaType)null, (byte[])EMPTY_BYTE_ARRAY);
      UNICODE_BOMS = Options.of(ByteString.decodeHex("efbbbf"), ByteString.decodeHex("feff"), ByteString.decodeHex("fffe"), ByteString.decodeHex("0000ffff"), ByteString.decodeHex("ffff0000"));
      UTF_32BE = Charset.forName("UTF-32BE");
      UTF_32LE = Charset.forName("UTF-32LE");
      UTC = TimeZone.getTimeZone("GMT");
      NATURAL_ORDER = String::compareTo;

      Method m;
      try {
         m = Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
      } catch (Exception var2) {
         m = null;
      }

      addSuppressedExceptionMethod = m;
      VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");
   }
}

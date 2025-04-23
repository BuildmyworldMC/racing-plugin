package net.dv8tion.jda.internal.utils;

import com.neovisionaries.ws.client.WebSocketFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Okio;
import org.slf4j.Logger;

public class IOUtil {
   private static final Logger log = JDALogger.getLog(IOUtil.class);

   public static void silentClose(AutoCloseable closeable) {
      try {
         closeable.close();
      } catch (Exception var2) {
      }

   }

   public static void silentClose(Closeable closeable) {
      try {
         closeable.close();
      } catch (IOException var2) {
      }

   }

   public static String getHost(String uri) {
      return URI.create(uri).getHost();
   }

   public static void setServerName(WebSocketFactory factory, String url) {
      String host = getHost(url);
      if (host != null) {
         factory.setServerName(host);
      }

   }

   public static OkHttpClient.Builder newHttpClientBuilder() {
      Dispatcher dispatcher = new Dispatcher();
      dispatcher.setMaxRequestsPerHost(25);
      ConnectionPool connectionPool = new ConnectionPool(5, 10L, TimeUnit.SECONDS);
      return (new OkHttpClient.Builder()).connectionPool(connectionPool).dispatcher(dispatcher);
   }

   public static byte[] readFully(File file) throws IOException {
      Checks.notNull(file, "File");
      Checks.check(file.exists(), "Provided file does not exist!");
      FileInputStream is = new FileInputStream(file);

      byte[] var7;
      try {
         long length = file.length();
         if (length > 2147483647L) {
            throw new IOException("Cannot read the file into memory completely due to it being too large!");
         }

         byte[] bytes = new byte[(int)length];
         int offset = 0;

         int numRead;
         for(boolean var6 = false; offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0; offset += numRead) {
         }

         if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
         }

         is.close();
         var7 = bytes;
      } catch (Throwable var9) {
         try {
            is.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      is.close();
      return var7;
   }

   public static byte[] readFully(InputStream stream) throws IOException {
      Checks.notNull(stream, "InputStream");
      byte[] buffer = new byte[1024];
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      byte[] var4;
      try {
         boolean var3 = false;

         while(true) {
            int readAmount;
            if ((readAmount = stream.read(buffer)) == -1) {
               var4 = bos.toByteArray();
               break;
            }

            bos.write(buffer, 0, readAmount);
         }
      } catch (Throwable var6) {
         try {
            bos.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      bos.close();
      return var4;
   }

   public static RequestBody createRequestBody(MediaType contentType, InputStream stream) {
      return new BufferedRequestBody(Okio.source(stream), contentType);
   }

   public static short getShortBigEndian(byte[] arr, int offset) {
      return (short)((arr[offset] & 255) << 8 | arr[offset + 1] & 255);
   }

   public static short getShortLittleEndian(byte[] arr, int offset) {
      return (short)(arr[offset] & 255 | (arr[offset + 1] & 255) << 8);
   }

   public static int getIntBigEndian(byte[] arr, int offset) {
      return arr[offset + 3] & 255 | (arr[offset + 2] & 255) << 8 | (arr[offset + 1] & 255) << 16 | (arr[offset] & 255) << 24;
   }

   public static void setIntBigEndian(byte[] arr, int offset, int it) {
      arr[offset] = (byte)(it >>> 24 & 255);
      arr[offset + 1] = (byte)(it >>> 16 & 255);
      arr[offset + 2] = (byte)(it >>> 8 & 255);
      arr[offset + 3] = (byte)(it & 255);
   }

   public static ByteBuffer reallocate(ByteBuffer original, int length) {
      ByteBuffer buffer = ByteBuffer.allocate(length);
      buffer.put(original);
      return buffer;
   }

   public static InputStream getBody(Response response) throws IOException {
      String encoding = response.header("content-encoding", "");
      InputStream data = new BufferedInputStream(response.body().byteStream());
      data.mark(256);

      try {
         if (encoding.equalsIgnoreCase("gzip")) {
            return new GZIPInputStream(data);
         } else {
            return (InputStream)(encoding.equalsIgnoreCase("deflate") ? new InflaterInputStream(data, new Inflater(true)) : data);
         }
      } catch (EOFException | ZipException var4) {
         data.reset();
         log.error("Failed to read gzip content for response. Headers: {}\nContent: '{}'", response.headers(), JDALogger.getLazyString(() -> {
            return new String(readFully(data));
         }), var4);
         return null;
      }
   }
}

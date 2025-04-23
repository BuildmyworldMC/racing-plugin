package okhttp3.internal.http2;

import java.io.IOException;
import okhttp3.internal.Util;
import okio.ByteString;

public final class Http2 {
   static final ByteString CONNECTION_PREFACE = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
   static final int INITIAL_MAX_FRAME_SIZE = 16384;
   static final byte TYPE_DATA = 0;
   static final byte TYPE_HEADERS = 1;
   static final byte TYPE_PRIORITY = 2;
   static final byte TYPE_RST_STREAM = 3;
   static final byte TYPE_SETTINGS = 4;
   static final byte TYPE_PUSH_PROMISE = 5;
   static final byte TYPE_PING = 6;
   static final byte TYPE_GOAWAY = 7;
   static final byte TYPE_WINDOW_UPDATE = 8;
   static final byte TYPE_CONTINUATION = 9;
   static final byte FLAG_NONE = 0;
   static final byte FLAG_ACK = 1;
   static final byte FLAG_END_STREAM = 1;
   static final byte FLAG_END_HEADERS = 4;
   static final byte FLAG_END_PUSH_PROMISE = 4;
   static final byte FLAG_PADDED = 8;
   static final byte FLAG_PRIORITY = 32;
   static final byte FLAG_COMPRESSED = 32;
   private static final String[] FRAME_NAMES = new String[]{"DATA", "HEADERS", "PRIORITY", "RST_STREAM", "SETTINGS", "PUSH_PROMISE", "PING", "GOAWAY", "WINDOW_UPDATE", "CONTINUATION"};
   static final String[] FLAGS = new String[64];
   static final String[] BINARY = new String[256];

   private Http2() {
   }

   static IllegalArgumentException illegalArgument(String message, Object... args) {
      throw new IllegalArgumentException(Util.format(message, args));
   }

   static IOException ioException(String message, Object... args) throws IOException {
      throw new IOException(Util.format(message, args));
   }

   static String frameLog(boolean inbound, int streamId, int length, byte type, byte flags) {
      String formattedType = type < FRAME_NAMES.length ? FRAME_NAMES[type] : Util.format("0x%02x", type);
      String formattedFlags = formatFlags(type, flags);
      return Util.format("%s 0x%08x %5d %-13s %s", inbound ? "<<" : ">>", streamId, length, formattedType, formattedFlags);
   }

   static String formatFlags(byte type, byte flags) {
      if (flags == 0) {
         return "";
      } else {
         switch(type) {
         case 2:
         case 3:
         case 7:
         case 8:
            return BINARY[flags];
         case 4:
         case 6:
            return flags == 1 ? "ACK" : BINARY[flags];
         case 5:
         default:
            String result = flags < FLAGS.length ? FLAGS[flags] : BINARY[flags];
            if (type == 5 && (flags & 4) != 0) {
               return result.replace("HEADERS", "PUSH_PROMISE");
            } else {
               return type == 0 && (flags & 32) != 0 ? result.replace("PRIORITY", "COMPRESSED") : result;
            }
         }
      }
   }

   static {
      for(int i = 0; i < BINARY.length; ++i) {
         BINARY[i] = Util.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
      }

      FLAGS[0] = "";
      FLAGS[1] = "END_STREAM";
      int[] prefixFlags = new int[]{1};
      FLAGS[8] = "PADDED";
      int[] frameFlags = prefixFlags;
      int i = prefixFlags.length;

      int var3;
      int prefixFlag;
      for(var3 = 0; var3 < i; ++var3) {
         prefixFlag = frameFlags[var3];
         FLAGS[prefixFlag | 8] = FLAGS[prefixFlag] + "|PADDED";
      }

      FLAGS[4] = "END_HEADERS";
      FLAGS[32] = "PRIORITY";
      FLAGS[36] = "END_HEADERS|PRIORITY";
      frameFlags = new int[]{4, 32, 36};
      int[] var11 = frameFlags;
      var3 = frameFlags.length;

      for(prefixFlag = 0; prefixFlag < var3; ++prefixFlag) {
         int frameFlag = var11[prefixFlag];
         int[] var6 = prefixFlags;
         int var7 = prefixFlags.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            int prefixFlag = var6[var8];
            FLAGS[prefixFlag | frameFlag] = FLAGS[prefixFlag] + '|' + FLAGS[frameFlag];
            FLAGS[prefixFlag | frameFlag | 8] = FLAGS[prefixFlag] + '|' + FLAGS[frameFlag] + "|PADDED";
         }
      }

      for(i = 0; i < FLAGS.length; ++i) {
         if (FLAGS[i] == null) {
            FLAGS[i] = BINARY[i];
         }
      }

   }
}

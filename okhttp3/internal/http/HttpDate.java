package okhttp3.internal.http;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import okhttp3.internal.Util;

public final class HttpDate {
   public static final long MAX_DATE = 253402300799999L;
   private static final ThreadLocal<DateFormat> STANDARD_DATE_FORMAT = new ThreadLocal<DateFormat>() {
      protected DateFormat initialValue() {
         DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
         rfc1123.setLenient(false);
         rfc1123.setTimeZone(Util.UTC);
         return rfc1123;
      }
   };
   private static final String[] BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS = new String[]{"EEE, dd MMM yyyy HH:mm:ss zzz", "EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy", "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MMM-yyyy HH-mm-ss z", "EEE, dd MMM yy HH:mm:ss z", "EEE dd-MMM-yyyy HH:mm:ss z", "EEE dd MMM yyyy HH:mm:ss z", "EEE dd-MMM-yyyy HH-mm-ss z", "EEE dd-MMM-yy HH:mm:ss z", "EEE dd MMM yy HH:mm:ss z", "EEE,dd-MMM-yy HH:mm:ss z", "EEE,dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MM-yyyy HH:mm:ss z", "EEE MMM d yyyy HH:mm:ss z"};
   private static final DateFormat[] BROWSER_COMPATIBLE_DATE_FORMATS;

   public static Date parse(String value) {
      if (value.length() == 0) {
         return null;
      } else {
         ParsePosition position = new ParsePosition(0);
         Date result = ((DateFormat)STANDARD_DATE_FORMAT.get()).parse(value, position);
         if (position.getIndex() == value.length()) {
            return result;
         } else {
            synchronized(BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS) {
               int i = 0;

               for(int count = BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length; i < count; ++i) {
                  DateFormat format = BROWSER_COMPATIBLE_DATE_FORMATS[i];
                  if (format == null) {
                     format = new SimpleDateFormat(BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS[i], Locale.US);
                     ((DateFormat)format).setTimeZone(Util.UTC);
                     BROWSER_COMPATIBLE_DATE_FORMATS[i] = (DateFormat)format;
                  }

                  position.setIndex(0);
                  result = ((DateFormat)format).parse(value, position);
                  if (position.getIndex() != 0) {
                     return result;
                  }
               }

               return null;
            }
         }
      }
   }

   public static String format(Date value) {
      return ((DateFormat)STANDARD_DATE_FORMAT.get()).format(value);
   }

   private HttpDate() {
   }

   static {
      BROWSER_COMPATIBLE_DATE_FORMATS = new DateFormat[BROWSER_COMPATIBLE_DATE_FORMAT_STRINGS.length];
   }
}

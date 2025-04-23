package com.github.hornta.racing.commando;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DateUtil {
   private static final int maxYears = 1000;
   private static final Pattern TIME_PATTERN = Pattern.compile((String)Stream.of("y", "mo", "w", "d", "h", "m", "s").map((i) -> {
      return "(?:([0-9]+)" + i + "[a-z]*[,]?)?";
   }).collect(Collectors.joining()), 2);
   private static final int MAX_YEARS = 100000;

   private DateUtil() {
   }

   public static long parseDuration(String time, boolean future) {
      return parseDuration(new GregorianCalendar(), time, future);
   }

   public static long parseDuration(Calendar from, String time, boolean future) throws IllegalArgumentException {
      Matcher matcher = TIME_PATTERN.matcher(time);
      int years = 0;
      int months = 0;
      int weeks = 0;
      int days = 0;
      int hours = 0;
      int minutes = 0;
      int seconds = 0;
      boolean found = false;

      while(matcher.find()) {
         if (matcher.group() != null && !matcher.group().isEmpty()) {
            for(int i = 0; i < matcher.groupCount(); ++i) {
               if (matcher.group(i) != null && !matcher.group(i).isEmpty()) {
                  found = true;
                  break;
               }
            }

            if (found) {
               if (matcher.group(1) != null && !matcher.group(1).isEmpty()) {
                  years = Integer.parseInt(matcher.group(1));
               }

               if (matcher.group(2) != null && !matcher.group(2).isEmpty()) {
                  months = Integer.parseInt(matcher.group(2));
               }

               if (matcher.group(3) != null && !matcher.group(3).isEmpty()) {
                  weeks = Integer.parseInt(matcher.group(3));
               }

               if (matcher.group(4) != null && !matcher.group(4).isEmpty()) {
                  days = Integer.parseInt(matcher.group(4));
               }

               if (matcher.group(5) != null && !matcher.group(5).isEmpty()) {
                  hours = Integer.parseInt(matcher.group(5));
               }

               if (matcher.group(6) != null && !matcher.group(6).isEmpty()) {
                  minutes = Integer.parseInt(matcher.group(6));
               }

               if (matcher.group(7) != null && !matcher.group(7).isEmpty()) {
                  seconds = Integer.parseInt(matcher.group(7));
               }
               break;
            }
         }
      }

      if (!found) {
         throw new IllegalArgumentException();
      } else {
         Calendar c = (Calendar)from.clone();
         if (years > 0) {
            if (years > 100000) {
               years = 100000;
            }

            c.add(1, years * (future ? 1 : -1));
         }

         if (months > 0) {
            c.add(2, months * (future ? 1 : -1));
         }

         if (weeks > 0) {
            c.add(3, weeks * (future ? 1 : -1));
         }

         if (days > 0) {
            c.add(5, days * (future ? 1 : -1));
         }

         if (hours > 0) {
            c.add(11, hours * (future ? 1 : -1));
         }

         if (minutes > 0) {
            c.add(12, minutes * (future ? 1 : -1));
         }

         if (seconds > 0) {
            c.add(13, seconds * (future ? 1 : -1));
         }

         Calendar max = (Calendar)from.clone();
         max.add(1, 1000);
         if (c.after(max)) {
            c = max;
         }

         return c.getTimeInMillis() / 1000L;
      }
   }

   static int dateDiff(int type, Calendar fromDate, Calendar toDate, boolean future) {
      int year = 1;
      int fromYear = fromDate.get(year);
      int toYear = toDate.get(year);
      if (Math.abs(fromYear - toYear) > 1000) {
         toDate.set(year, fromYear + (future ? 1000 : -1000));
      }

      int diff = 0;

      long savedDate;
      for(savedDate = fromDate.getTimeInMillis(); future && !fromDate.after(toDate) || !future && !fromDate.before(toDate); ++diff) {
         savedDate = fromDate.getTimeInMillis();
         fromDate.add(type, future ? 1 : -1);
      }

      --diff;
      fromDate.setTimeInMillis(savedDate);
      return diff;
   }

   public static String formatDateDiff(long ts) {
      Calendar c = new GregorianCalendar();
      c.setTimeInMillis(ts);
      Calendar now = new GregorianCalendar();
      return formatDateDiff(now, c);
   }

   public static String formatDateDiff(Calendar fromDate, Calendar toDate) {
      boolean future = false;
      if (toDate.equals(fromDate)) {
         return "<now>";
      } else {
         if (toDate.after(fromDate)) {
            future = true;
         }

         StringBuilder sb = new StringBuilder();
         int[] types = new int[]{1, 2, 5, 11, 12, 13};
         String[] names = new String[]{"<year>", "<years>", "<month>", "<months>", "<day>", "<days>", "<hour>", "<hours>", "<minute>", "<minutes>", "<second>", "<seconds>"};
         int accuracy = 0;

         for(int i = 0; i < types.length && accuracy != 2; ++i) {
            int diff = dateDiff(types[i], fromDate, toDate, future);
            if (diff > 0) {
               ++accuracy;
               sb.append(" ").append(diff).append(" ").append(names[i * 2 + (diff > 1 ? 1 : 0)]);
            }
         }

         return sb.length() == 0 ? "<now>" : sb.toString().trim();
      }
   }
}

package net.dv8tion.jda.internal.utils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;

public final class Helpers {
   public static boolean isEmpty(CharSequence seq) {
      return seq == null || seq.length() == 0;
   }

   public static boolean containsWhitespace(CharSequence seq) {
      if (isEmpty(seq)) {
         return false;
      } else {
         for(int i = 0; i < seq.length(); ++i) {
            if (Character.isWhitespace(seq.charAt(i))) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isBlank(CharSequence seq) {
      if (isEmpty(seq)) {
         return true;
      } else {
         for(int i = 0; i < seq.length(); ++i) {
            if (!Character.isWhitespace(seq.charAt(i))) {
               return false;
            }
         }

         return true;
      }
   }

   public static int countMatches(CharSequence seq, char c) {
      if (isEmpty(seq)) {
         return 0;
      } else {
         int count = 0;

         for(int i = 0; i < seq.length(); ++i) {
            if (seq.charAt(i) == c) {
               ++count;
            }
         }

         return count;
      }
   }

   public static String truncate(String input, int maxWidth) {
      if (input == null) {
         return null;
      } else {
         Checks.notNegative(maxWidth, "maxWidth");
         if (input.length() <= maxWidth) {
            return input;
         } else {
            return maxWidth == 0 ? "" : input.substring(0, maxWidth);
         }
      }
   }

   public static String rightPad(String input, int size) {
      int pads = size - input.length();
      if (pads <= 0) {
         return input;
      } else {
         StringBuilder out = new StringBuilder(input);

         for(int i = pads; i > 0; --i) {
            out.append(' ');
         }

         return out.toString();
      }
   }

   public static String leftPad(String input, int size) {
      int pads = size - input.length();
      if (pads <= 0) {
         return input;
      } else {
         StringBuilder out = new StringBuilder();

         for(int i = pads; i > 0; --i) {
            out.append(' ');
         }

         return out.append(input).toString();
      }
   }

   public static boolean isNumeric(String input) {
      if (isEmpty(input)) {
         return false;
      } else {
         char[] var1 = input.toCharArray();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            char c = var1[var3];
            if (!Character.isDigit(c)) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean deepEquals(Collection<?> first, Collection<?> second) {
      if (first == second) {
         return true;
      } else if (first != null && second != null && first.size() == second.size()) {
         Iterator<?> itFirst = first.iterator();
         Iterator itSecond = second.iterator();

         Object elementFirst;
         Object elementSecond;
         do {
            if (!itFirst.hasNext()) {
               return true;
            }

            elementFirst = itFirst.next();
            elementSecond = itSecond.next();
         } while(Objects.equals(elementFirst, elementSecond));

         return false;
      } else {
         return false;
      }
   }

   public static boolean deepEqualsUnordered(Collection<?> first, Collection<?> second) {
      if (first == second) {
         return true;
      } else if (first != null && second != null) {
         return first.size() == second.size() && second.containsAll(first);
      } else {
         return false;
      }
   }

   public static <E extends Enum<E>> EnumSet<E> copyEnumSet(Class<E> clazz, Collection<E> col) {
      return col.isEmpty() ? EnumSet.noneOf(clazz) : EnumSet.copyOf(col);
   }

   public static <T extends Throwable> T appendCause(T throwable, Throwable cause) {
      Throwable t;
      for(t = throwable; t.getCause() != null; t = t.getCause()) {
      }

      t.initCause(cause);
      return throwable;
   }

   public static boolean hasCause(Throwable throwable, Class<? extends Throwable> cause) {
      for(Throwable cursor = throwable; cursor != null; cursor = cursor.getCause()) {
         if (cause.isInstance(cursor)) {
            return true;
         }
      }

      return false;
   }
}

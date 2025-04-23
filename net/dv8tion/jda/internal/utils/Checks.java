package net.dv8tion.jda.internal.utils;

import java.util.Collection;
import org.jetbrains.annotations.Contract;

public class Checks {
   @Contract("null -> fail")
   public static void isSnowflake(String snowflake) {
      isSnowflake(snowflake, snowflake);
   }

   @Contract("null, _ -> fail")
   public static void isSnowflake(String snowflake, String message) {
      notNull(snowflake, message);
      if (snowflake.length() > 20 || !Helpers.isNumeric(snowflake)) {
         throw new IllegalArgumentException(message + " is not a valid snowflake value!");
      }
   }

   @Contract("false, _ -> fail")
   public static void check(boolean expression, String message) {
      if (!expression) {
         throw new IllegalArgumentException(message);
      }
   }

   @Contract("false, _, _ -> fail")
   public static void check(boolean expression, String message, Object... args) {
      if (!expression) {
         throw new IllegalArgumentException(String.format(message, args));
      }
   }

   @Contract("false, _, _ -> fail")
   public static void check(boolean expression, String message, Object arg) {
      if (!expression) {
         throw new IllegalArgumentException(String.format(message, arg));
      }
   }

   @Contract("null, _ -> fail")
   public static void notNull(Object argument, String name) {
      if (argument == null) {
         throw new IllegalArgumentException(name + " may not be null");
      }
   }

   @Contract("null, _ -> fail")
   public static void notEmpty(CharSequence argument, String name) {
      notNull(argument, name);
      if (Helpers.isEmpty(argument)) {
         throw new IllegalArgumentException(name + " may not be empty");
      }
   }

   @Contract("null, _ -> fail")
   public static void notBlank(CharSequence argument, String name) {
      notNull(argument, name);
      if (Helpers.isBlank(argument)) {
         throw new IllegalArgumentException(name + " may not be blank");
      }
   }

   @Contract("null, _ -> fail")
   public static void noWhitespace(CharSequence argument, String name) {
      notNull(argument, name);
      if (Helpers.containsWhitespace(argument)) {
         throw new IllegalArgumentException(name + " may not contain blanks");
      }
   }

   @Contract("null, _ -> fail")
   public static void notEmpty(Collection<?> argument, String name) {
      notNull(argument, name);
      if (argument.isEmpty()) {
         throw new IllegalArgumentException(name + " may not be empty");
      }
   }

   @Contract("null, _ -> fail")
   public static void notEmpty(Object[] argument, String name) {
      notNull(argument, name);
      if (argument.length == 0) {
         throw new IllegalArgumentException(name + " may not be empty");
      }
   }

   @Contract("null, _ -> fail")
   public static void noneNull(Collection<?> argument, String name) {
      notNull(argument, name);
      argument.forEach((it) -> {
         notNull(it, name);
      });
   }

   @Contract("null, _ -> fail")
   public static void noneNull(Object[] argument, String name) {
      notNull(argument, name);
      Object[] var2 = argument;
      int var3 = argument.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Object it = var2[var4];
         notNull(it, name);
      }

   }

   @Contract("null, _ -> fail")
   public static <T extends CharSequence> void noneEmpty(Collection<T> argument, String name) {
      notNull(argument, name);
      argument.forEach((it) -> {
         notEmpty(it, name);
      });
   }

   @Contract("null, _ -> fail")
   public static <T extends CharSequence> void noneBlank(Collection<T> argument, String name) {
      notNull(argument, name);
      argument.forEach((it) -> {
         notBlank(it, name);
      });
   }

   @Contract("null, _ -> fail")
   public static <T extends CharSequence> void noneContainBlanks(Collection<T> argument, String name) {
      notNull(argument, name);
      argument.forEach((it) -> {
         noWhitespace(it, name);
      });
   }

   public static void positive(int n, String name) {
      if (n <= 0) {
         throw new IllegalArgumentException(name + " may not be negative or zero");
      }
   }

   public static void positive(long n, String name) {
      if (n <= 0L) {
         throw new IllegalArgumentException(name + " may not be negative or zero");
      }
   }

   public static void notNegative(int n, String name) {
      if (n < 0) {
         throw new IllegalArgumentException(name + " may not be negative");
      }
   }

   public static void notNegative(long n, String name) {
      if (n < 0L) {
         throw new IllegalArgumentException(name + " may not be negative");
      }
   }
}

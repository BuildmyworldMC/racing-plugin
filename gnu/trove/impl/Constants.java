package gnu.trove.impl;

public class Constants {
   private static final boolean VERBOSE = System.getProperty("gnu.trove.verbose", (String)null) != null;
   public static final int DEFAULT_CAPACITY = 10;
   public static final float DEFAULT_LOAD_FACTOR = 0.5F;
   public static final byte DEFAULT_BYTE_NO_ENTRY_VALUE;
   public static final short DEFAULT_SHORT_NO_ENTRY_VALUE;
   public static final char DEFAULT_CHAR_NO_ENTRY_VALUE;
   public static final int DEFAULT_INT_NO_ENTRY_VALUE;
   public static final long DEFAULT_LONG_NO_ENTRY_VALUE;
   public static final float DEFAULT_FLOAT_NO_ENTRY_VALUE;
   public static final double DEFAULT_DOUBLE_NO_ENTRY_VALUE;

   static {
      String property = System.getProperty("gnu.trove.no_entry.byte", "0");
      byte value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = 127;
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = -128;
      } else {
         value = Byte.valueOf(property);
      }

      if (value > 127) {
         value = 127;
      } else if (value < -128) {
         value = -128;
      }

      DEFAULT_BYTE_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_BYTE_NO_ENTRY_VALUE: " + DEFAULT_BYTE_NO_ENTRY_VALUE);
      }

      property = System.getProperty("gnu.trove.no_entry.short", "0");
      short value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = 32767;
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = -32768;
      } else {
         value = Short.valueOf(property);
      }

      if (value > 32767) {
         value = 32767;
      } else if (value < -32768) {
         value = -32768;
      }

      DEFAULT_SHORT_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_SHORT_NO_ENTRY_VALUE: " + DEFAULT_SHORT_NO_ENTRY_VALUE);
      }

      property = System.getProperty("gnu.trove.no_entry.char", "\u0000");
      char value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = '\uffff';
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = 0;
      } else {
         value = property.toCharArray()[0];
      }

      if (value > '\uffff') {
         value = '\uffff';
      } else if (value < 0) {
         value = 0;
      }

      DEFAULT_CHAR_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_CHAR_NO_ENTRY_VALUE: " + Integer.valueOf(value));
      }

      property = System.getProperty("gnu.trove.no_entry.int", "0");
      int value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = Integer.MAX_VALUE;
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = Integer.MIN_VALUE;
      } else {
         value = Integer.valueOf(property);
      }

      DEFAULT_INT_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_INT_NO_ENTRY_VALUE: " + DEFAULT_INT_NO_ENTRY_VALUE);
      }

      String property = System.getProperty("gnu.trove.no_entry.long", "0");
      long value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = Long.MAX_VALUE;
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = Long.MIN_VALUE;
      } else {
         value = Long.valueOf(property);
      }

      DEFAULT_LONG_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_LONG_NO_ENTRY_VALUE: " + DEFAULT_LONG_NO_ENTRY_VALUE);
      }

      property = System.getProperty("gnu.trove.no_entry.float", "0");
      float value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = Float.MAX_VALUE;
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = Float.MIN_VALUE;
      } else if ("MIN_NORMAL".equalsIgnoreCase(property)) {
         value = 1.17549435E-38F;
      } else if ("NEGATIVE_INFINITY".equalsIgnoreCase(property)) {
         value = Float.NEGATIVE_INFINITY;
      } else if ("POSITIVE_INFINITY".equalsIgnoreCase(property)) {
         value = Float.POSITIVE_INFINITY;
      } else {
         value = Float.valueOf(property);
      }

      DEFAULT_FLOAT_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_FLOAT_NO_ENTRY_VALUE: " + DEFAULT_FLOAT_NO_ENTRY_VALUE);
      }

      property = System.getProperty("gnu.trove.no_entry.double", "0");
      double value;
      if ("MAX_VALUE".equalsIgnoreCase(property)) {
         value = Double.MAX_VALUE;
      } else if ("MIN_VALUE".equalsIgnoreCase(property)) {
         value = Double.MIN_VALUE;
      } else if ("MIN_NORMAL".equalsIgnoreCase(property)) {
         value = 2.2250738585072014E-308D;
      } else if ("NEGATIVE_INFINITY".equalsIgnoreCase(property)) {
         value = Double.NEGATIVE_INFINITY;
      } else if ("POSITIVE_INFINITY".equalsIgnoreCase(property)) {
         value = Double.POSITIVE_INFINITY;
      } else {
         value = Double.valueOf(property);
      }

      DEFAULT_DOUBLE_NO_ENTRY_VALUE = value;
      if (VERBOSE) {
         System.out.println("DEFAULT_DOUBLE_NO_ENTRY_VALUE: " + DEFAULT_DOUBLE_NO_ENTRY_VALUE);
      }

   }
}

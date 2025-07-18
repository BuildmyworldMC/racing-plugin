package com.github.hornta.racing.messenger;

public enum PlaceholderOption {
   DELIMITER;

   public static PlaceholderOption fromString(String string) {
      PlaceholderOption[] var1 = values();

       for (PlaceholderOption value : var1) {
           if (value.name().equalsIgnoreCase(string)) {
               return value;
           }
       }

      return null;
   }
}

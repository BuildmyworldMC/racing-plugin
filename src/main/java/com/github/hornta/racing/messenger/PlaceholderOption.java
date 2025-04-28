package com.github.hornta.racing.messenger;

public enum PlaceholderOption {
   DELIMITER;

   public static PlaceholderOption fromString(String string) {
      PlaceholderOption[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         PlaceholderOption value = var1[var3];
         if (value.name().equalsIgnoreCase(string)) {
            return value;
         }
      }

      return null;
   }
}

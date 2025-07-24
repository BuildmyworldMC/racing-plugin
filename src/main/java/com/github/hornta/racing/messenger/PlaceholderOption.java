package com.github.hornta.racing.messenger;

public enum PlaceholderOption {
   DELIMITER;

   public static PlaceholderOption fromString(String string) {
      PlaceholderOption[] placeholderOptions = values();

       for (PlaceholderOption placeholderOption : placeholderOptions) {
           if (placeholderOption.name().equalsIgnoreCase(string)) {
               return placeholderOption;
           }
       }

      return null;
   }
}

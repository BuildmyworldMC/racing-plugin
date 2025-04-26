package com.github.hornta.racing.enums;

public enum RaceState {
   ENABLED,
   DISABLED,
   UNDER_CONSTRUCTION;

   public static RaceState fromString(String string) {
      RaceState[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         RaceState state = var1[var3];
         if (state.name().equalsIgnoreCase(string)) {
            return state;
         }
      }

      return null;
   }

   // $FF: synthetic method
   private static RaceState[] $values() {
      return new RaceState[]{ENABLED, DISABLED, UNDER_CONSTRUCTION};
   }
}

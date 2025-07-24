package com.github.hornta.racing.enums;

public enum RaceState {
   ENABLED,
   DISABLED,
   UNDER_CONSTRUCTION;

   public static RaceState fromString(String string) {
      RaceState[] raceStates = values();

       for (RaceState state : raceStates) {
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

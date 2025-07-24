package com.github.hornta.racing.enums;

public enum RaceSignType {
   JOIN,
   FASTEST,
   FASTEST_LAP,
   WINS,
   WIN_RATIO,
   RUNS,
   INFO;

   public static RaceSignType fromString(String string) {
      RaceSignType[] raceSignTypes = values();

       for (RaceSignType type : raceSignTypes) {
           if (type.name().compareToIgnoreCase(string) == 0) {
               return type;
           }
       }

      return null;
   }

   // $FF: synthetic method
   private static RaceSignType[] $values() {
      return new RaceSignType[]{JOIN, FASTEST, FASTEST_LAP, WINS, WIN_RATIO, RUNS, INFO};
   }
}

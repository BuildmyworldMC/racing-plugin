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
      RaceSignType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         RaceSignType type = var1[var3];
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

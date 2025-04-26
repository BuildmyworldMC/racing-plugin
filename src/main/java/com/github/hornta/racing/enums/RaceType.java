package com.github.hornta.racing.enums;

public enum RaceType {
   PLAYER,
   HORSE,
   PIG,
   ELYTRA,
   BOAT,
   STRIDER,
   MINECART;

   public static RaceType fromString(String string) {
      RaceType[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         RaceType type = var1[var3];
         if (type.name().compareToIgnoreCase(string) == 0) {
            return type;
         }
      }

      return null;
   }

   // $FF: synthetic method
   private static RaceType[] $values() {
      return new RaceType[]{PLAYER, HORSE, PIG, ELYTRA, BOAT, STRIDER, MINECART};
   }
}

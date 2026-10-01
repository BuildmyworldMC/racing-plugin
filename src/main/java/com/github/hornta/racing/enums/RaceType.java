package com.github.hornta.racing.enums;

public enum RaceType {
   PLAYER,
   HORSE,
   PIG,
   ELYTRA,
   BOAT,
   STRIDER,
   MINECART,
   HAPPY_GHAST,
   CAMEL;

   public static RaceType fromString(String string) {
      RaceType[] raceTypes = values();

       for (RaceType type : raceTypes) {
           if (type.name().compareToIgnoreCase(string) == 0) {
               return type;
           }
       }

      return null;
   }

   // $FF: synthetic method
   private static RaceType[] $values() {
      return new RaceType[]{
          PLAYER,
          HORSE,
          PIG,
          ELYTRA,
          BOAT,
          STRIDER,
          MINECART,
          HAPPY_GHAST,
          CAMEL
      };
   }
}

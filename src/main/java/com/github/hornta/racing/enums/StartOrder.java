package com.github.hornta.racing.enums;

public enum StartOrder {
   FASTEST,
   FASTEST_LAP,
   WINS,
   SLOWEST,
   SLOWEST_LAP,
   RANDOM,
   JOIN_ORDER,
   REVERSE_JOIN_ORDER;

   public static StartOrder fromString(String string) {
      StartOrder[] startOrders = values();

       for (StartOrder type : startOrders) {
           if (type.name().compareToIgnoreCase(string) == 0) {
               return type;
           }
       }

      return null;
   }

   // $FF: synthetic method
   private static StartOrder[] $values() {
      return new StartOrder[]{FASTEST, FASTEST_LAP, WINS, SLOWEST, SLOWEST_LAP, RANDOM, JOIN_ORDER, REVERSE_JOIN_ORDER};
   }
}

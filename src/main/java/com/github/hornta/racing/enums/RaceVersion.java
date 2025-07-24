package com.github.hornta.racing.enums;

import java.util.EnumSet;
import java.util.Iterator;

public enum RaceVersion {
   V1,
   V2,
   V3,
   V4,
   V5,
   V6,
   V7,
   V8,
   V9,
   V10,
   V11,
   V12,
   V13,
   V14,
   V15,
   V16;

   private static final RaceVersion[] copyOfValues = values();
   private int order;

   public static RaceVersion fromString(String name) {
       for (RaceVersion value : copyOfValues) {
           if (value.name().equals(name)) {
               return value;
           }
       }

      throw new IllegalArgumentException("Couldn't translate " + name + " into a RaceVersion");
   }

   public static RaceVersion getLast() {
      return copyOfValues[copyOfValues.length - 1];
   }

   public void setOrder(int order) {
      this.order = order;
   }

   public int getOrder() {
      return this.order;
   }

   public boolean isGreater(RaceVersion v) {
      return this.order > v.order;
   }

   // $FF: synthetic method
   private static RaceVersion[] $values() {
      return new RaceVersion[]{V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15, V16};
   }

   static {
      int counter = 0;

      for (Iterator<RaceVersion> raceVersionIterator = EnumSet.allOf(RaceVersion.class).iterator(); raceVersionIterator.hasNext(); ++counter) {
         RaceVersion v = raceVersionIterator.next();
         v.setOrder(counter);
      }

   }
}

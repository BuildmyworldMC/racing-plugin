package com.github.hornta.racing.hd_top_list;

import java.util.EnumSet;
import java.util.Iterator;

public enum HDTopListVersion {
   V1;

   private static final HDTopListVersion[] copyOfValues = values();
   private int order;

   public static HDTopListVersion fromString(String name) {
      HDTopListVersion[] var1 = copyOfValues;
      int var2 = var1.length;

       for (HDTopListVersion value : var1) {
           if (value.name().equals(name)) {
               return value;
           }
       }

      throw new IllegalArgumentException("Couldn't translate " + name + " into a HDTopListVersion");
   }

   public static HDTopListVersion getLast() {
      return copyOfValues[copyOfValues.length - 1];
   }

   public void setOrder(int order) {
      this.order = order;
   }

   public int getOrder() {
      return this.order;
   }

   public boolean isGreater(HDTopListVersion v) {
      return this.order > v.order;
   }

   // $FF: synthetic method
   private static HDTopListVersion[] $values() {
      return new HDTopListVersion[]{V1};
   }

   static {
      int counter = 0;

      for(Iterator<HDTopListVersion> var1 = EnumSet.allOf(HDTopListVersion.class).iterator(); var1.hasNext(); ++counter) {
         HDTopListVersion v = (HDTopListVersion)var1.next();
         v.setOrder(counter);
      }

   }
}

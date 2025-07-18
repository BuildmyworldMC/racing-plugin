package com.github.hornta.racing.objects;

public class DurationUnit {
   private final long amount;
   private final String singulars;
   private final String plurals;

   public DurationUnit(long amount, String singulars, String plurals) {
      this.amount = amount;
      this.singulars = singulars;
      this.plurals = plurals;
   }

   public long getAmount() {
      return this.amount;
   }

   public String getNumerous() {
      return this.amount == 1L ? this.singulars : this.plurals;
   }
}

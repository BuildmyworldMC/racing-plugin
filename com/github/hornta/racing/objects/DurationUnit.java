package com.github.hornta.racing.objects;

public class DurationUnit {
   private final long amount;
   private final String singularis;
   private final String pluralis;

   public DurationUnit(long amount, String singularis, String pluralis) {
      this.amount = amount;
      this.singularis = singularis;
      this.pluralis = pluralis;
   }

   public long getAmount() {
      return this.amount;
   }

   public String getNumerus() {
      return this.amount == 1L ? this.singularis : this.pluralis;
   }
}

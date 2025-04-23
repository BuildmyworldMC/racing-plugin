package com.github.hornta.racing.objects;

import org.bukkit.potion.PotionEffectType;

public class RacePotionEffect {
   private final PotionEffectType type;
   private final int amplifier;

   public RacePotionEffect(PotionEffectType type, int amplifier) {
      this.type = type;
      this.amplifier = amplifier;
   }

   public PotionEffectType getType() {
      return this.type;
   }

   public int getAmplifier() {
      return this.amplifier;
   }
}

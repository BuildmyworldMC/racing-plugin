package com.github.hornta.racing.objects;

import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;

public class HorseData {
   private final Color color;
   private final Style style;
   private final int age;

   public HorseData(Horse horse) {
      this.color = horse.getColor();
      this.style = horse.getStyle();
      this.age = horse.getAge();
   }

   public Color getColor() {
      return this.color;
   }

   public int getAge() {
      return this.age;
   }

   public Style getStyle() {
      return this.style;
   }
}

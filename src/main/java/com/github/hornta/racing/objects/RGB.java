package com.github.hornta.racing.objects;

import java.util.Random;

public class RGB {
   private final int r;
   private final int g;
   private final int b;
   private static final Random random = new Random();

   RGB(int r, int g, int b) {
      this.r = r;
      this.g = g;
      this.b = b;
   }

   private static float randomWithin() {
      return 0.0F + random.nextFloat();
   }

   public static RGB randomLightColor() {
      float hue = randomWithin();
      float saturation = 0.9F;
      float lightness = 0.55F;
      return fromHSL(hue, saturation, lightness);
   }

   private static float hue2rgb(float p, float q, float t) {
      if (t < 0.0F) {
         ++t;
      }

      if (t > 1.0F) {
         --t;
      }

      if (t < 0.16666667F) {
         return p + (q - p) * 6.0F * t;
      } else if (t < 0.5F) {
         return q;
      } else {
         return t < 0.6666667F ? p + (q - p) * (0.6666667F - t) * 6.0F : p;
      }
   }

   public static RGB fromHSL(float h, float s, float l) {
      float r;
      float g;
      float b;
      if (s == 0.0F) {
         r = l;
         g = l;
         b = l;
      } else {
         float q = (double)l < 0.5D ? l * (1.0F + s) : l + s - l * s;
         float p = 2.0F * l - q;
         r = hue2rgb(p, q, h + 0.33333334F);
         g = hue2rgb(p, q, h);
         b = hue2rgb(p, q, h - 0.33333334F);
      }

      return new RGB(Math.round(r * 255.0F), Math.round(g * 255.0F), Math.round(b * 255.0F));
   }

   public int getR() {
      return this.r;
   }

   public int getG() {
      return this.g;
   }

   public int getB() {
      return this.b;
   }
}

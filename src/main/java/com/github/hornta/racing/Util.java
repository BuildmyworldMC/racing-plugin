package com.github.hornta.racing;

import com.github.hornta.racing.messenger.MessageManager;
import com.github.hornta.racing.objects.DurationUnit;
import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;

public class Util {
   private static final double HALF_RIGHT_ANGLE = 45.0D;
   private static final int MS_IN_ONE_DAY = 86400000;
   private static final int MS_IN_ONE_HOUR = 3600000;
   private static final int MS_IN_ONE_MINUTE = 60000;
   private static final long MS_IN_ONE_SECOND = 1000L;
   private static final int MAX_DURATION_UNITS = 2;

   public static Location snapAngles(Location location) {
      location.setPitch((float)(45.0D * (double)Math.round((double)location.getPitch() / 45.0D)));
      location.setYaw((float)(45.0D * (double)Math.round((double)location.getYaw() / 45.0D)));
      return location;
   }

   public static Location centerOnBlockHorizontally(Location location) {
      Location newLoc = location.clone();
      newLoc.setX((double)newLoc.getBlockX() + 0.5D);
      newLoc.setZ((double)newLoc.getBlockZ() + 0.5D);
      return newLoc;
   }

   public static Location centerOnBlock(Location location) {
      Location newLoc = location.clone();
      newLoc.setX((double)newLoc.getBlockX() + 0.5D);
      newLoc.setY((double)newLoc.getBlockY() + 0.5D);
      newLoc.setZ((double)newLoc.getBlockZ() + 0.5D);
      return newLoc;
   }

   public static String getFilenameWithoutExtension(File file) {
      String filename = file.getName();
      int lastDotIndex = filename.lastIndexOf(46);
      return lastDotIndex == -1 ? filename : filename.substring(0, lastDotIndex);
   }

   public static int randomRangeInt(int min, int max) {
      if (min > max) {
         throw new IllegalArgumentException("min must not be greater than max");
      } else {
         Random r = new Random();
         return r.nextInt(max - min + 1) + min;
      }
   }

   public static String getTimeLeft(int duration) {
      return getTimeLeft((long)duration);
   }

   public static String getTimeLeft(long duration) {
      if (duration == 0L) {
         return null;
      } else {
         DecimalFormat df = new DecimalFormat("#.#");
         df.setRoundingMode(RoundingMode.CEILING);
         long days = duration / 86400000L;
         long hours = duration % 86400000L / 3600000L;
         long minutes = duration % 86400000L % 3600000L / 60000L;
         long seconds = duration % 86400000L % 3600000L % 60000L;
         List<DurationUnit> units = new ArrayList<>();
         units.add(new DurationUnit(days, "<day>", "<days>"));
         units.add(new DurationUnit(hours, "<hour>", "<hours>"));
         units.add(new DurationUnit(minutes, "<minute>", "<minutes>"));
         units.add(new DurationUnit(seconds, "<second>", "<seconds>"));
         StringBuilder stringBuilder = new StringBuilder();
         int count = 0;

          for (DurationUnit unit : units) {
              long amount = unit.getAmount();
              if (count != 0 && amount == 0L) {
                  break;
              }

              if (amount != 0L) {
                  if (unit == units.getLast()) {
                      stringBuilder.append(df.format(((float) amount / 1000.0F)));
                  } else {
                      stringBuilder.append(amount);
                  }

                  stringBuilder.append(" ");
                  stringBuilder.append(unit.getNumerous());
                  stringBuilder.append(", ");
                  ++count;
                  if (count == 2) {
                      break;
                  }
              }
          }

         String string = stringBuilder.toString();
         return string.substring(0, string.length() - 2);
      }
   }

   public static void setTimeUnitValues() {
      MessageManager.setValue("second", MessageKey.TIME_UNIT_SECOND);
      MessageManager.setValue("seconds", MessageKey.TIME_UNIT_SECONDS);
      MessageManager.setValue("minute", MessageKey.TIME_UNIT_MINUTE);
      MessageManager.setValue("minutes", MessageKey.TIME_UNIT_MINUTES);
      MessageManager.setValue("hour", MessageKey.TIME_UNIT_HOURS);
      MessageManager.setValue("hours", MessageKey.TIME_UNIT_HOURS);
      MessageManager.setValue("day", MessageKey.TIME_UNIT_DAY);
      MessageManager.setValue("days", MessageKey.TIME_UNIT_DAYS);
   }
}

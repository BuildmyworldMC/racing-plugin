package io.papermc.lib.environments;

public class SpigotEnvironment extends CraftBukkitEnvironment {
   public String getName() {
      return "Spigot";
   }

   public boolean isSpigot() {
      return true;
   }
}

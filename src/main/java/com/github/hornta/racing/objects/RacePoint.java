package com.github.hornta.racing.objects;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.Permission;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RacePoint {
   private Location location;
   private int position;
   private final UUID id;
   private Hologram hologram;

   public RacePoint(UUID id, int position, Location location) {
      this.id = id;
      this.position = position;
      this.location = location;
   }

   public Hologram getHologram() {
      return this.hologram;
   }

   public UUID getId() {
      return this.id;
   }

   public int getPosition() {
      return this.position;
   }

   public void setPosition(int position) {
      this.position = position;
   }

   public Location getLocation() {
      return this.location.clone();
   }

   public void setLocation(Location location) {
      this.location = location;
      if (this.hologram != null) {
         this.hologram.setLocation(location.clone().add(new Vector(0, 0.5, 0)));
      }
   }

   public void setupHologram() {
      if (RacingPlugin.getInstance().isDecentHologramsLoaded()) {
         Location hologramLoc = this.location.clone().add(new Vector(0, 0.5, 0));
         String hologramName = "racepoint_" + this.id.toString();
         this.hologram = DHAPI.createHologram(hologramName, hologramLoc);
         DHAPI.addHologramLine(this.hologram, "§d" + this.position);

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(Permission.RACING_MODIFY.toString())) {
               this.hologram.setShowPlayer(player);
            } else {
               this.hologram.setHidePlayer(player);
            }
         }
      } else {
         System.out.println("Decent Hologram is not loaded");
      }
   }

   public void removeHologram() {
      if (RacingPlugin.getInstance().isDecentHologramsLoaded() && this.hologram != null) {
         this.hologram.delete();
         this.hologram = null;
      }
   }
}
package com.github.hornta.racing.objects;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.enums.Permission;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
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
         this.hologram.teleport(location.clone().add(new Vector(0, 1, 0)));
      }

   }

   public void setupHologram() {
      if (RacingPlugin.getInstance().isHolographicDisplaysLoaded()) {
         this.hologram = HologramsAPI.createHologram(RacingPlugin.getInstance(), this.location.clone().add(new Vector(0, 1, 0)));
         this.hologram.appendTextLine("§d" + this.position);
         this.hologram.getVisibilityManager().setVisibleByDefault(false);
         Iterator var1 = Bukkit.getOnlinePlayers().iterator();

         while(var1.hasNext()) {
            Player player = (Player)var1.next();
            if (player.hasPermission(Permission.RACING_MODIFY.toString())) {
               this.hologram.getVisibilityManager().showTo(player);
            }
         }

      }
   }

   public void removeHologram() {
      if (RacingPlugin.getInstance().isHolographicDisplaysLoaded() && this.hologram != null) {
         this.hologram.delete();
         this.hologram = null;
      }
   }
}

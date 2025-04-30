package com.github.hornta.racing.objects;

import com.github.hornta.racing.RacingPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RaceCheckpoint extends RacePoint implements Comparable<RaceCheckpoint> {
   private final int radius;
   private CheckpointParticleTask task;
   private final Vector vector;
   private final List<Player> players = new ArrayList<>();

   public RaceCheckpoint(UUID id, int position, Location location, int radius) {
      super(id, position, location);
      this.radius = radius;
      this.vector = location.toVector();
   }

   public int getRadius() {
      return this.radius;
   }

   public void stopTask() {
      if (this.task != null) {
         this.task.cancel();
         this.task = null;
      }
   }

   public void startTask(boolean isEditing) {
      this.startTask(isEditing, false);
   }

   public void startTask(boolean isEditing, boolean isLast) {
      this.task = new CheckpointParticleTask(this, isEditing, isLast);
      this.task.runTaskTimerAsynchronously(RacingPlugin.getInstance(), 0L, 1L);
   }

   public boolean isInside(Entity entity) {
      return entity.getLocation().toVector().isInSphere(this.vector, (double)this.radius);
   }

   public void addPlayer(Player player) {
      this.players.add(player);
   }

   public void removePlayer(Player player) {
      this.players.remove(player);
   }

   public List<Player> getPlayers() {
      return new ArrayList(this.players);
   }

   public int compareTo(RaceCheckpoint o) {
      return Integer.compare(this.getPosition(), o.getPosition());
   }
}

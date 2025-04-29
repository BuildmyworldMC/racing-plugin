package com.github.hornta.racing.objects;

import com.comphenix.protocol.wrappers.WrappedParticle;
import com.github.hornta.racing.enums.Permission;
import com.github.hornta.racing.packetwrapper.WrapperPlayServerWorldParticles;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CheckpointParticleTask extends BukkitRunnable {
   private static final double ANGLE_INCREMENT = Math.toRadians(4.0D);
   private static final int NUMBER_ORBS = 2;
   private static final double ANGLE_OFFSET = 3.141592653589793D;
   private double t;
   private final RaceCheckpoint checkpoint;
   private final boolean isEditing;
   private final boolean isLast;
   private final RGB defaultColor;
   private final RGB isInsideColor;

   public CheckpointParticleTask(RaceCheckpoint checkpoint, boolean isEditing, boolean isLast) {
      this.checkpoint = checkpoint;
      this.isEditing = isEditing;
      this.isLast = isLast;
      this.defaultColor = new RGB(255, 0, 0);
      this.isInsideColor = new RGB(0, 255, 0);
   }

   public void run() {
      List<Player> players = this.checkpoint.getPlayers();
      boolean isInside = false;
      if (this.isEditing) {
         Iterator var3 = Bukkit.getOnlinePlayers().iterator();

         while(var3.hasNext()) {
            Player player = (Player)var3.next();
            if (this.checkpoint.isInside(player)) {
               isInside = true;
               break;
            }
         }
      }

      Vector dir = this.checkpoint.getLocation().getDirection();

      for(int i = 0; i < 2; ++i) {
         double offset = (double)i * 3.141592653589793D;
         double angle = this.t + offset;
         Vector p = new Vector(-dir.getZ(), dir.getY(), dir.getX());
         Vector c = p.crossProduct(dir);
         Vector v = c.rotateAroundAxis(dir, angle).normalize().multiply(this.checkpoint.getRadius());
         Location loc = this.checkpoint.getLocation().add(v);
         WrapperPlayServerWorldParticles particle = new WrapperPlayServerWorldParticles();
         particle.setNumberOfParticles(1);
         particle.setLongDistance(true);
         RGB rgb = null;
         if (!isInside && this.isLast) {
            rgb = RGB.randomLightColor();
         }

         particle.setParticle(WrappedParticle.create(Particle.DUST, new DustOptions(Color.fromRGB(isInside ? this.isInsideColor.getR() : (this.isLast ? rgb.getR() : this.defaultColor.getR()), isInside ? this.isInsideColor.getG() : (this.isLast ? rgb.getG() : this.defaultColor.getG()), isInside ? this.isInsideColor.getB() : (this.isLast ? rgb.getB() : this.defaultColor.getB())), 2.0F)));
         particle.setX(loc.getX());
         particle.setY(loc.getY());
         particle.setZ(loc.getZ());
         particle.setParticleData(2.0F);
         Iterator var15;
         Player player;
         if (this.isEditing) {
            var15 = Bukkit.getOnlinePlayers().iterator();

            while(var15.hasNext()) {
               player = (Player)var15.next();
               if (player.hasPermission(Permission.RACING_MODIFY.toString()) && player.getLocation().getWorld() == this.checkpoint.getLocation().getWorld()) {
                  particle.sendPacket(player);
               }
            }
         } else {
            var15 = players.iterator();

            while(var15.hasNext()) {
               player = (Player)var15.next();
               if (player.getLocation().getWorld() == this.checkpoint.getLocation().getWorld()) {
                  particle.sendPacket(player);
               }
            }
         }
      }

      this.t += ANGLE_INCREMENT;
   }
}

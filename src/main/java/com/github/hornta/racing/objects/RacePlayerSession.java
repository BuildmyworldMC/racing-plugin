package com.github.hornta.racing.objects;

import com.github.hornta.racing.RacingPlugin;
import com.github.hornta.racing.Util;
import com.github.hornta.racing.enums.RaceType;
import com.github.hornta.racing.enums.RespawnType;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Steerable;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Tameable;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;

public class RacePlayerSession {
   public static final double MAX_HEALTH = 20.0D;
   public static final int MAX_FOOD_LEVEL = 20;
   private final RaceSession raceSession;
   private final double chargedEntryFee;
   private final Player player;
   private Location startLocation;
   private RaceCheckpoint currentCheckpoint;
   private RaceCheckpoint nextCheckpoint;
   private BossBar bossBar;
   private Entity vehicle;
   private int currentLap;
   private long lapStartTime;
   private long fastestLap = Long.MAX_VALUE;
   private long personalBestLapTime = Long.MAX_VALUE;
   private boolean allowedToEnterVehicle;
   private boolean allowedToExitVehicle;
   private RaceParticipantReset restore;

   RacePlayerSession(RaceSession raceSession, Player player, double chargedEntryFee) {
      this.raceSession = raceSession;
      this.player = player;
      this.chargedEntryFee = chargedEntryFee;
      if (raceSession.getRace().getResultByPlayerId().containsKey(player.getUniqueId())) {
         this.personalBestLapTime = raceSession.getRace().getResultByPlayerId().get(player.getUniqueId()).getFastestLap();
      }

   }

   public long getLapStartTime() {
      return this.lapStartTime;
   }

   public void setLapStartTime(long millis) {
      this.lapStartTime = millis;
   }

   public void setFastestLapTime(long millis) {
      if (millis < this.fastestLap) {
         this.fastestLap = millis;
      }

      if (millis < this.personalBestLapTime) {
         this.personalBestLapTime = millis;
      }

   }

   public long getFastestLap() {
      return this.fastestLap;
   }

   public long getPersonalBestLapTime() {
      return this.personalBestLapTime;
   }

   public boolean isFinished() {
      return this.nextCheckpoint == null;
   }

   void startCooldown() {
      if (this.player.isInsideVehicle()) {
         RacingPlugin.debug("%s is inside a vehicle. Attempting to eject it...", this.player.getName());
         this.allowedToExitVehicle = true;
         Objects.requireNonNull(this.player.getVehicle()).eject();
         this.allowedToExitVehicle = false;
         RacingPlugin.debug("Result of ejecting %s from vehicle: %B", this.player.getName(), this.player.getVehicle() == null);
      }

      RacingPlugin.debug("Teleporting %s to start location", this.player.getName());
      this.respawn(RespawnType.FROM_START, () -> {
         this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 2.0F);
         this.restore = new RaceParticipantReset(this);
      }, () -> {
         this.player.setHealth(20.0D);
         RacingPlugin.debug("Setting health to %f on %s", 20.0D, this.player.getName());
      });
      if (this.raceSession.getRace().getType() == RaceType.HORSE) {
         this.freezeHorse();
         RacingPlugin.debug("Freezed horse movement");
      }

   }

   Entity getVehicle() {
      return this.vehicle;
   }

   public double getChargedEntryFee() {
      return this.chargedEntryFee;
   }

   void startRace() {
      this.player.setWalkSpeed(this.raceSession.getRace().getWalkSpeed());
      this.player.setFoodLevel(20);
      this.player.removePotionEffect(PotionEffectType.JUMP_BOOST);
      if (this.vehicle instanceof Pig) {
         this.player.getInventory().setItemInMainHand(new ItemStack(Material.CARROT_ON_A_STICK, 1));
      } else if (this.vehicle instanceof Strider) {
         this.player.getInventory().setItemInMainHand(new ItemStack(Material.WARPED_FUNGUS_ON_A_STICK, 1));
      } else if (this.vehicle instanceof Horse) {
         this.unfreezeHorse();
      }

      if (this.raceSession.getRace().getType() == RaceType.ELYTRA) {
         this.player.getInventory().setChestplate(new ItemStack(Material.ELYTRA, 1));
      }

   }

   public void respawnInVehicle() {
      Location location;
      if (this.currentCheckpoint != null && this.raceSession.getRace().getType() != RaceType.ELYTRA) {
         location = this.currentCheckpoint.getLocation();
      } else {
         location = this.startLocation;
      }

      this.respawnInVehicle(location, null, null);
   }

   public void respawnInVehicle(Location location, Runnable runnable, Runnable fireTicksResetCallback) {
      if (this.vehicle != null) {
         this.exitVehicle();
         this.vehicle.remove();
      }

      switch(this.raceSession.getRace().getType()) {
      case PIG:
         this.spawnVehicle(EntityType.PIG, location);
         this.setupPig();
         break;
      case STRIDER:
         this.spawnVehicle(EntityType.STRIDER, location);
         this.setupStrider();
         break;
      case MINECART:
         this.spawnVehicle(EntityType.MINECART, location);
         break;
      case HORSE:
         HorseData horseData = null;
         if (this.vehicle != null) {
            horseData = new HorseData((Horse)this.vehicle);
         }

         this.spawnVehicle(EntityType.HORSE, location);
         this.setupHorse(horseData);
         break;
      case BOAT:
         this.spawnVehicle(EntityType.OAK_BOAT, location);
         RacingPlugin.debug("Setting boat type to " + EntityType.OAK_BOAT);
      case ELYTRA:
      case PLAYER:
      }

      if (this.vehicle != null) {
         this.vehicle.setInvulnerable(true);
         RacingPlugin.debug("Making vehicle invulnerable");
      }

      Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
         Location playerTeleportLoc = location;
         if (this.vehicle instanceof Boat) {
            playerTeleportLoc = location.clone().add(0.0D, -0.45D, 0.0D);
         }

         PaperLib.teleportAsync(this.player, playerTeleportLoc, TeleportCause.PLUGIN).thenAccept((result) -> {
            Bukkit.getScheduler().runTask(RacingPlugin.getInstance(), () -> {
               RacingPlugin.debug("Teleported %s to vehicle %s", this.player.getName(), this.vehicle.getType());
               Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
                  this.player.setFireTicks(0);
                  if (fireTicksResetCallback != null) {
                     fireTicksResetCallback.run();
                  }

               }, 2L);
               this.enterVehicle(this.vehicle);
               if (runnable != null) {
                  runnable.run();
               }

            });
         });
      }, 1L);
   }

   public void respawn(RespawnType type, Runnable runnable, Runnable fireTicksResetCallback) {
      RacingPlugin.debug("Respawn %s %s", this.player.getName(), type);
      if (type == null) {
         throw new NullPointerException();
      } else {
         Location loc;
         if (type != RespawnType.FROM_START && this.currentCheckpoint != null) {
            loc = this.currentCheckpoint.getLocation();
            if (this.vehicle instanceof Boat) {
               loc.add(0.0D, -0.5D, 0.0D);
            }
         } else {
            loc = this.startLocation;
         }

         if (this.player.isSleeping()) {
            RacingPlugin.debug("%s was sleeping. Trying to wake them up...", this.player.getName());
            this.player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 255, false, false, false));
            this.player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1, false, false, false));
            RacingPlugin.debug("Result of waking %s up: %B", this.player.getName(), this.player.isSleeping());
         }

         RacingPlugin.debug("Attempting to set fall distance to zero on %s to prevent taking damage from falling", this.player.getName());
         this.player.setFallDistance(0.0F);
         RacingPlugin.debug("Fall distance on %s was set to %f", this.player.getName(), this.player.getFallDistance());
         switch(this.raceSession.getRace().getType()) {
         case PIG:
         case STRIDER:
         case MINECART:
         case HORSE:
         case BOAT:
            this.respawnInVehicle(loc, runnable, fireTicksResetCallback);
            break;
         case ELYTRA:
         case PLAYER:
            PaperLib.teleportAsync(this.player, loc, TeleportCause.PLUGIN).thenAccept((result) -> {
               Bukkit.getScheduler().runTaskLater(RacingPlugin.getInstance(), () -> {
                  Bukkit.getScheduler().scheduleSyncDelayedTask(RacingPlugin.getInstance(), () -> {
                     this.player.setFireTicks(0);
                     if (fireTicksResetCallback != null) {
                        fireTicksResetCallback.run();
                     }

                  }, 2L);
                  if (result && runnable != null) {
                     runnable.run();
                  }

               }, 1L);
            });
         }

      }
   }

   private void spawnVehicle(EntityType type, Location location) {
      RacingPlugin.debug("Attempting to spawn vehicle of type %s at %s", type, location);
      this.vehicle = Objects.requireNonNull(this.startLocation.getWorld()).spawnEntity(location, type);
      RacingPlugin.debug("Spawned vehicle at " + this.vehicle.getLocation());
   }

   private void setupPig() {
      ((LivingEntity)this.vehicle).setAI(false);
      RacingPlugin.debug("Disable pig AI");
      ((Steerable)this.vehicle).setSaddle(true);
      RacingPlugin.debug("Giving pig a saddle");
      Objects.requireNonNull(((Attributable) this.vehicle).getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(this.raceSession.getRace().getPigSpeed());
      RacingPlugin.debug("Setting movementspeed on pig to " + this.raceSession.getRace().getPigSpeed());
   }

   private void setupStrider() {
      ((LivingEntity)this.vehicle).setAI(false);
      RacingPlugin.debug("Disable strider AI");
      ((Steerable)this.vehicle).setSaddle(true);
      RacingPlugin.debug("Giving strider a saddle");
      Objects.requireNonNull(((Attributable) this.vehicle).getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(this.raceSession.getRace().getStriderSpeed());
      RacingPlugin.debug("Setting movementspeed on strider to " + this.raceSession.getRace().getStriderSpeed());
   }

   private void setupHorse(HorseData horseData) {
      ((LivingEntity)this.vehicle).setAI(false);
      RacingPlugin.debug("Disable horse AI");
      ((Tameable)this.vehicle).setTamed(true);
      RacingPlugin.debug("Set horse to be tamed");
      ((Tameable)this.vehicle).setOwner(this.player);
      RacingPlugin.debug("Set horse owner to " + this.player.getName());
      ((AbstractHorse)this.vehicle).getInventory().setSaddle(new ItemStack(Material.SADDLE, 1));
      RacingPlugin.debug("Giving horse a saddle");
      ((AbstractHorse)this.vehicle).setJumpStrength(this.raceSession.getRace().getHorseJumpStrength());
      RacingPlugin.debug("Setting horse jump strength to" + this.raceSession.getRace().getHorseJumpStrength());
      Objects.requireNonNull(((Attributable) this.vehicle).getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(this.raceSession.getRace().getHorseSpeed());
      RacingPlugin.debug("Setting horse movement speed to " + this.raceSession.getRace().getHorseSpeed());
      if (horseData != null) {
         RacingPlugin.debug("Transferring old horse values to new horse");
         RacingPlugin.debug("Attempt to set horse color to " + horseData.getColor());
         ((Horse)this.vehicle).setColor(horseData.getColor());
         RacingPlugin.debug("Horse color set to " + ((Horse)this.vehicle).getColor());
         RacingPlugin.debug("Attempting to set horse style to " + horseData.getStyle());
         ((Horse)this.vehicle).setStyle(horseData.getStyle());
         RacingPlugin.debug("Horse style set to " + ((Horse)this.vehicle).getStyle());
         RacingPlugin.debug("Attempting to set horse age to " + horseData.getAge());
         ((Ageable)this.vehicle).setAge(horseData.getAge());
         RacingPlugin.debug("Horse age set to " + ((Ageable)this.vehicle).getAge());
      }

   }

   public void freezeHorse() {
      ((AbstractHorse)this.vehicle).setJumpStrength(0.0D);
      Objects.requireNonNull(((Attributable) this.vehicle).getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0D);
   }

   private void unfreezeHorse() {
      Objects.requireNonNull(((Attributable) this.vehicle).getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(this.raceSession.getRace().getHorseSpeed());
      ((AbstractHorse)this.vehicle).setJumpStrength(this.raceSession.getRace().getHorseJumpStrength());
   }

   void setStartPoint(RaceStartPoint startPoint) {
      this.startLocation = Util.snapAngles(startPoint.getLocation());
   }

   public Location getStartLocation() {
      return this.startLocation;
   }

   public Player getPlayer() {
      return this.player;
   }

   boolean isAllowedToEnterVehicle() {
      return this.allowedToEnterVehicle;
   }

   boolean isAllowedToExitVehicle() {
      return this.allowedToExitVehicle;
   }

   public void enterVehicle(Entity vehicle) {
      RacingPlugin.debug("Attempting to enter passenger %s to vehicle %s", this.player.getName(), vehicle.getType());
      this.allowedToEnterVehicle = true;
      vehicle.addPassenger(this.player);
      this.allowedToEnterVehicle = false;
      RacingPlugin.debug("Result of attempting to enter %s into %s: %b", this.player.getName(), vehicle.getType(), this.player.isInsideVehicle());
   }

   void exitVehicle() {
      this.allowedToExitVehicle = true;
      this.vehicle.removePassenger(this.player);
      this.allowedToExitVehicle = false;
   }

   void restore() {
      if (this.restore != null) {
         RacingPlugin.debug("Restoring %s...", this.player.getName());
         this.startLocation = null;
         this.currentCheckpoint = null;
         if (this.nextCheckpoint != null) {
            this.nextCheckpoint.removePlayer(this.player);
            this.nextCheckpoint = null;
         }

         this.restore.restore();
         this.restore = null;
         this.bossBar.removeAll();
         this.bossBar = null;
         if (this.vehicle != null) {
            RacingPlugin.debug("Attempt to eject %s from vehicle", this.player.getName());
            this.exitVehicle();
            this.vehicle.remove();
            RacingPlugin.debug("%s ejected from vehicle result: %b", this.player.getName(), this.player.getVehicle() == null);
         }

         this.vehicle = null;
      }
   }

   public RaceCheckpoint getCurrentCheckpoint() {
      return this.currentCheckpoint;
   }

   public RaceCheckpoint getNextCheckpoint() {
      return this.nextCheckpoint;
   }

   public void setNextCheckpoint(RaceCheckpoint checkpoint) {
      this.currentCheckpoint = this.nextCheckpoint;
      if (this.currentCheckpoint != null) {
         this.currentCheckpoint.removePlayer(this.player);
      }

      this.nextCheckpoint = checkpoint;
      if (this.nextCheckpoint != null) {
         this.nextCheckpoint.addPlayer(this.player);
      }

   }

   public BossBar getBossBar() {
      return this.bossBar;
   }

   void setBossBar(BossBar bossBar) {
      bossBar.addPlayer(this.player);
      this.bossBar = bossBar;
   }

   public int getCurrentLap() {
      return this.currentLap;
   }

   public void setCurrentLap(int currentLap) {
      this.currentLap = currentLap;
   }

   public boolean isRestored() {
      return this.restore == null;
   }

   public RaceSession getRaceSession() {
      return this.raceSession;
   }
}

package com.github.hornta.racing.objects;

import com.github.hornta.racing.ConfigKey;
import com.github.hornta.racing.RacingPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RaceParticipantReset {
   private final RacePlayerSession playerSession;
   private final float walkSpeed;
   private final int foodLevel;
   private final ItemStack[] storageContents;
   private final ItemStack[] armorContents;
   private final ItemStack[] extraContents;
   private final Collection<PotionEffect> potionEffects;
   private final double health;
   private final GameMode gameMode;
   private final int fireTicks;
   private final boolean allowFlight;
   private final float exp;
   private final float exhaustion;
   private final int level;
   private final float saturation;

   RaceParticipantReset(RacePlayerSession playerSession) {
      this.playerSession = playerSession;
      this.gameMode = playerSession.getPlayer().getGameMode();
      this.walkSpeed = playerSession.getPlayer().getWalkSpeed();
      this.foodLevel = playerSession.getPlayer().getFoodLevel();
      this.potionEffects = new ArrayList<>(playerSession.getPlayer().getActivePotionEffects());
      this.health = playerSession.getPlayer().getHealth();
      this.fireTicks = playerSession.getPlayer().getFireTicks();
      this.allowFlight = playerSession.getPlayer().getAllowFlight();
      this.exp = playerSession.getPlayer().getExp();
      this.exhaustion = playerSession.getPlayer().getExhaustion();
      this.level = playerSession.getPlayer().getLevel();
      this.saturation = playerSession.getPlayer().getSaturation();

      ItemStack cursorItem = playerSession.getPlayer().getItemOnCursor();
      if (cursorItem.getAmount() > 0) {
         HashMap<Integer, ItemStack> rest = playerSession.getPlayer().getInventory().addItem(cursorItem);
         RacingPlugin.debug("Add items to inventory from cursor before closing inventory of %s", playerSession.getPlayer().getName());
         if (!rest.isEmpty()) {
            playerSession.getPlayer().setItemOnCursor((ItemStack)rest.get(0));
            RacingPlugin.debug("Setting cursor items of %s to %s", playerSession.getPlayer().getName(), rest.get(0));
         } else {
            playerSession.getPlayer().setItemOnCursor((ItemStack)null);
         }
      }

      playerSession.getPlayer().closeInventory();
      RacingPlugin.debug("Closing inventory of %s", playerSession.getPlayer().getName());
      this.storageContents = cloneItemArray(playerSession.getPlayer().getInventory().getStorageContents());
      this.armorContents = cloneItemArray(playerSession.getPlayer().getInventory().getArmorContents());
      this.extraContents = cloneItemArray(playerSession.getPlayer().getInventory().getExtraContents());
      if ((Boolean)RacingPlugin.getInstance().getConfiguration().get(ConfigKey.ADVENTURE_ON_START)) {
         playerSession.getPlayer().setGameMode(GameMode.ADVENTURE);
         RacingPlugin.debug("Setting game mode to %s on %s", GameMode.ADVENTURE, playerSession.getPlayer().getName());
      }

      RacingPlugin.debug("Attempting to set exhaustion to %f on %s", 0.0F, playerSession.getPlayer().getName());
      playerSession.getPlayer().setExhaustion(0.0F);
      RacingPlugin.debug("Exhaustion was set to %f on %s", playerSession.getPlayer().getExhaustion(), playerSession.getPlayer().getName());
      RacingPlugin.debug("Attempting to set saturation to %f on %s", 0.0F, playerSession.getPlayer().getName());
      playerSession.getPlayer().setSaturation(0.0F);
      RacingPlugin.debug("Saturation was set to %f on %s", playerSession.getPlayer().getSaturation(), playerSession.getPlayer().getName());
      RacingPlugin.debug("Attempting to set experience to %f on %s", 0.0F, playerSession.getPlayer().getName());
      playerSession.getPlayer().setExp(0.0F);
      RacingPlugin.debug("Experience was set to %f on %s", playerSession.getPlayer().getExp(), playerSession.getPlayer().getName());
      RacingPlugin.debug("Attempting to set level to %f on %s", 0.0F, playerSession.getPlayer().getName());
      playerSession.getPlayer().setLevel(0);
      RacingPlugin.debug("Level was set to %d on %s", playerSession.getPlayer().getLevel(), playerSession.getPlayer().getName());
      RacingPlugin.debug("Attempting to set walk speed to %f on %s", 0.0F, playerSession.getPlayer().getName());
      playerSession.getPlayer().setWalkSpeed(0.0F);
      RacingPlugin.debug("Walk speed was set to %f on %s", playerSession.getPlayer().getWalkSpeed(), playerSession.getPlayer().getName());
      RacingPlugin.debug("Attempting to set food level to %d on %s", 6, playerSession.getPlayer().getName());
      playerSession.getPlayer().setFoodLevel(6);
      RacingPlugin.debug("Setting food level to %d on %s", playerSession.getPlayer().getFoodLevel(), playerSession.getPlayer().getName());
      RacingPlugin.debug("Attempting to clear inventory of %s", playerSession.getPlayer().getName());
      playerSession.getPlayer().getInventory().clear();
      RacingPlugin.debug("Inventory of %s was cleared. Contents: %s", playerSession.getPlayer().getName(), playerSession.getPlayer().getInventory().getContents());
      RacingPlugin.debug("Attempting to remove potion effects on %s", playerSession.getPlayer().getName());

       for (PotionEffect effect : playerSession.getPlayer().getActivePotionEffects()) {
           playerSession.getPlayer().removePotionEffect(effect.getType());
           RacingPlugin.debug("Removed potion effect %s on %s", effect.getType(), playerSession.getPlayer().getName());
       }

      int countdown = RacingPlugin.getInstance().getConfiguration().get(ConfigKey.COUNTDOWN);
      RacingPlugin.debug("Attempting to add jump potion effect on %s", playerSession.getPlayer().getName());
      playerSession.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, countdown * 20, 128));
      RacingPlugin.debug("Potion effect %s added to %s", playerSession.getPlayer().getPotionEffect(PotionEffectType.JUMP_BOOST), playerSession.getPlayer().getName());
      
      switch(playerSession.getRaceSession().getRace().getType()) {
         case HORSE:
         case BOAT:
         case PIG:
         case STRIDER:
         case MINECART:
         case HAPPY_GHAST:
         case CAMEL:
         case NAUTILUS:
            playerSession.getPlayer().setAllowFlight(true);
            break;
         case ELYTRA:
         case PLAYER:
            playerSession.getPlayer().setAllowFlight(false);
      }

   }

   public void restore() {
      RacingPlugin.debug("Removing potion effects on %s...", this.playerSession.getPlayer().getName());

       for (PotionEffect effect : this.playerSession.getPlayer().getActivePotionEffects()) {
           this.playerSession.getPlayer().removePotionEffect(effect.getType());
           RacingPlugin.debug("Removed potion effect %s on %s", effect.getType(), this.playerSession.getPlayer().getName());
       }

      this.playerSession.getPlayer().addPotionEffects(this.potionEffects);
      RacingPlugin.debug("Restored potion effects of %s", this.playerSession.getPlayer().getName());
      this.playerSession.getPlayer().setFoodLevel(this.foodLevel);
      RacingPlugin.debug("Restored food level on %s to %d", this.playerSession.getPlayer().getName(), this.foodLevel);
      this.playerSession.getPlayer().setWalkSpeed(this.walkSpeed);
      RacingPlugin.debug("Restored walk speed of %s to %f", this.playerSession.getPlayer().getName(), this.walkSpeed);
      this.playerSession.getPlayer().setLevel(this.level);
      RacingPlugin.debug("Restored level of %s to %d", this.playerSession.getPlayer().getLevel(), this.level);
      this.playerSession.getPlayer().setExp(this.exp);
      RacingPlugin.debug("Restored experience of %s to %f", this.playerSession.getPlayer().getName(), this.exp);
      this.playerSession.getPlayer().setSaturation(this.saturation);
      RacingPlugin.debug("Restored saturation of %s to %f", this.playerSession.getPlayer().getName(), this.saturation);
      this.playerSession.getPlayer().setExhaustion(this.exhaustion);
      RacingPlugin.debug("Restored exhaustion of %s to %f", this.playerSession.getPlayer().getName(), this.exhaustion);
      this.playerSession.getPlayer().setFireTicks(this.fireTicks);
      RacingPlugin.debug("Restored fire ticks of %s to %d", this.playerSession.getPlayer().getName(), this.fireTicks);
      this.playerSession.getPlayer().setAllowFlight(this.allowFlight);
      RacingPlugin.debug("Restoring allowFlight of %s to %b", this.playerSession.getPlayer().getName(), this.allowFlight);
      double maxHealth = 20.0D;
      try {
         for (String attrName : new String[]{"MAX_HEALTH", "GENERIC_MAX_HEALTH"}) {
            try {
               Attribute attribute = Attribute.valueOf(attrName);
               org.bukkit.attribute.AttributeInstance attributeInstance = this.playerSession.getPlayer().getAttribute(attribute);
               if (attributeInstance != null) {
                  maxHealth = attributeInstance.getValue();
                  break;
               }
            } catch (IllegalArgumentException | NoSuchFieldError ignored) {
            }
         }
      } catch (Throwable ignored) {
      }
      double clampedHealth = Math.min(this.health, maxHealth);
      this.playerSession.getPlayer().setHealth(clampedHealth);
      RacingPlugin.debug("Restored health of %s to %f", this.playerSession.getPlayer().getName(), clampedHealth);
      this.playerSession.getPlayer().setGameMode(this.gameMode);
      RacingPlugin.debug("Restored game mode of %s to %s", this.playerSession.getPlayer().getName(), this.gameMode);
      this.playerSession.getPlayer().getInventory().clear();
      this.playerSession.getPlayer().getInventory().setStorageContents(this.storageContents);
      this.playerSession.getPlayer().getInventory().setArmorContents(this.armorContents);
      this.playerSession.getPlayer().getInventory().setExtraContents(this.extraContents);
      this.playerSession.getPlayer().updateInventory();
      RacingPlugin.debug("Restored inventory of %s", this.playerSession.getPlayer().getName());
   }

   private static ItemStack[] cloneItemArray(ItemStack[] items) {
      if (items == null) {
         return null;
      }
      ItemStack[] copy = new ItemStack[items.length];
      for (int i = 0; i < items.length; i++) {
         copy[i] = items[i] != null ? items[i].clone() : null;
      }
      return copy;
   }
}

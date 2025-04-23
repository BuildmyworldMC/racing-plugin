package com.github.hornta.racing.objects;

import com.github.hornta.racing.enums.RaceSignType;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class RaceSign {
   private final Sign sign;
   private final UUID creator;
   private final Instant createdAt;
   private final int laps;
   private final RaceSignType type;

   public RaceSign(Sign sign, UUID creator, Instant createdAt, int laps, RaceSignType type) {
      this.sign = sign;
      this.creator = creator;
      this.createdAt = createdAt;
      this.laps = laps;
      this.type = type;
   }

   public Sign getSign() {
      return this.sign;
   }

   public UUID getCreator() {
      return this.creator;
   }

   public Instant getCreatedAt() {
      return this.createdAt;
   }

   public String getKey() {
      return createKey(this.sign.getBlock());
   }

   public int getLaps() {
      return this.laps;
   }

   public RaceSignType getSignType() {
      return this.type;
   }

   public static String createKey(Block block) {
      String var10000 = block.getLocation().getWorld().getName();
      return var10000 + "_" + block.getLocation().getBlockX() + "_" + block.getLocation().getBlockY() + "_" + block.getLocation().getBlockZ();
   }
}

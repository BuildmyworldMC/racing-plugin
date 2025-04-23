package com.github.hornta.racing.commando;

import com.github.hornta.racing.commando.completers.ArtCompleter;
import com.github.hornta.racing.commando.completers.BiomeCompleter;
import com.github.hornta.racing.commando.completers.BooleanEffectCompleter;
import com.github.hornta.racing.commando.completers.GameModeCompleter;
import com.github.hornta.racing.commando.completers.MaterialCompleter;
import com.github.hornta.racing.commando.completers.OnlinePlayerCompleter;
import com.github.hornta.racing.commando.completers.PotionEffectCompleter;
import com.github.hornta.racing.commando.completers.WorldCompleter;
import com.github.hornta.racing.commando.completers.WorldNormalCompleter;

public enum CarbonArgumentType {
   STRING(true),
   INTEGER(true),
   NUMBER(true),
   BOOLEAN(BooleanEffectCompleter.class),
   ONLINE_PLAYER(OnlinePlayerCompleter.class),
   WORLD(WorldCompleter.class),
   WORLD_NORMAL(WorldNormalCompleter.class),
   POTION_EFFECT(PotionEffectCompleter.class),
   GAME_MODE(GameModeCompleter.class),
   BIOME(BiomeCompleter.class),
   ART(ArtCompleter.class),
   MATERIAL(MaterialCompleter.class),
   DURATION(true),
   OTHER(false);

   private final Class<?> completer;
   private final boolean isPrimitive;

   private CarbonArgumentType(boolean isPrimitive) {
      this.completer = null;
      this.isPrimitive = isPrimitive;
   }

   private CarbonArgumentType(Class<?> completer) {
      this.completer = completer;
      this.isPrimitive = false;
   }

   public boolean isCompletable() {
      return this.completer != null;
   }

   public Class<?> getCompleter() {
      return this.completer;
   }

   public boolean isPrimitive() {
      return this.isPrimitive;
   }
}

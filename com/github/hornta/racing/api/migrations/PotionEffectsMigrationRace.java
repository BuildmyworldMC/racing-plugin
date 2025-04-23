package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class PotionEffectsMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V3;
   }

   public RaceVersion to() {
      return RaceVersion.V4;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.createSection("potion_effects");
   }
}

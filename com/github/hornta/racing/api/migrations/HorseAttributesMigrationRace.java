package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class HorseAttributesMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V8;
   }

   public RaceVersion to() {
      return RaceVersion.V9;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("horse_speed", 0.225D);
      yamlConfiguration.set("horse_jump_strength", 0.7D);
   }
}

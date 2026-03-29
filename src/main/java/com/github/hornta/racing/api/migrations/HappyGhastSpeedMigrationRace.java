package com.github.hornta.racing.api.migrations;

import com.github.hornta.racing.api.IRaceFileMigration;
import com.github.hornta.racing.enums.RaceVersion;
import org.bukkit.configuration.file.YamlConfiguration;

public class HappyGhastSpeedMigrationRace implements IRaceFileMigration {
   public RaceVersion from() {
      return RaceVersion.V16;
   }

   public RaceVersion to() {
      return RaceVersion.V17;
   }

   public void migrate(YamlConfiguration yamlConfiguration) {
      yamlConfiguration.set("happy_ghast_speed", 16D);
   }
}
